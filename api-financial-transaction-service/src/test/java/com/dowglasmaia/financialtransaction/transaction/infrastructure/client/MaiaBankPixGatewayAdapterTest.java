package com.dowglasmaia.financialtransaction.transaction.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MaiaBankPixGatewayAdapterTest {

    @Mock MaiaBankClient client;

    @Test
    void sendingCallsTheMaiaBankClientOnce() {
        when(client.receiversPix()).thenReturn(ResponseEntity.noContent().build());

        new MaiaBankPixGatewayAdapter(client).send();

        verify(client).receiversPix();
    }

    @Test
    void aFailureOfTheClientPropagatesUntouched() {
        when(client.receiversPix()).thenThrow(new IllegalStateException("maiabank down"));

        assertThatThrownBy(() -> new MaiaBankPixGatewayAdapter(client).send())
              .isInstanceOf(IllegalStateException.class).hasMessage("maiabank down");
    }
}
