package me.philcali.device.pool.service.rpc.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ObtainDeviceRequest.class)
abstract class ObtainDeviceRequestModel {
    abstract CompositeKey accountKey();

    abstract ProvisionObject provision();

    @Nullable
    abstract ReservationObject reservation();
}
