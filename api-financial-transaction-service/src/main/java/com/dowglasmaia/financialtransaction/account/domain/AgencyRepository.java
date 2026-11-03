package com.dowglasmaia.financialtransaction.account.domain;

import java.util.Optional;

public interface AgencyRepository {

    Optional<Agency> findByNumber(String number);

    Agency save(Agency agency);
}
