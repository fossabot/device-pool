package me.philcali.device.pool.service.rpc.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ObtainDeviceResponse.class)
abstract class ObtainDeviceResponseModel {
    abstract CompositeKey accountKey();

    abstract DeviceObject device();

    abstract Status status();

    @Nullable
    abstract String message();
}
