package com.dowglasmaia.financialtransaction.account.infrastructure.security;

import com.dowglasmaia.financialtransaction.account.application.usecase.CheckCallerLinkedUseCase;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * The whole business API needs the role {@code customer} AND a token subject linked to a local user (otherwise 403).
 * Enforced in the filter chain, on purpose: the generated API interface exposes its handlers as default methods that
 * call the controller methods internally, so annotation-based method security would be bypassed.
 */
@Component("customerLinkedAuthorization")
class CustomerLinkedAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private final CheckCallerLinkedUseCase checkCallerLinked;

    CustomerLinkedAuthorization(CheckCallerLinkedUseCase checkCallerLinked) {
        this.checkCallerLinked = checkCallerLinked;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication caller = authentication.get();
        boolean customer = caller != null && caller.getAuthorities().stream()
              .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        return new AuthorizationDecision(customer && checkCallerLinked.execute());
    }
}
