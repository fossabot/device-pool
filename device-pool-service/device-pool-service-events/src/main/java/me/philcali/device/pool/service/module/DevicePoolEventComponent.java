package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;
import me.philcali.device.pool.service.workflow.CreateReservationStep;
import me.philcali.device.pool.service.event.DevicePoolEventRouter;
import me.philcali.device.pool.service.workflow.FailProvisionStep;
import me.philcali.device.pool.service.workflow.FinishProvisionStep;
import me.philcali.device.pool.service.workflow.ObtainDevicesStep;
import me.philcali.device.pool.service.workflow.StartProvisionStep;

import javax.inject.Singleton;

@Component(modules = {
        JacksonModule.class,
        WorkflowModule.class,
        DynamoDBModule.class,
        ApplicationModule.class,
        HttpModule.class,
        LambdaModule.class,
        DevicePoolClientModule.class,
        EventRouterModule.class
})
@Singleton
public interface DevicePoolEventComponent {
    ObjectMapper mapper();

    DevicePoolEventRouter eventRouter();

    StartProvisionStep startProvisionStep();

    CreateReservationStep createReservationStep();

    FailProvisionStep failProvisionStep();

    FinishProvisionStep finishProvisionStep();

    ObtainDevicesStep obtainDevicesStep();
}
