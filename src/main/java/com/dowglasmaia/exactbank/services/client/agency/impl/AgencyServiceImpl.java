package com.dowglasmaia.exactbank.services.client.agency.impl;

import com.dowglasmaia.exactbank.entity.Agency;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.repository.AgencyRepository;
import com.dowglasmaia.exactbank.services.client.agency.AgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository agencyRepository;

    @Autowired
    public AgencyServiceImpl(AgencyRepository agencyRepository){
        this.agencyRepository = agencyRepository;
    }

    @Override
    public String getCurrentAgency(){
        log.info("Start getCurrentAgency");

        // Lógica para obter a agência atual
        return "1120";
    }

    @Override
    public Agency findByNumber(String number){
        return agencyRepository.findByNumber(number)
              .orElseThrow(()-> new ObjectNotFoundExeption("Agency not found.", HttpStatus.FOUND) );
    }
}
