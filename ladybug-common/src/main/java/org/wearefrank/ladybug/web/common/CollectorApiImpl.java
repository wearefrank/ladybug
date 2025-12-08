package org.wearefrank.ladybug.web.common;

import jakarta.inject.Inject;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.Span;
import org.wearefrank.ladybug.TestTool;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

@Component
public class CollectorApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(Span[] trace) {
		ArrayList<String> parentIds = new ArrayList<>();
		for (Span span: trace) {
			if (span.getParentId() != null && !parentIds.contains(span.getParentId())) {
				parentIds.add(span.getParentId());
			}
		}
		ArrayList<String> endpoints = new ArrayList<>();
		for (int i = trace.length - 1; i >= 0; i--) {
			if (trace[i].getParentId() == null) {
				testTool.startpoint(trace[i].getTraceId(), null, trace[i].getName(), trace[i].toHashmap().toString());
				endpoints.add(trace[i].getName());
			} else {
				if (parentIds.contains(trace[i].getId())) {
					testTool.startpoint(trace[i].getTraceId(), null, trace[i].getName(), trace[i].toHashmap().toString());
					endpoints.add(trace[i].getName());
				} else {
					testTool.infopoint(trace[i].getTraceId(), null, trace[i].getName(), trace[i].toHashmap().toString());
				}
			}
		}
		for (int i = endpoints.size() - 1; i >= 0; i--) {
			testTool.endpoint(trace[0].getTraceId(), null, endpoints.get(i), "Endpoint");
		}
	}
}
