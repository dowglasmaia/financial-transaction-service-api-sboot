package com.dowglasmaia.financialtransaction.services.client.account;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AccountEntity;

public interface FindAccountByNumberAndUserIdService {

    AccountEntity findByNumberAndUserId();

}
