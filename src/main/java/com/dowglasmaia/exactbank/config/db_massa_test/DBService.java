package com.dowglasmaia.exactbank.config.db_massa_test;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.entity.Agency;
import com.dowglasmaia.exactbank.entity.User;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.repository.AgencyRepository;
import com.dowglasmaia.exactbank.repository.UserRepository;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
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

    @SneakyThrows
    public void instanciateTestDatabase()   {
        var user = createUser();

        var agency = createAgency();

        var account = createAccount(agency, user);

        Thread.sleep(1000);

        userAccountDTO.setId(String.valueOf(UUID.randomUUID()));
        userAccountDTO.setUserId(user.getId());
        userAccountDTO.setAgencyNumber(agency.getNumber());
        userAccountDTO.setNumber(account.getNumber());

    }

    private User createUser(){
        return userRepository.save(User.builder()
              .name("Dowglas Maia")
              .CPF("82215344040")
              .email("dowglasmaia@live.com")
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
