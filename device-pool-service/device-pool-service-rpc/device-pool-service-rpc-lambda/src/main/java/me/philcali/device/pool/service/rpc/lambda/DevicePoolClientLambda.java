package me.philcali.device.pool.service.rpc.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationResponse;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.services.lambda.model.LogType;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Base64;

public class DevicePoolClientLambda implements DevicePoolClient {
    private static final Logger LOGGER = LogManager.getLogger(DevicePoolClientLambda.class);
    private final LambdaClient lambda;
    private final ObjectMapper mapper;

    @Inject
    public DevicePoolClientLambda(
            final LambdaClient lambda,
            final ObjectMapper mapper) {
        this.lambda = lambda;
        this.mapper = mapper;
    }

    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.LAMBDA;
    }

    private <Req, Res> Res invoke(
            Context context,
            CompositeKey accountKey,
            Req request,
            Class<Res> responseClass) throws RemoteServiceException {
        try {
            ClientContext clientContext = ClientContext.of(
                    accountKey,
                    request.getClass().getSimpleName().replace("Request", ""));
            LOGGER.debug("Invoking {} with client context {}", context.endpoint().uri(), clientContext);
            byte[] clientContextPayload = mapper.writeValueAsBytes(clientContext);
            InvokeResponse response = lambda.invoke(InvokeRequest.builder()
                    .functionName(context.endpoint().uri())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .logType(LogType.TAIL)
                    .clientContext(Base64.getEncoder().encodeToString(clientContextPayload))
                    .payload(SdkBytes.fromUtf8String(mapper.writeValueAsString(request)))
                    .build());
            LOGGER.info("Invoked {}: {}", context.endpoint().uri(), response.logResult());
            if (response.statusCode() > 200) {
                throw new RemoteServiceException(response.functionError());
            }
            return mapper.readValue(response.payload().asByteArray(), responseClass);
        } catch (IOException e) {
            throw new RemoteServiceException(e);
        } catch (LambdaException e) {
            LOGGER.error("Failed to invoke {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e,
                    e.isThrottlingException()
                    || (e.statusCode() >= 500 && e.statusCode() <= 504));
        }
    }

    @Override
    public CancelReservationResponse cancelReservation(Context context, CancelReservationRequest request)
            throws RemoteServiceException {
        return invoke(context, request.accountKey(), request, CancelReservationResponse.class);
    }

    @Override
    public ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request)
            throws RemoteServiceException {
        return invoke(context, request.accountKey(), request, ObtainDeviceResponse.class);
    }
}
