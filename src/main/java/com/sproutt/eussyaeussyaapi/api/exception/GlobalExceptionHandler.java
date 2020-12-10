package com.sproutt.eussyaeussyaapi.api.exception;

import com.sproutt.eussyaeussyaapi.api.exception.dto.ErrorCode;
import com.sproutt.eussyaeussyaapi.api.exception.dto.ValidateError;
import com.sproutt.eussyaeussyaapi.api.exception.dto.ValidationErrorResponse;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Optional;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSourceAccessor messageSourceAccessor;

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        log.info("handleMethodArgumentNotValidException : {}", exception);

        ValidationErrorResponse response = new ValidationErrorResponse();
        response.of(ErrorCode.INVALID_INPUT_VALUE);

        List<ObjectError> errors = exception.getBindingResult().getAllErrors();

        for (ObjectError error : errors) {
            FieldError fieldError = (FieldError) error;

            response.addValidateError(new ValidateError(fieldError.getField(), getErrorMessage(fieldError)));

            log.info("error code: {}", getFirstCode(fieldError));
            log.info("error arguments: {}", fieldError.getArguments());
            log.info("default message: {}", fieldError.getDefaultMessage());
            log.info("changed message: {}", getErrorMessage(fieldError));
        }

        log.info("response: {}", response.toString());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity handleNotFoundException(NotFoundException exception) {
        log.info("handleNotFoundException : {}", exception);

        return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(exception.getMessage());
    }

    private String getErrorMessage(FieldError fieldError) {
        Optional<String> code = getFirstCode(fieldError);

        if (code.orElse(null) == null) {
            return null;
        }

        return messageSourceAccessor.getMessage(code.get(), fieldError.getArguments(), fieldError.getDefaultMessage());
    }

    private Optional<String> getFirstCode(FieldError fieldError) {
        String[] codes = fieldError.getCodes();

        if (codes == null || codes.length == 0) {
            return Optional.empty();
        }

        return Optional.of(codes[0]);
    }
}
