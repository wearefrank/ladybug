package org.wearefrank.ladybug.test;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.TraceTree;
import org.wearefrank.ladybug.storage.database.DatabaseTracingStorage;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestTracingApiImpl {
    private TracingApiImpl tracingApiImpl;

    @Before
    public void setUp() {
        tracingApiImpl = new TracingApiImpl();
    }

    @Test
    public void testSortInTraces() {
        Span span1 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .build();

        Span span2 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .build();

        Span span3 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace2"))
                .build();

        HashMap<String, ArrayList<Span>> result =
                tracingApiImpl.sortInTraces(List.of(span1, span2, span3));

        assertEquals(2, result.size());

        String trace1Id = tracingApiImpl.byteStringToHex(span1.getTraceId());
        String trace2Id = tracingApiImpl.byteStringToHex(span3.getTraceId());

        assertEquals(2, result.get(trace1Id).size());
        assertEquals(1, result.get(trace2Id).size());
    }

    @Test
    public void testDeleteTrace() {
        DatabaseTracingStorage storage =
                mock(DatabaseTracingStorage.class);

        tracingApiImpl.setDatabaseTracingStorage(storage);

        List<String> traceIds = List.of("trace1", "trace2");

        tracingApiImpl.deleteTrace(traceIds);

        verify(storage).dropSpansByTraceId(traceIds);
    }

    @Test
    public void testDeleteAllTraces() {
        DatabaseTracingStorage storage =
                mock(DatabaseTracingStorage.class);

        tracingApiImpl.setDatabaseTracingStorage(storage);

        tracingApiImpl.deleteAllTraces();

        verify(storage).dropAllSpans();
    }

    @Test
    public void testProcessSpansCreatesTree() {
        Span root = Span.newBuilder()
                .setSpanId(ByteString.copyFromUtf8("root"))
                .build();

        Span child = Span.newBuilder()
                .setSpanId(ByteString.copyFromUtf8("child"))
                .setParentSpanId(ByteString.copyFromUtf8("root"))
                .build();

        TraceTree tree =
                tracingApiImpl.processSpans(new ArrayList<>(List.of(root, child)));

        assertNotNull(tree.getRoot());
    }

    @Test
    public void testProcessSpansWithOrphanSpan() {
        Span span1 = Span.newBuilder()
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setStartTimeUnixNano(100)
                .build();

        Span span2 = Span.newBuilder()
                .setSpanId(ByteString.copyFromUtf8("span2"))
                .setParentSpanId(ByteString.copyFromUtf8("unknown"))
                .setStartTimeUnixNano(200)
                .build();

        TraceTree tree =
                tracingApiImpl.processSpans(new ArrayList<>(List.of(span1, span2)));

        assertNotNull(tree);
        assertNotNull(tree.getRoot());
    }
}
