package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestTracingApiImpl {

    private TracingApiImpl tracingApi;
    private TestTool testTool;

    @Before
    public void setUp() {
        tracingApi = new TracingApiImpl();

        testTool = mock(TestTool.class);
        tracingApi.setTestTool(testTool);
    }

    @Test
    public void testByteStringToHex() {
        ByteString byteString = ByteString.copyFromUtf8("test");

        String result = tracingApi.byteStringToHex(byteString);

        assertEquals("74657374", result);
    }

    @Test
    public void testToHashMap() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace"))
                .setSpanId(ByteString.copyFromUtf8("span"))
                .setParentSpanId(ByteString.copyFromUtf8("parent"))
                .setName("test-span")
                .setStartTimeUnixNano(12345L)
                .addAttributes(
                        KeyValue.newBuilder()
                                .setKey("http.method")
                                .setValue(
                                        AnyValue.newBuilder()
                                                .setStringValue("GET")
                                                .build()
                                )
                                .build()
                )
                .build();

        HashMap<String, String> map = tracingApi.toHashMap(span);

        assertNotNull(map);

        assertEquals("test-span", map.get("name"));
        assertEquals("12345", map.get("start_time_unix_nano"));

        assertTrue(map.containsKey("trace_id"));
        assertTrue(map.containsKey("span_id"));
        assertTrue(map.containsKey("parent_span_id"));
    }

    @Test
    public void testProcessSpans() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setParentSpanId(ByteString.copyFromUtf8("parent1"))
                .setName("test-span")
                .setStartTimeUnixNano(12345L)
                .build();

        tracingApi.processSpans(List.of(span));

        verify(testTool, times(1))
                .startpoint(
                        anyString(),
                        isNull(),
                        eq("test-span"),
                        anyMap(),
                        anyString(),
                        anyString(),
                        eq(12345L)
                );

        verify(testTool, times(1))
                .endpoint(
                        anyString(),
                        isNull(),
                        eq("test-span"),
                        isNull(),
                        anyString(),
                        anyString(),
                        eq(12345L)
                );

        verify(testTool, times(1))
                .close(anyString());
    }

    @Test
    public void testProcessSpansWithoutParent() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("root-span")
                .setStartTimeUnixNano(999L)
                .build();

        tracingApi.processSpans(List.of(span));

        verify(testTool).startpoint(
                anyString(),
                isNull(),
                eq("root-span"),
                anyMap(),
                anyString(),
                eq(""),
                eq(999L)
        );
    }
}