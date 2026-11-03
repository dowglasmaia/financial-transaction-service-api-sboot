package com.dowglasmaia.financialtransaction.account.domain;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByExternalSubject(String externalSubject);

    User save(User user);
}
