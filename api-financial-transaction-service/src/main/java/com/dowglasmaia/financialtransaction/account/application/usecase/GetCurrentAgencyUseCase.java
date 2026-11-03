package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AgencyRepository;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** The id of the agency the operations run in (the demo bank has one). */
@Slf4j
@Service
public class GetCurrentAgencyUseCase {

    private static final String CURRENT_AGENCY_NUMBER = "0211";

    private final AgencyRepository agencies;

    public GetCurrentAgencyUseCase(AgencyRepository agencies) {
        this.agencies = agencies;
    }

    public String execute() {
        log.info("Start getCurrentAgency");
        return agencies.findByNumber(CURRENT_AGENCY_NUMBER)
              .orElseThrow(() -> new ObjectNotFoundExeption("Agency not found."))
              .getId();
    }
}
