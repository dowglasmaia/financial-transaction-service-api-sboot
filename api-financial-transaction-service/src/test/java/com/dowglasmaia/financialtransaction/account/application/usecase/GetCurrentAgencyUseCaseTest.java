package com.dowglasmaia.financialtransaction.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.port.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentAgencyUseCaseTest {

    @Mock AgencyRepository agencies;

    @Test
    void theCurrentAgencyIsTheDemoAgency0211() {
        when(agencies.findByNumber("0211")).thenReturn(Optional.of(Agency.restore("agency-1", "0211")));

        assertThat(new GetCurrentAgencyUseCase(agencies).execute()).isEqualTo("agency-1");
    }

    @Test
    void aMissingCurrentAgencyIsNotFound() {
        when(agencies.findByNumber("0211")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetCurrentAgencyUseCase(agencies).execute())
              .isInstanceOf(ObjectNotFoundException.class).hasMessage("Agency not found.");
    }
}
