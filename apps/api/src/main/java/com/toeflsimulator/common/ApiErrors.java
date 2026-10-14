package com.toeflsimulator.common;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ProblemDetail problem(ApiException error) {
        var result = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(error.status), error.getMessage());
        result.setType(URI.create("urn:toefl:problem:" + error.code));
        result.setProperty("code", error.code);
        return result;
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail invalid(Exception error) { return problem(new ApiException(422, "INVALID_REQUEST", "Invalid request.")); }
}
