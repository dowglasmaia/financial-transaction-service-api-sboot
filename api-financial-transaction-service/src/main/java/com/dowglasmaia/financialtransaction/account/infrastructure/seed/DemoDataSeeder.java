package com.dowglasmaia.financialtransaction.account.infrastructure.seed;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.application.port.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AccountRepository accountRepository;


    /** The token subject to link to the demo customer (SEED_OWNER_SUB). Blank = leave the user unlinked. */
    @Value("${app.seed.owner-subject:}")
    private String ownerSubject;

    /** Optional token subject of a SECOND demo customer (SEED_SECOND_SUB), to try two real logins side by side. */
    @Value("${app.seed.second-subject:}")
    private String secondSubject;

    public DemoDataSeeder(UserRepository userRepository,
                          AgencyRepository agencyRepository,
                          AccountRepository accountRepository){
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

    private Account createSecondAccount(Agency agency){
        User user = userRepository.save(User.create("Second Demo User", "52998224725", "second.user@example.test", 11, 999997777)
              .linkedTo(secondSubject));
        return accountRepository.save(Account.create(agency, user, "3033", BigDecimal.valueOf(1000.0)));
    }

    /** Points the demo customer at the configured token subject (also when the data already existed with another one). */
    private void link(User user, String subject){
        if (subject != null && !subject.isBlank() && !subject.equals(user.getExternalSubject())) {
            userRepository.save(user.linkedTo(subject));
        }
    }

    private User createUser(){
        return userRepository.save(User.create("Demo User", "11144477735", "demo.user@example.test", 11, 999998888));
    }

    private Agency createAgency(){
        return agencyRepository.save(Agency.create("0211"));
    }

    private Account createAccount(Agency agency, User user){
        return accountRepository.save(Account.create(agency, user, "2022", BigDecimal.valueOf(5000.0)));
    }

}
