package com.dowglasmaia.financialtransaction.services.client;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.User;
import com.dowglasmaia.financialtransaction.exeptions.ForbiddenExeption;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The authenticated caller: the token subject ("sub") resolved to a local user and its single account (decision D-25).
 * Replaces the fake global identity. There is no auto-provisioning: a subject that is not linked to a user gets 403.
 * Registered as the bean "currentUser" so method security can ask {@code @currentUser.linked}.
 */
@Component("currentUser")
public class CurrentUser {

    static final String NOT_LINKED = "Access denied: this identity is not linked to a customer.";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public CurrentUser(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    /** The "sub" claim of the access token of the current request. */
    public String subject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return token.getToken().getSubject();
        }
        throw new ForbiddenExeption("Access denied.");
    }

    public Optional<User> user() {
        return userRepository.findByExternalSubject(subject());
    }

    public boolean isLinked() {
        return user().isPresent();
    }

    /** The caller's account (one per user). Not linked, or linked without an account: 403. */
    public Account account() {
        User user = user().orElseThrow(() -> new ForbiddenExeption(NOT_LINKED));
        return accountRepository.findFirstByUserIdOrderByNumberAsc(user.getId())
              .orElseThrow(() -> new ForbiddenExeption(NOT_LINKED));
    }
}
