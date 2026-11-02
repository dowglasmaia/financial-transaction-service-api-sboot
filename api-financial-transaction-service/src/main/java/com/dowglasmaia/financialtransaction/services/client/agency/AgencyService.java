package com.dowglasmaia.financialtransaction.services.client.agency;

import com.dowglasmaia.financialtransaction.entity.Agency;

public interface AgencyService {
    String getCurrentAgency();

    Agency  findByNumber(String number);

}
