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
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testDfsOnlyRoot() {
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

        TraceTree traceTree = new TraceTree(span, testTool);

        traceTree.dfs();

        List<Report> reports = testTool.getTransientReports();

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("root", report.getName());
        assertEquals(3, report.getCheckpoints().size());
    }

    @Test
    public void testDfsWithChildSpan() {
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

        TraceTree traceTree = new TraceTree(parent, testTool);

        traceTree.addEdge(
                traceTree.byteStringToHex(parent.getSpanId()),
                child
        );

        traceTree.dfs();

        List<Report> reports = testTool.getTransientReports();

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("parent", report.getName());
        assertTrue(report.getNumberOfCheckpoints() >= 4);
    }

    @Test
    public void testToHashMap() {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("testSpan")
                .build();

        TraceTree traceTree = new TraceTree(span, testTool);

        var map = traceTree.toHashMap(span);

        assertTrue(map.containsKey("trace_id"));
        assertTrue(map.containsKey("span_id"));
        assertTrue(map.containsKey("name"));
    }
}