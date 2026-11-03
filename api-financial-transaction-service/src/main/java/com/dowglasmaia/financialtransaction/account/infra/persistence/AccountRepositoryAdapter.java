package com.dowglasmaia.financialtransaction.account.infra.persistence;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.domain.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Implements the domain port over Spring Data; the only place that maps account entities to domain objects and back. */
@Component
class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accounts;

    AccountRepositoryAdapter(AccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Account> findByNumber(String number) {
        return accounts.findByNumber(number).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> findFirstByUserId(String userId) {
        return accounts.findFirstByUserIdOrderByNumberAsc(userId).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> findByAgencyAndNumber(String agencyId, String number) {
        return accounts.findByAgencyAndNumber(agencyId, number).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Account save(Account account) {
        return toDomain(accounts.save(toEntity(account)));
    }

    /** One atomic statement (see {@link AccountJpaRepository#debit}); never a read followed by a write. */
    @Override
    public boolean tryDebit(String accountId, BigDecimal amount) {
        return accounts.debit(accountId, amount) > 0;
    }

    @Override
    public boolean credit(String accountId, BigDecimal amount) {
        return accounts.credit(accountId, amount) > 0;
    }

    static Account toDomain(AccountEntity entity) {
        return Account.restore(entity.getId(), AgencyRepositoryAdapter.toDomain(entity.getAgency()),
              UserRepositoryAdapter.toDomain(entity.getUser()), entity.getNumber(), entity.getBalance());
    }

    static AccountEntity toEntity(Account account) {
        return AccountEntity.builder()
              .id(account.getId())
              .agency(AgencyRepositoryAdapter.toEntity(account.getAgency()))
              .user(UserRepositoryAdapter.toEntity(account.getUser()))
              .number(account.getNumber())
              .balance(account.getBalance())
              .build();
    }
}
