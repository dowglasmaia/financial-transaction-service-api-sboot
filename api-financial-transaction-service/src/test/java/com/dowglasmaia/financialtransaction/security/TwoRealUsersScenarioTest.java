package com.dowglasmaia.financialtransaction.security;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.UserEntity;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two real Auth0 test users (their user_id values, no secrets) played end to end with tokens from the mock issuer:
 * user 1 is linked to the seeded demo customer (account 2022), user 2 to a second customer created here (account 9998).
 * Everything created is removed afterwards and the demo customer gets its previous subject back.
 */
class TwoRealUsersScenarioTest extends AbstractIntegrationTest {

    static final String USER_1 = "auth0|6ae9a7d25898a59fc803d242";
    static final String USER_2 = "auth0|6ae9a8f5cc59993b7f7394f0";
    static final String RANGE = "?initial-date=01/01/2000&final-date=31/12/2100";

    private final String token1 = MockIssuer.tokenFor(USER_1);
    private final String token2 = MockIssuer.tokenFor(USER_2);
    private String previousSubject;
    private AccountEntity second;

    @BeforeEach
    void linkTheTwoUsers() {
        UserEntity demo = seedAccount().getUser();
        previousSubject = demo.getExternalSubject();
        demo.setExternalSubject(USER_1);
        userRepository.save(demo);
        second = createOtherAccount("9998", new BigDecimal("1000.00"), USER_2);
    }

    @AfterEach
    void restoreTheSeed() {
        UserEntity demo = seedAccount().getUser();
        demo.setExternalSubject(previousSubject);
        userRepository.save(demo);
    }

    @Test
    void bothUsersSignInAndSeeOnlyTheirOwnAccount() throws Exception {
        assertThat(bodyOf(getAs(token1, RANGE).andExpect(status().isOk())).get("balance").decimalValue()).isEqualByComparingTo("5000");
        assertThat(bodyOf(getAs(token2, RANGE).andExpect(status().isOk())).get("balance").decimalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void user1PaysUser2AndOnlyUser2SeesIt() throws Exception {
        postAs(token1, "/deposit", "{\"agency\":\"0211\",\"account\":\"9998\",\"amount\":250.00}").andExpect(status().isNoContent());

        assertThat(bodyOf(getAs(token2, RANGE)).get("transaction")).hasSize(1);
        assertThat(bodyOf(getAs(token1, RANGE)).get("transaction")).isEmpty();
        assertThat(sameValue(new BigDecimal("1250.00"), balanceOf("9998"))).isTrue();
    }

    @Test
    void neitherUserCanReadOrDebitTheOther() throws Exception {
        var ofUser1 = ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("9.00"), seedAccount().getId(), OffsetDateTime.now().minusHours(1));

        getAs(token2, "/" + ofUser1.getId()).andExpect(status().isNotFound());
        postAs(token2, "/atm-withdrawal", "{\"amount\":10.00,\"agency\":\"0211\",\"numberAccount\":\"2022\"}")
              .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Account not found."));
        postAs(token1, "/atm-withdrawal", "{\"amount\":10.00,\"agency\":\"0211\",\"numberAccount\":\"9998\"}")
              .andExpect(status().isNotFound());

        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(sameValue(new BigDecimal("1000.00"), balanceOf("9998"))).isTrue();
    }

    @Test
    void aThirdValidLoginWithoutACustomerIsForbidden() throws Exception {
        getAs(MockIssuer.tokenFor("auth0|000000000000000000000000"), RANGE).andExpect(status().isForbidden());
    }

    @Test
    void afterTheTestTheSecondCustomerIsGone() {
        // the base class removes what createOtherAccount created; this just documents that it was ours
        assertThat(accountRepository.findByNumber("9998")).isPresent().get().isEqualTo(second);
    }
}
