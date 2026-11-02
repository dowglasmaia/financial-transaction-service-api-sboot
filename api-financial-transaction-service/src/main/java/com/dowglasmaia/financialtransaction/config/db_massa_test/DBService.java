package com.dowglasmaia.financialtransaction.config.db_massa_test;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.Agency;
import com.dowglasmaia.financialtransaction.entity.User;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.repository.AgencyRepository;
import com.dowglasmaia.financialtransaction.repository.UserRepository;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DBService {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AccountRepository accountRepository;

    private UserAccountDTO userAccountDTO = new UserAccountDTO();

    public UserAccountDTO extractUserAccountFromJwt(){
        return userAccountDTO;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class UserAccountDTO {
        private String id;
        private String userId;
        private String agencyNumber;
        private String number;
    }


    @Autowired
    public DBService(UserRepository userRepository,
                     AgencyRepository agencyRepository,
                     AccountRepository accountRepository){
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Idempotent: creates the demo agency/user/account only when they do not exist yet, so the app can restart
     * on an existing database (the schema is no longer recreated). Always loads the acting identity.
     */
    @SneakyThrows
    public void instanciateTestDatabase()   {
        var agency = agencyRepository.findByNumber("0211").orElseGet(this::createAgency);

        var account = accountRepository.findByNumber("2022")
              .orElseGet(() -> createAccount(agency, createUser()));
        var user = account.getUser();

        Thread.sleep(1000);

        userAccountDTO.setId(String.valueOf(UUID.randomUUID()));
        userAccountDTO.setUserId(user.getId());
        userAccountDTO.setAgencyNumber(agency.getNumber());
        userAccountDTO.setNumber(account.getNumber());

    }

    private User createUser(){
        return userRepository.save(User.builder()
              .name("Demo User")
              .CPF("11144477735")
              .email("demo.user@example.test")
              .areaCode(11)
              .phoneNumber(999998888)
              .build());
    }

    private Agency createAgency(){
        return agencyRepository.save(
              Agency.builder()
                    .number("0211")
                    .build());
    }

    private Account createAccount(Agency agency, User user){
        return accountRepository.save(
              Account.builder()
                    .agency(agency)
                    .user(user)
                    .number("2022")
                    .balance(BigDecimal.valueOf(5000.0))
                    .build()
        );
    }

}
