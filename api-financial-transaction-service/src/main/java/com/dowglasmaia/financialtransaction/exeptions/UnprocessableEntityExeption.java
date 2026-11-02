package com.dowglasmaia.financialtransaction.exeptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class UnprocessableEntityExeption extends RuntimeException{

    protected String message;
    protected HttpStatus httpStatus;

    public UnprocessableEntityExeption(String message, HttpStatus httpStatus) {
        super(message);
        this.message =  message;
        this.httpStatus = httpStatus;
    }
}
