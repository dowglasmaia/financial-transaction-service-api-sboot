package com.dowglasmaia.financialtransaction.services;

import com.dowglasmaia.provider.model.WithdrawRequestDTO;

public interface AtmWithdrawalService {

    void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest);
}
