package com.dowglasmaia.financialtransaction.config.db_massa_test;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyEntity;
import com.dowglasmaia.financialtransaction.account.infra.persistence.UserEntity;
import com.dowglasmaia.financialtransaction.account.infra.persistence.AccountJpaRepository;
import com.dowglasmaia.financialtransaction.account.infra.persistence.AgencyJpaRepository;
import com.dowglasmaia.financialtransaction.account.infra.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DBService {

    private final UserJpaRepository userRepository;
    private final AgencyJpaRepository agencyRepository;
    private final AccountJpaRepository accountRepository;


    /** The token subject to link to the demo customer (SEED_OWNER_SUB). Blank = leave the user unlinked. */
    @Value("${app.seed.owner-subject:}")
    private String ownerSubject;

    /** Optional token subject of a SECOND demo customer (SEED_SECOND_SUB), to try two real logins side by side. */
    @Value("${app.seed.second-subject:}")
    private String secondSubject;

    @Autowired
    public DBService(UserJpaRepository userRepository,
                     AgencyJpaRepository agencyRepository,
                     AccountJpaRepository accountRepository){
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Idempotent: creates the demo agency/user/account only when they do not exist yet, so the app can restart
     * on an existing database (the schema is no longer recreated). Links the demo customer to the configured subject.
     */
    public void instanciateTestDatabase()   {
        var agency = agencyRepository.findByNumber("0211").orElseGet(this::createAgency);

        var account = accountRepository.findByNumber("2022")
              .orElseGet(() -> createAccount(agency, createUser()));
        link(account.getUser(), ownerSubject);

        if (secondSubject != null && !secondSubject.isBlank()) {
            var second = accountRepository.findByNumber("3033").orElseGet(() -> createSecondAccount(agency));
            link(second.getUser(), secondSubject);
        }
    }

    private AccountEntity createSecondAccount(AgencyEntity agency){
        UserEntity user = userRepository.save(UserEntity.builder()
              .name("Second Demo User")
              .CPF("52998224725")
              .email("second.user@example.test")
              .areaCode(11)
              .phoneNumber(999997777)
              .externalSubject(secondSubject)
              .build());
        return accountRepository.save(AccountEntity.builder()
              .agency(agency)
              .user(user)
              .number("3033")
              .balance(BigDecimal.valueOf(1000.0))
              .build());
    }

    /** Points the demo customer at the configured token subject (also when the data already existed with another one). */
    private void link(UserEntity user, String subject){
        if (subject != null && !subject.isBlank() && !subject.equals(user.getExternalSubject())) {
            user.setExternalSubject(subject);
            userRepository.save(user);
        }
    }

    private UserEntity createUser(){
        return userRepository.save(UserEntity.builder()
              .name("Demo User")
              .CPF("11144477735")
              .email("demo.user@example.test")
              .areaCode(11)
              .phoneNumber(999998888)
              .build());
    }

    private AgencyEntity createAgency(){
        return agencyRepository.save(
              AgencyEntity.builder()
                    .number("0211")
                    .build());
    }

    private AccountEntity createAccount(AgencyEntity agency, UserEntity user){
        return accountRepository.save(
              AccountEntity.builder()
                    .agency(agency)
                    .user(user)
                    .number("2022")
                    .balance(BigDecimal.valueOf(5000.0))
                    .build()
        );
    }

}
