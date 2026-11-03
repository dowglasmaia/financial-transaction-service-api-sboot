package com.dowglasmaia.financialtransaction.services.client.agency;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyEntity;

public interface AgencyService {
    String getCurrentAgency();

    AgencyEntity  findByNumber(String number);

}
