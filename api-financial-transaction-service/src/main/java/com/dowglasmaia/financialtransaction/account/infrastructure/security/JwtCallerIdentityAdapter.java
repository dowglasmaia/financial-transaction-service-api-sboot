package com.dowglasmaia.financialtransaction.account.infrastructure.security;

import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Reads the "sub" claim of the access token of the current request. */
@Component
class JwtCallerIdentityAdapter implements CallerIdentity {

    @Override
    public Optional<String> subject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return Optional.of(token.getToken().getSubject());
        }
        return Optional.empty();
    }
}
