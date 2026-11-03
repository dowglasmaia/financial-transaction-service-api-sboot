package com.dowglasmaia.financialtransaction.shared.error;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class UnprocessableEntityException extends RuntimeException{

    protected String message;
    protected HttpStatus httpStatus;

    /** The status is what the handler answers for this exception ({@link HttpStatus#UNPROCESSABLE_ENTITY}); callers need not import it. */
    public UnprocessableEntityException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public UnprocessableEntityException(String message, HttpStatus httpStatus) {
        super(message);
        this.message =  message;
        this.httpStatus = httpStatus;
    }
}
