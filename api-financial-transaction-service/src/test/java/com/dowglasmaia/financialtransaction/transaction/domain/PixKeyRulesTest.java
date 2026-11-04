package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SL-06 (F-11): the Pix key rules. The same accept/reject cases are mirrored by the frontend validators
 * (`pix-key.spec.ts`): change one side and the other must follow. A rule never throws: a bad value is just invalid.
 */
class PixKeyRulesTest {

    private static boolean valid(PixKeyType type, String value) {
        return new PixKey(type, value).isValid();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "82215344040", "822.153.440-40", "11144477735"})
    void validCpfs(String cpf) {
        assertThat(valid(PixKeyType.CPF, cpf)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "5299822472", "529982247251", "52998224724", "529.982.247-24", "00000000000", "111.111.111-11",
          "99999999999", "529.982.24725", "529982.247-25", "abcdefghijk", " 52998224725", "529 982 247 25", "5299822472a"})
    void invalidCpfs(String cpf) {
        assertThat(valid(PixKeyType.CPF, cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.b-c@example.com", "user+tag@mail.example.com.br", "a_b@x.io", "maria@maiabank.com.br", "x@museum.museum"})
    void validEmails(String email) {
        assertThat(valid(PixKeyType.EMAIL, email)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nope", "a@b", "@example.com", "a@.com", "a..b@example.com", ".a@example.com", "a.@example.com",
          "a@-example.com", "a@example-.com", "a@example..com", "a b@example.com", "a@example.c", "a@example.c0m", "a@@example.com",
          "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@example.com"})
    void invalidEmails(String email) {
        assertThat(valid(PixKeyType.EMAIL, email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11999998888", "21912345678", "99900000000"})
    void validPhones(String phone) {
        assertThat(valid(PixKeyType.PHONE, phone)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1199999888", "119999988888", "11899998888", "01999998888", "10999998888", "(11)99999-8888", "+5511999998888", "11 99999 8888", "11a99998888"})
    void invalidPhones(String phone) {
        assertThat(valid(PixKeyType.PHONE, phone)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25"})
    void aCpfIsMaskedForLogsKeepingOnlyTheCheckDigits(String cpf) {
        assertThat(new PixKey(PixKeyType.CPF, cpf).masked()).isEqualTo("***.***.***-25");
    }

    @ParameterizedTest
    @ValueSource(strings = {"maria@maiabank.com.br"})
    void anEmailIsMaskedKeepingItsFirstLetterAndDomain(String email) {
        assertThat(new PixKey(PixKeyType.EMAIL, email).masked()).isEqualTo("m***@maiabank.com.br");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11999998888"})
    void aPhoneIsMaskedKeepingItsLastFourDigits(String phone) {
        assertThat(new PixKey(PixKeyType.PHONE, phone).masked()).isEqualTo("*******8888");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"x"})
    void maskingNeverThrowsAndNeverShowsAShortOrBadValue(String value) {
        for (PixKeyType type : PixKeyType.values()) {
            assertThat(new PixKey(type, value).masked()).doesNotContain("x");
        }
    }
}
