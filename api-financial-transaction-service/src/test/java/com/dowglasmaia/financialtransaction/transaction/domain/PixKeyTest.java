package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PixKeyTest {

    @Test
    void aCpfKeyIsValidatedWithTheCpfAlgorithm() {
        assertThat(new PixKey(PixKeyType.CPF, "82215344040").isValid()).isTrue();
        assertThat(new PixKey(PixKeyType.CPF, "822.153.440-40").isValid()).isTrue();
        assertThat(new PixKey(PixKeyType.CPF, "12345678900").isValid()).isFalse();
    }

    @Test
    void anEmailKeyUsesTheEmailRule() {
        assertThat(new PixKey(PixKeyType.EMAIL, "a.b-c@example.com").isValid()).isTrue();
        assertThat(new PixKey(PixKeyType.EMAIL, "nope").isValid()).isFalse();
    }

    @Test
    void aPhoneKeyUsesThePhoneRule() {
        assertThat(new PixKey(PixKeyType.PHONE, "11999998888").isValid()).isTrue();
        assertThat(new PixKey(PixKeyType.PHONE, "1199999888").isValid()).isFalse();
    }

    @Test
    void theKeyTypeLabelIsTheLowerCaseValueOfTheContract() {
        assertThat(PixKeyType.CPF.label()).isEqualTo("cpf");
        assertThat(PixKeyType.EMAIL.label()).isEqualTo("email");
        assertThat(PixKeyType.PHONE.label()).isEqualTo("phone");
    }
}
