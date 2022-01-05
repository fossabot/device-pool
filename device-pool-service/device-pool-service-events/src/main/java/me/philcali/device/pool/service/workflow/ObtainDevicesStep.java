package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.DevicePoolClientFactory;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class ObtainDevicesStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private final ReservationRepo reservationRepo;
    private final DevicePoolClientFactory clientFactory;

    @Inject
    public ObtainDevicesStep(
            final ReservationRepo reservationRepo,
            final DevicePoolClientFactory clientFactory) {
        this.reservationRepo = reservationRepo;
        this.clientFactory = clientFactory;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        if (Objects.isNull(input.endpoint())) {
            throw new WorkflowExecutionException("Workflow input is missing required endpoint.");
        }
        Context context = Context.of(input.endpoint());
        DevicePoolClient client = clientFactory.get(input.endpoint().type());
        List<ReservationObject> existing = listAll(input.provision().selfKey(), reservationRepo);
        int finalized = 0;
        Queue<ReservationObject> pendingReservations = new LinkedList<>();
        for (ReservationObject existingReservation : existing) {
            if (existingReservation.status().isTerminal()) {
                finalized++;
            } else {
                pendingReservations.offer(existingReservation);
            }
        }
        int updated = finalized;
        for (int time = 0; time < input.provision().amount() - finalized; time++) {
            ReservationObject reservationObject = pendingReservations.poll();
            // Update existing device, or obtain a new one
            ObtainDeviceResponse response = client.obtainDevice(context, ObtainDeviceRequest.builder()
                    .provision(input.provision())
                    .accountKey(input.key())
                    .reservation(reservationObject)
                    .build());
            // Obtained a new one, create the entry
            if (Objects.isNull(reservationObject)) {
                reservationObject = reservationRepo.create(input.provision().selfKey(), CreateReservationObject.builder()
                        .deviceId(response.device().id())
                        .id(UUID.randomUUID().toString())
                        .build());
            }
            // Update with status code and message
            reservationRepo.update(reservationObject.key(), UpdateReservationObject.builder()
                    .id(reservationObject.id())
                    .status(response.status())
                    .message(response.message())
                    .build());
            // Add to total finalized
            if (response.status().isTerminal()) {
                updated++;
            }
        }
        AtomicReference<Status> currentStatus = new AtomicReference<>(input.provision().status());
        boolean done = updated == input.provision().amount();
        if (done) {
            currentStatus.set(Status.SUCCEEDED);
        }
        return WorkflowState.builder()
                .from(input.update(b -> b.status(currentStatus.get())))
                .done(done)
                .build();
    }
}
