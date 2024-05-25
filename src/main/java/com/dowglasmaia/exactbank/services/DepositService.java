package com.dowglasmaia.exactbank.services;

import com.dowglasmaia.provider.model.DepositRequestDTO;

public interface DepositService {

    void makeDeposit(DepositRequestDTO request);

}
