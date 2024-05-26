package com.dowglasmaia.exactbank.services.client.agency;

import com.dowglasmaia.exactbank.entity.Agency;

public interface AgencyService {
    String getCurrentAgency();

    Agency  findByNumber(String number);

}
