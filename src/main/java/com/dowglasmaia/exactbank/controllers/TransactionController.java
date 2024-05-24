package com.dowglasmaia.exactbank.controllers;

import com.dowglasmaia.exactbank.services.PixService;
import com.dowglasmaia.provider.api.TransactionsApi;
import com.dowglasmaia.provider.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dowglasmaia.exactbank.controllers.ValidateBodyRequest.validatePixRequestDTO;

@RestController
@RequestMapping(value = "/api/v1")
public class TransactionController implements TransactionsApi {

    private final PixService pixService;

    @Autowired
    public TransactionController(PixService pixService){
        this.pixService = pixService;
    }


    @Override
    public ResponseEntity<Void> sendPix(PixRequestDTO body){

        validatePixRequestDTO(body);

        pixService.sendPix(body);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> deposit(DepositRequestDTO body){

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<TransactionsDTO> getBalance(){

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<TransactionsDTO> getTransactions(String initialDate, String finalDate){

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> rechargeMobile(MobileRechargeRequestDTO body){

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @Override
    public ResponseEntity<Void> withdraw(WithdrawRequestDTO body){

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


}
