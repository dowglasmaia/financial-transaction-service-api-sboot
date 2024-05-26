package com.dowglasmaia.exactbank.services.client.maiaBank.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountModel {

    private String id;
    private String number;
    private BigDecimal balance;
    private String agencyNumber;
    private String userId;
}
