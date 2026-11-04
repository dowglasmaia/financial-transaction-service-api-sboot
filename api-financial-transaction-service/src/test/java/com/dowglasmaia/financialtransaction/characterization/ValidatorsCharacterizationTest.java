package com.dowglasmaia.financialtransaction.characterization;

import com.dowglasmaia.financialtransaction.transaction.api.ValidateBodyRequest;
import com.dowglasmaia.financialtransaction.transaction.domain.CPFValidator;
import com.dowglasmaia.financialtransaction.transaction.domain.EmailValidator;
import com.dowglasmaia.financialtransaction.transaction.domain.PhoneValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit characterization of the hand-written validators (spec BE-01). */
class ValidatorsCharacterizationTest {

    @Test
    void cpfAcceptsValidNumbersWithOrWithoutMask() {
        assertThat(CPFValidator.isCpf("82215344040")).isTrue();
        assertThat(CPFValidator.isCpf("822.153.440-40")).isTrue();
    }

    @Test
    void cpfRejectsWrongCheckDigitsAndNonNumeric() {
        assertThat(CPFValidator.isCpf("12345678900")).isFalse();
        assertThat(CPFValidator.isCpf("abcdefghijk")).isFalse();
    }

    @Test
    void cpfWithOneCharacterThrows() { // characterizes F-11: no length check
        assertThatThrownBy(() -> CPFValidator.isCpf("1")).isInstanceOf(StringIndexOutOfBoundsException.class);
    }

    @Test
    void cpfWithRepeatedDigitsIsAccepted() { // characterizes F-11: "000.000.000-00" passes the digit algorithm
        assertThat(CPFValidator.isCpf("00000000000")).isTrue();
    }

    @Test
    void phoneRegexIsAreaCodePlusNineDigitsStartingWithNine() {
        assertThat(PhoneValidator.isValidPhoneNumber("11999998888")).isTrue();
        assertThat(PhoneValidator.isValidPhoneNumber("1199999888")).isFalse();
        assertThat(PhoneValidator.isValidPhoneNumber("11899998888")).isFalse();
        assertThat(PhoneValidator.isValidPhoneNumber("01999998888")).isFalse();
        assertThat(PhoneValidator.isValidPhoneNumber(null)).isFalse();
    }

    @Test
    void emailRegex() {
        assertThat(EmailValidator.isValidEmail("a.b-c@example.com")).isTrue();
        assertThat(EmailValidator.isValidEmail("nope")).isFalse();
        assertThat(EmailValidator.isValidEmail("")).isFalse();
        assertThat(EmailValidator.isValidEmail(null)).isFalse();
    }

    @Test
    void everyValidatorRejectsANullBodyWithAMessage() { // flipped by SL-03 (was F-11: NullPointerException)
        assertThatThrownBy(() -> ValidateBodyRequest.validatorPixRequest(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Request body is required");
        assertThatThrownBy(() -> ValidateBodyRequest.validatorDepositRequest(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Request body is required");
        assertThatThrownBy(() -> ValidateBodyRequest.validateWithdrawRequest(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Request body is required");
    }

    @Test
    void mobileRechargeValidatorRejectsANullBodyWithAMessage() {
        assertThatThrownBy(() -> ValidateBodyRequest.validateMobileRechargeRequest(null))
              .isInstanceOf(IllegalArgumentException.class).hasMessage("Request body is required");
    }
}
