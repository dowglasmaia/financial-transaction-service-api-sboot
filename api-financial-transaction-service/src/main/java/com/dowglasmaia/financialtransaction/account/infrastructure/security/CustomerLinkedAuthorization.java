package com.dowglasmaia.financialtransaction.account.infrastructure.security;

import com.dowglasmaia.financialtransaction.account.application.usecase.CheckCallerLinkedUseCase;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * The whole business API needs the role {@code customer} AND a token subject linked to a local user (otherwise 403).
 * Enforced in the filter chain, on purpose: the generated API interface exposes its handlers as default methods that
 * call the controller methods internally, so annotation-based method security would be bypassed.
 */
@Component("customerLinkedAuthorization")
class CustomerLinkedAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(CustomerLinkedAuthorization.class);

    private final CheckCallerLinkedUseCase checkCallerLinked;

    CustomerLinkedAuthorization(CheckCallerLinkedUseCase checkCallerLinked) {
        this.checkCallerLinked = checkCallerLinked;
    }

    /** Same decision as before; a refusal of a signed-in caller is logged with its reason (never a token or a subject). */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication caller = authentication.get();
        List<String> roles = caller == null ? List.of() : caller.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (!roles.contains("ROLE_CUSTOMER")) {
            if (caller != null && !(caller instanceof AnonymousAuthenticationToken)) {
                log.info("Access denied (403): the token has no ROLE_CUSTOMER (roles in the token: {})", roles);
            }
            return new AuthorizationDecision(false);
        }
        boolean linked = checkCallerLinked.execute();
        if (!linked) {
            log.info("Access denied (403): the token subject is not linked to a customer");
        }
        return new AuthorizationDecision(linked);
    }
}
