package org.wearefrank.ladybug.web.springmvc.api;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wearefrank.ladybug.Span;
import org.wearefrank.ladybug.web.common.CollectorApiImpl;

import lombok.Setter;

@RestController
@RequestMapping("/collector")
@RolesAllowed("IbisWebService")
public class CollectorApi {
	@Autowired
	private @Setter CollectorApiImpl delegate;

	@PostMapping(value = "/")
	public ResponseEntity<Void> collectSpans(Span[] trace) {
		delegate.processSpans(trace);
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> collectSpansJson(Span[] trace) {
		delegate.processSpans(trace);
		return ResponseEntity.ok().build();
	}

}