package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class TestTracingApiImpl extends ReportRelatedTestCase {
    private TracingApiImpl tracingApiImpl;

    @Before
    public void setUp() {
        super.setUp();

        tracingApiImpl = new TracingApiImpl();
        tracingApiImpl.setTestTool(testTool);
    }

    @Test
    public void testProcessSingleRootSpan() throws StorageException {
        Span root = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("root")
                .build();

        ArrayList<Span> spans = new ArrayList<>();
        spans.add(root);

        tracingApiImpl.processSpans(spans);

        String correlationId = tracingApiImpl.byteStringToHex(root.getTraceId());

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
        assertEquals(2, report.getCheckpoints().size());
    }

    @Test
    public void testProcessParentChildSpans() throws StorageException {
        Span parent = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace2"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("parent")
                .build();

        Span child = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace2"))
                .setSpanId(ByteString.copyFromUtf8("span2"))
                .setParentSpanId(ByteString.copyFromUtf8("span1"))
                .setName("child")
                .build();

        ArrayList<Span> spans = new ArrayList<>();
        spans.add(parent);
        spans.add(child);

        tracingApiImpl.processSpans(spans);

        String correlationId = tracingApiImpl.byteStringToHex(parent.getTraceId());

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
        assertEquals(4, report.getCheckpoints().size());
    }

    @Test
    public void testProcessSpansWithoutParentLinksUsesTimeOrdering() throws StorageException {
        Span parent = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace3"))
                .setSpanId(ByteString.copyFromUtf8("first"))
                .setName("first")
                .setStartTimeUnixNano(100)
                .build();

        Span second = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace3"))
                .setSpanId(ByteString.copyFromUtf8("second"))
                .setParentSpanId(ByteString.copyFromUtf8("missing"))
                .setName("second")
                .setStartTimeUnixNano(200)
                .build();

        Span child = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace3"))
                .setSpanId(ByteString.copyFromUtf8("third"))
                .setParentSpanId(ByteString.copyFromUtf8("second"))
                .setName("third")
                .setStartTimeUnixNano(300)
                .build();

        ArrayList<Span> spans = new ArrayList<>();
        spans.add(parent);
        spans.add(child);
        spans.add(second);

        tracingApiImpl.processSpans(spans);

        String correlationId = tracingApiImpl.byteStringToHex(parent.getTraceId());

        List<Report> reports = findAndGetReports(
                testTool,
                testTool.getDebugStorage(),
                correlationId,
                false
        );

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("first", report.getName());
        assertEquals(6, report.getCheckpoints().size());
    }

    @Test
    public void testProcessSpansWithExternalParent() throws StorageException {
        Span child = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace4"))
                .setSpanId(ByteString.copyFromUtf8("child"))
                .setParentSpanId(ByteString.copyFromUtf8("external"))
                .setName("child")
                .build();

        ArrayList<Span> spans = new ArrayList<>();
        spans.add(child);

        tracingApiImpl.processSpans(spans);

        String correlationId = tracingApiImpl.byteStringToHex(child.getTraceId());

        List<Report> reports = findAndGetReports(
                testTool,
                testTool.getDebugStorage(),
                correlationId,
                false
        );

        assertFalse(reports.isEmpty());

        Report report = reports.get(0);

        assertNotNull(report);
        assertEquals("child", report.getName());
        assertEquals(2, report.getCheckpoints().size());
    }
}