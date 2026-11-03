package com.dowglasmaia.financialtransaction.account.domain;

import java.util.Optional;

/** Output port: who is calling. The infrastructure reads the authenticated token; empty when there is none. */
public interface CallerIdentity {

    /** The identity-provider subject ("sub") of the current request. */
    Optional<String> subject();
}
