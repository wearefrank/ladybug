package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.TraceTree;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestTraceTree {
    private TestTool testTool;
    private TraceTree traceTree;

    @BeforeEach
    void setUp() {
        testTool = Mockito.mock(TestTool.class);
        traceTree = new TraceTree(testTool);
    }

    @Test
    void testDfsOnlyRoot() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("root")
                .addAttributes(KeyValue.newBuilder()
                        .setKey("http.method")
                        .setValue(AnyValue.newBuilder().setStringValue("GET").build())
                        .build())
                .build();

        traceTree.dfs(span);

        verify(testTool).startpoint(anyString(), isNull(), eq("root"), any());
        verify(testTool).infopoint(anyString(), isNull(), eq("http.method"), anyString());
        verify(testTool).endpoint(anyString(), isNull(), eq("root"), eq("Endpoint"));
    }

    @Test
    void testDfsWithChildSpan() {
        Span parent = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("parent"))
                .setName("parent")
                .build();

        Span child = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("child"))
                .setParentSpanId(ByteString.copyFromUtf8("parent"))
                .setName("child")
                .build();

        traceTree.addEdge(traceTree.byteStringToHex(parent.getSpanId()), child);

        traceTree.dfs(parent);

        verify(testTool).startpoint(anyString(), isNull(), eq("parent"), any());
        verify(testTool).startpoint(anyString(), isNull(), eq("child"), any());
        verify(testTool).endpoint(anyString(), isNull(), eq("child"), eq("Endpoint"));
        verify(testTool).endpoint(anyString(), isNull(), eq("parent"), eq("Endpoint"));
    }

    @Test
    void testByteStringToHex() {
        ByteString bytes = ByteString.copyFromUtf8("abc");
        String hex = traceTree.byteStringToHex(bytes);

        assert hex != null;
        assert !hex.isEmpty();
        assert hex.equals("616263");
    }

    @Test
    void testToHashMap() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("testSpan")
                .build();

        var map = traceTree.toHashMap(span);

        assert map.containsKey("trace_id");
        assert map.containsKey("span_id");
        assert map.containsKey("name");
    }
}