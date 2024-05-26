package com.dowglasmaia.exactbank.config;


import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.provider.model.ResponseErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@EnableWebMvc
@RestControllerAdvice
public class HandlerException extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ObjectNotFoundExeption.class)
    public final ResponseEntity<ResponseErrorDTO> handlerObjectNotFoundExeption(Exception ex){
        ResponseErrorDTO exceptionResponse = new ResponseErrorDTO();
        exceptionResponse.setCode(HttpStatus.NOT_FOUND.name());
        exceptionResponse.setMessage(ex.getMessage());
        return new ResponseEntity<ResponseErrorDTO>(exceptionResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnprocessableEntityExeption.class)
    public final ResponseEntity<ResponseErrorDTO> handlerUnprocessableEntityExeption(Exception ex){
        ResponseErrorDTO exceptionResponse = new ResponseErrorDTO();
        exceptionResponse.setCode(HttpStatus.UNPROCESSABLE_ENTITY.name());
        exceptionResponse.setMessage(ex.getMessage());
        return new ResponseEntity<ResponseErrorDTO>(exceptionResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public final ResponseEntity<ResponseErrorDTO> handleIllegalArgumentException(IllegalArgumentException ex){
        ResponseErrorDTO exceptionResponse = new ResponseErrorDTO();
        exceptionResponse.setCode(HttpStatus.UNPROCESSABLE_ENTITY.name());
        exceptionResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }


}
