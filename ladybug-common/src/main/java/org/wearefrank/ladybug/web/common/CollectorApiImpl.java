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
import org.wearefrank.ladybug.TraceTree;
import org.wearefrank.ladybug.TestTool;

import java.lang.invoke.MethodHandles;
import java.util.*;

@Component
public class CollectorApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(ArrayList<Span> spans) {
		TraceTree traceTree = new TraceTree(testTool);
		ArrayList<String> spanIds = new ArrayList<>();

		for (Span span : spans) {
			spanIds.add(byteStringToHex(span.getSpanId()));
		}

		Span rootSpan = findRoot(spans);

		if (rootSpan != null) {
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
			traceTree.dfs(rootSpan);
		}
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