package com.dowglasmaia.financialtransaction.shared.error;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/** The caller is authenticated but not allowed (for example a token whose subject is not linked to any user). */
@Getter
@Setter
public class ForbiddenExeption extends RuntimeException {

    protected String message;
    protected HttpStatus httpStatus;

    public ForbiddenExeption(String message) {
        super(message);
        this.message = message;
        this.httpStatus = HttpStatus.FORBIDDEN;
    }
}
