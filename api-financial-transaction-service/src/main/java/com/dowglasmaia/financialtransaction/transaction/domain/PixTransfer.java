package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;

/** Value object: what a customer asks for in a Pix transfer. */
public record PixTransfer(PixKey key, BigDecimal amount) {
}
