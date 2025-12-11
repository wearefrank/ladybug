/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug.web.common;

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
