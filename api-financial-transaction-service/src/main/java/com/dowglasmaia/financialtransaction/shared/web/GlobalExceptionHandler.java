package com.dowglasmaia.financialtransaction.shared.web;


import com.dowglasmaia.financialtransaction.shared.error.ForbiddenException;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundException;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import com.dowglasmaia.provider.model.ResponseErrorDTO;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@EnableWebMvc
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    public final ResponseEntity<ResponseErrorDTO> handlerObjectNotFoundExeption(Exception ex){
        ResponseErrorDTO exceptionResponse = new ResponseErrorDTO();
        exceptionResponse.setCode(HttpStatus.NOT_FOUND.name());
        exceptionResponse.setMessage(ex.getMessage());
        return new ResponseEntity<ResponseErrorDTO>(exceptionResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public final ResponseEntity<ResponseErrorDTO> handlerForbiddenExeption(Exception ex){
        ResponseErrorDTO exceptionResponse = new ResponseErrorDTO();
        exceptionResponse.setCode(HttpStatus.FORBIDDEN.name());
        exceptionResponse.setMessage(ex.getMessage());
        return new ResponseEntity<ResponseErrorDTO>(exceptionResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
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

    /**
     * A parameter that cannot be read as its type. The path id of GET /transactions/{id} stays a 400 (malformed id); every
     * other parameter (dates, page, size) is a 422 with the same envelope as the rest of the field validation.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request){
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        ResponseErrorDTO body = new ResponseErrorDTO();
        if ("id".equals(name)) {
            body.setCode(HttpStatus.BAD_REQUEST.name());
            body.setMessage("Invalid id: expected a UUID.");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
        body.setCode(HttpStatus.UNPROCESSABLE_ENTITY.name());
        body.setMessage(("from".equals(name) || "to".equals(name))
              ? "Invalid value for '" + name + "': expected yyyy-MM-dd."
              : "Invalid value for '" + name + "'.");
        return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /** A required query parameter that did not come: 422 like every other field problem (dates have their own wording). */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request){
        ResponseErrorDTO body = new ResponseErrorDTO();
        body.setCode(HttpStatus.UNPROCESSABLE_ENTITY.name());
        body.setMessage(("from".equals(ex.getParameterName()) || "to".equals(ex.getParameterName()))
              ? "Dates are required: send from and to."
              : "Missing parameter '" + ex.getParameterName() + "'.");
        return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
    }

}
