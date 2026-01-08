package org.wearefrank.ladybug.web.springmvc.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class ExceptionHandlers {
	@ExceptionHandler
	public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
		return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
	}
}
