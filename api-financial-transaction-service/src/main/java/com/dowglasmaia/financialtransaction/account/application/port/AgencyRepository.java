package com.dowglasmaia.financialtransaction.account.application.port;

import com.dowglasmaia.financialtransaction.account.domain.Agency;
import java.util.Optional;

public interface AgencyRepository {

    Optional<Agency> findByNumber(String number);

    Agency save(Agency agency);
}
