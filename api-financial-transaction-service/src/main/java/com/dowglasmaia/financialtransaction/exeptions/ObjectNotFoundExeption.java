package com.dowglasmaia.financialtransaction.exeptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ObjectNotFoundExeption extends RuntimeException{

    protected String message;
    protected HttpStatus httpStatus;

    /** The status is what the handler answers for this exception ({@link HttpStatus#NOT_FOUND}); callers need not import it. */
    public ObjectNotFoundExeption(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public ObjectNotFoundExeption(String message, HttpStatus httpStatus) {
        super(message);
        this.message =  message;
        this.httpStatus = httpStatus;
    }
}
