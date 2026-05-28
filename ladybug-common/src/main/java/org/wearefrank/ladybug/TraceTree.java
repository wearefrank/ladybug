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
package org.wearefrank.ladybug;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

import java.util.*;

public class TraceTree {
    @Getter
    private HashMap<String, ArrayList<Span>> spans;

    @Getter
    private Span root;

    @Getter
    private String traceId;

    TestTool testTool;

    public TraceTree(Span root, TestTool testTool) {
        this.spans = new HashMap<>();
        this.root = root;
        this.traceId = byteStringToHex(root.getTraceId());
        this.testTool = testTool;
    }

    public void addEdge(String parent, Span child) {
        spans.putIfAbsent(parent, new ArrayList<>());
        spans.get(parent).add(child);
    }

    public void dfs() {
        HashSet<Span> visited = new HashSet<>();
        dfsRecursive(this.root, visited);
    }

    private void dfsRecursive(Span span, HashSet<Span> visited) {
        testTool.startpoint(byteStringToHex(span.getTraceId()), null, span.getName(), toHashMap(span).toString(), false);

        for (KeyValue keyValue : span.getAttributesList()) {
            AnyValue anyValue = keyValue.getValue();
            String value = String.valueOf(anyValue.getField(anyValue.getDescriptorForType().findFieldByNumber(anyValue.getValueCase().getNumber())));
            testTool.infopoint(byteStringToHex(span.getTraceId()), null, keyValue.getKey(), value);
        }

        visited.add(span);

        if (spans.containsKey(byteStringToHex(span.getSpanId()))) {
            for (Span sibling : spans.get(byteStringToHex(span.getSpanId()))) {
                if (!visited.contains(sibling)) {
                    dfsRecursive(sibling, visited);
                }
            }
        }

        testTool.endpoint(byteStringToHex(span.getTraceId()), null, span.getName(), "Endpoint");
    }

    public String byteStringToHex(ByteString byteString) {
        return Hex.encodeHexString(byteString.toByteArray());
    }

    public HashMap<String, String> toHashMap(Span span) {
        HashMap<String, String> map = new HashMap<>();

        span.getAllFields().forEach((descriptor, value) -> {
            if (value instanceof ByteString) {
                map.put(descriptor.getName(), byteStringToHex((ByteString) value));
            } else {
                map.put(descriptor.getName(), value.toString());
            }
        });

        return map;
    }
}