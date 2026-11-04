package com.dowglasmaia.financialtransaction.transaction.application.port;

/**
 * Output port: the bank that receives the Pix. A failure of any kind is thrown to the caller, which then reverses the
 * debit; the call must never run inside a database transaction.
 */
public interface PixGateway {

    void send();
}
