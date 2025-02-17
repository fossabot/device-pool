package me.philcali.device.pool.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import me.philcali.device.pool.service.module.DaggerDevicePoolsComponent;
import me.philcali.device.pool.service.module.DevicePoolsComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DevicePools implements RequestStreamHandler {
    private final DevicePoolsComponent component;

    public DevicePools(DevicePoolsComponent component) {
        this.component = component;
    }

    public DevicePools() {
        this(DaggerDevicePoolsComponent.create());
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        component.handler().proxyStream(inputStream, outputStream, context);
    }
}
