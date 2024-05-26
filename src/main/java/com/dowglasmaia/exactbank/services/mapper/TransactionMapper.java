package com.dowglasmaia.exactbank.services.mapper;

import com.dowglasmaia.exactbank.entity.Transaction;
import com.dowglasmaia.exactbank.services.model.TransactionRequestDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    Transaction toTransaction(TransactionRequestDTO request);
}
