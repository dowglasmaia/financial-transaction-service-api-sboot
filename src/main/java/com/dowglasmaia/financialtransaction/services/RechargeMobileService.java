package com.dowglasmaia.financialtransaction.services;

import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;

public interface RechargeMobileService {

    void makeRecharge(MobileRechargeRequestDTO request);

}
