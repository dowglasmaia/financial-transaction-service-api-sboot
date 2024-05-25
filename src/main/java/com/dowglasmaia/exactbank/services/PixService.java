package com.dowglasmaia.exactbank.services;

import com.dowglasmaia.provider.model.PixRequestDTO;

public interface PixService {

    void makeTransfer(PixRequestDTO request);

}
