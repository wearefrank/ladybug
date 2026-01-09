package org.wearefrank.ladybug.web.springmvc.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.invoke.MethodHandles;


@RestControllerAdvice
public class ExceptionHandlers {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public ExceptionHandlers() {
		// TODO: Decrease log level.
		log.error("Constructing org.wearefrank.ladybug.web.springmvc.api.ExceptionHandlers");
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<?> handleAccessDeniedException(AuthorizationDeniedException e) {
		log.error("ExceptionHandlers.handleAccessDeniedException() captured exception", e);
		return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
	}
}
