package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class DeleteProvisionFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LogManager.getLogger(DeleteProvisionFunction.class);
    private final ReservationRepo reservationRepo;
    private final TableSchema<ProvisionObject> tableSchema;

    @Inject
    public DeleteProvisionFunction(
            final ReservationRepo reservationRepo,
            final TableSchema<ProvisionObject> tableSchema) {
        this.reservationRepo = reservationRepo;
        this.tableSchema = tableSchema;
    }

    @Override
    public boolean test(Record record) {
        return record.getEventName().equals(OperationType.REMOVE.name())
                && primaryKeyFrom(record, StreamRecord::getOldImage).equals(ProvisionRepoDynamo.RESOURCE);
    }

    @Override
    public void accept(
            Map<String, AttributeValue> newImage,
            Map<String, AttributeValue> oldImage) {
        ProvisionObject provisionObject = tableSchema.mapToItem(oldImage);
        listAll(provisionObject.selfKey(), reservationRepo).forEach(reservationObject -> {
            if (!reservationObject.status().isTerminal()) {
                LOGGER.warn("Removing a non-terminal reservation, investigation is needed: {}",
                        reservationObject);
            }
            reservationRepo.delete(reservationObject.key(), reservationObject.id());
            LOGGER.info("Removing associated reservation from provision {}: {}",
                    provisionObject.id(),
                    reservationObject.id());
        });
    }
}
