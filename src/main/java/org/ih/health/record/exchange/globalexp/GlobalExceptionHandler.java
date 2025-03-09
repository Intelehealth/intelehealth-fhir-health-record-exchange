package org.ih.health.record.exchange.globalexp;

import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;

import org.ih.health.record.exchange.exp.InvalidParamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler({ InvalidParamException.class })
	public ResponseEntity<?> badRequest(InvalidParamException ex, HttpServletRequest request) {
		ex.printStackTrace();
		ErrorResponse response = new ErrorResponse();
		response.setTimestamp(LocalDateTime.now());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
		response.setPath(request.getRequestURI());
		response.setMessage(ex.getMessage());
		return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
	}

}
