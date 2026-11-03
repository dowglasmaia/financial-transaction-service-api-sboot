package com.dowglasmaia.financialtransaction.transaction.infrastructure.client;

import com.dowglasmaia.financialtransaction.transaction.application.port.PixGateway;
import org.springframework.stereotype.Component;

/** Implements the Pix port with the MaiaBank Feign client (timeouts and circuit breaker stay on the client). */
@Component
class MaiaBankPixGatewayAdapter implements PixGateway {

    private final MaiaBankClient client;

    MaiaBankPixGatewayAdapter(MaiaBankClient client) {
        this.client = client;
    }

    @Override
    public void send() {
        client.receiversPix();
    }
}
