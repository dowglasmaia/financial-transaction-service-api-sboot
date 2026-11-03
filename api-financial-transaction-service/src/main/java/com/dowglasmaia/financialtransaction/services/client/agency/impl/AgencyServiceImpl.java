package com.dowglasmaia.financialtransaction.services.client.agency.impl;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyEntity;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyJpaRepository;
import com.dowglasmaia.financialtransaction.services.client.agency.AgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AgencyServiceImpl implements AgencyService {

    private final AgencyJpaRepository agencyRepository;

    @Autowired
    public AgencyServiceImpl(AgencyJpaRepository agencyRepository){
        this.agencyRepository = agencyRepository;
    }

    @Override
    public String getCurrentAgency(){
        log.info("Start getCurrentAgency");

        // Lógica para obter a agência atual

        var agency = findByNumber("0211");
        return agency.getId();
    }

    @Override
    public AgencyEntity findByNumber(String number){
        return agencyRepository.findByNumber(number)
              .orElseThrow(()-> new ObjectNotFoundExeption("Agency not found.", HttpStatus.FOUND) );
    }
}
