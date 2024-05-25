package com.dowglasmaia.exactbank.services;

import com.dowglasmaia.exactbank.entity.Agency;

public interface AgencyService {
    String getCurrentAgency();

    Agency  findByNumber(String number);

}
