package com.dowglasmaia.financialtransaction.account.application.port;

import com.dowglasmaia.financialtransaction.account.domain.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByExternalSubject(String externalSubject);

    User save(User user);
}
