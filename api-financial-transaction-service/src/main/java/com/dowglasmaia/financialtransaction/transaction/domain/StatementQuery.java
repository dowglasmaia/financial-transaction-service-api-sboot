package com.dowglasmaia.financialtransaction.transaction.domain;

import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;

/** What the caller asked of the statement: the period, an optional type filter, the sort and the page. */
public record StatementQuery(StatementPeriod period, TransactionType type, Sort sort, boolean ascending, int page, int size) {

    public enum Sort { DATE, AMOUNT }

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static StatementQuery of(StatementPeriod period, String type, String sort, String order, Integer page, Integer size) {
        return new StatementQuery(period, typeOf(type), sortOf(sort), ascendingOf(order), pageOf(page), sizeOf(size));
    }

    private static TransactionType typeOf(String type) {
        if (type == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException("type must be one of: DEPOSIT, PIX, PHONE_RECHARGE, WITHDRAW");
        }
    }

    private static Sort sortOf(String sort) {
        if (sort == null || "date".equals(sort)) {
            return Sort.DATE;
        }
        if ("amount".equals(sort)) {
            return Sort.AMOUNT;
        }
        throw new UnprocessableEntityException("sort must be one of: date, amount");
    }

    private static boolean ascendingOf(String order) {
        if (order == null || "desc".equals(order)) {
            return false;
        }
        if ("asc".equals(order)) {
            return true;
        }
        throw new UnprocessableEntityException("order must be asc or desc");
    }

    private static int pageOf(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new UnprocessableEntityException("page must be 0 or greater");
        }
        return page;
    }

    private static int sizeOf(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new UnprocessableEntityException("size must be between 1 and 100");
        }
        return size;
    }
}
