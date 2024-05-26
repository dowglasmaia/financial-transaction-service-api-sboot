package com.dowglasmaia.exactbank.services;

import com.dowglasmaia.provider.model.WithdrawRequestDTO;

public interface AtmWithdrawalService {

    void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest);
}
