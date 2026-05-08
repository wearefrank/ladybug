/*
   Copyright 2026 WeAreFrank!

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

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.opentelemetry.proto.trace.v1.Span;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TraceTree;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.database.DatabaseTracingStorage;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.*;

@Component
public class TracingApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	@Autowired
	private @Setter DatabaseTracingStorage databaseTracingStorage;

	public void storeSpan(Span span) throws SQLException {
		databaseTracingStorage.store(span);
	}

	public ArrayList<Report> getTraceReports() throws SQLException {
		List<Span> spans =  databaseTracingStorage.getAllSpans();

		HashMap<String, ArrayList<Span>> unorderedTraces = sortInTraces(spans);

		ArrayList<TraceTree> traces = new ArrayList<>();

		for (ArrayList<Span> unorderedTrace : unorderedTraces.values()) {
			traces.add(processSpans(unorderedTrace));
		}

		return testTool.getTraceReports(traces);
	}

	public HashMap<String, ArrayList<Span>> sortInTraces(List<Span> spans) {
		HashMap<String, ArrayList<Span>> unorderedTraces = new HashMap<>();

		for (Span span : spans) {
			unorderedTraces.compute(byteStringToHex(span.getTraceId()), (key, existing) -> {
				ArrayList<Span> updated =
						existing == null ? new ArrayList<>() : new ArrayList<>(existing);

				updated.add(span);
				return updated;
			});
		}

		return unorderedTraces;
	}

	public TraceTree processSpans(ArrayList<Span> spans) {
		Span root = findRoot(spans);

		TraceTree traceTree = new TraceTree(root, testTool);
		ArrayList<String> spanIds = new ArrayList<>();

		for (Span span : spans) {
			spanIds.add(byteStringToHex(span.getSpanId()));
		}

		if (root != null) {
			for (Span span: spans) {
				String parentId = byteStringToHex(span.getParentSpanId());

				if (!parentId.isEmpty() && spanIds.contains(parentId)) {
					traceTree.addEdge(parentId, span);
				} else {
					ArrayList<Span> timeSortedSpans = new ArrayList<>(spans);
					timeSortedSpans.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));

					for (int i = 0; i < timeSortedSpans.size(); i++) {
						if (byteStringToHex(span.getSpanId()).equals(byteStringToHex(timeSortedSpans.get(i).getSpanId()))) {
							if (i > 0) {
								traceTree.addEdge(byteStringToHex(timeSortedSpans.get(i - 1).getSpanId()), span);
							}
						}
					}
				}
			}
		}

		return traceTree;
	}

	public String byteStringToHex(ByteString byteString) {
		return Hex.encodeHexString(byteString.toByteArray());
	}

	private Span findRoot(ArrayList<Span> unorderedTrace) {
		ArrayList<String> spanIds = new ArrayList<>();

		for (Span span : unorderedTrace) {
			spanIds.add(byteStringToHex(span.getSpanId()));
		}

		for (Span span : unorderedTrace) {
			String parentId = byteStringToHex(span.getParentSpanId());
			if (parentId.isEmpty()) {
				return span;
			}
		}

		for (Span span : unorderedTrace) {
			String parentId = byteStringToHex(span.getParentSpanId());
			if (!spanIds.contains(parentId)) {
				return span;
			}
		}

		return null;
	}
}