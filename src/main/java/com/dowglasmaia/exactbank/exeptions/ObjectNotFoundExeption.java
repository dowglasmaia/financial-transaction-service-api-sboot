package com.dowglasmaia.exactbank.exeptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ObjectNotFoundExeption extends RuntimeException{

    protected String message;
    protected HttpStatus httpStatus;

    public ObjectNotFoundExeption(String message, HttpStatus httpStatus) {
        super(message);
        this.message =  message;
        this.httpStatus = httpStatus;
    }
}
