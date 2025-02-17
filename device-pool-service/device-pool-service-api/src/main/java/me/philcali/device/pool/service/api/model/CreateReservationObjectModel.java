package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateReservationObject.class)
interface CreateReservationObjectModel {
    String id();

    String deviceId();

    @Value.Default
    default Status status() {
        return Status.REQUESTED;
    }
}
