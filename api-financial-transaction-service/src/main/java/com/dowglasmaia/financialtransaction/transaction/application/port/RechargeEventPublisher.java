package com.dowglasmaia.financialtransaction.transaction.application.port;

import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;

/**
 * Output port: announces that a mobile recharge was requested. The implementation must write the event in the
 * caller's transaction (outbox), so the debit, the ledger row and the event commit or roll back together.
 */
public interface RechargeEventPublisher {

    void publish(MobileRecharge recharge);
}
