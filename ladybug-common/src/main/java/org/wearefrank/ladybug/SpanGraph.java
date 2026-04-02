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
import io.opentelemetry.proto.trace.v1.Span;
import org.apache.commons.codec.binary.Hex;

import java.util.*;

public class SpanGraph {
    private Map<String, List<Span>> spans;
    TestTool testTool;

    public SpanGraph(TestTool testTool) {
        spans = new HashMap<>();
        this.testTool = testTool;
    }

    public void addEdge(String parent, Span child) {
        spans.putIfAbsent(parent, new ArrayList<>());
        spans.get(parent).add(child);
    }

    public void dfs(Span root) {
        Set<Span> visited = new HashSet<>();
        dfsRecursive(root, visited);
    }

    private void dfsRecursive(Span node, Set<Span> visited) {
        testTool.startpoint(byteStringToHex(node.getTraceId()), null, node.getName(), toHashMap(node).toString());
        visited.add(node);

        if (spans.containsKey(byteStringToHex(node.getSpanId()))) {
            for (Span neighbor : spans.get(byteStringToHex(node.getSpanId()))) {
                if (!visited.contains(neighbor)) {
                    dfsRecursive(neighbor, visited);
                }
            }
        }

        testTool.endpoint(byteStringToHex(node.getTraceId()), null, node.getName(), "Endpoint");
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