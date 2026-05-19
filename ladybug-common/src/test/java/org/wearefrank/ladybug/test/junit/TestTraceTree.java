package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;

import org.junit.Before;
import org.junit.Test;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TraceTree;

import java.util.List;

import static org.junit.Assert.*;

public class TestTraceTree extends ReportRelatedTestCase {

    private TraceTree traceTree;

    @Before
    public void setUp() {
        super.setUp();
        traceTree = new TraceTree(testTool);
    }

    @Test
    public void testDfsOnlyRoot() throws Exception {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("root")
                .addAttributes(KeyValue.newBuilder()
                        .setKey("http.method")
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("GET")
                                .build())
                        .build())
                .build();

        traceTree.dfs(span);

        String correlationId = traceTree.byteStringToHex(span.getTraceId());

        List<Report> reports = findAndGetReports(
                testTool,
                testTool.getDebugStorage(),
                correlationId,
                false
        );

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("root", report.getName());
        assertEquals(3, report.getCheckpoints().size());
    }

    @Test
    public void testDfsWithChildSpan() throws Exception {
        Span parent = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace2"))
                .setSpanId(ByteString.copyFromUtf8("parent"))
                .setName("parent")
                .build();

        Span child = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace2"))
                .setSpanId(ByteString.copyFromUtf8("child"))
                .setParentSpanId(ByteString.copyFromUtf8("parent"))
                .setName("child")
                .build();

        traceTree.addEdge(
                traceTree.byteStringToHex(parent.getSpanId()),
                child
        );

        traceTree.dfs(parent);

        String correlationId = traceTree.byteStringToHex(parent.getTraceId());

        List<Report> reports = findAndGetReports(
                testTool,
                testTool.getDebugStorage(),
                correlationId,
                false
        );

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("parent", report.getName());
        assertTrue(report.getNumberOfCheckpoints() >= 4);
    }

    @Test
    public void testByteStringToHex() {
        ByteString bytes = ByteString.copyFromUtf8("abc");

        String hex = traceTree.byteStringToHex(bytes);

        assertNotNull(hex);
        assertFalse(hex.isEmpty());
        assertEquals("616263", hex);
    }

    @Test
    public void testToHashMap() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("testSpan")
                .build();

        var map = traceTree.toHashMap(span);

        assertTrue(map.containsKey("trace_id"));
        assertTrue(map.containsKey("span_id"));
        assertTrue(map.containsKey("name"));
    }
}