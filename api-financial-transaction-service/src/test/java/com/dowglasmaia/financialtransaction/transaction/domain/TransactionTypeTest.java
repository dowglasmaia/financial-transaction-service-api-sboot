package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The statement classification (BR-52): a deposit is incoming; Pix, recharge and withdrawal are outgoing. */
class TransactionTypeTest {

    @Test
    void onlyADepositIsIncoming() {
        assertThat(TransactionType.DEPOSIT.isIncoming()).isTrue();
        assertThat(TransactionType.incoming()).containsExactly(TransactionType.DEPOSIT);
    }

    @Test
    void pixRechargeAndWithdrawalAreOutgoing() {
        assertThat(TransactionType.outgoing()).containsExactlyInAnyOrder(TransactionType.PIX, TransactionType.PHONE_RECHARGE, TransactionType.WITHDRAW);
        for (TransactionType type : TransactionType.values()) {
            assertThat(type.isIncoming()).isNotEqualTo(type.isOutgoing());
        }
    }
}
