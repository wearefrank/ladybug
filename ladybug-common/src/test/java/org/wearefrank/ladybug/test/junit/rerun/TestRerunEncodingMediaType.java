package org.wearefrank.ladybug.test.junit.rerun;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.*;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class TestRerunEncodingMediaType extends ReportRelatedTestCase {
    private Object fetchedFromStub = null;

    @Before
    public void setUp() {
        super.setUp();
        fetchedFromStub = null;
    }

    class CustomRerunner implements Rerunner {
        @SneakyThrows
        @Override
        public String rerun(String correlationId, Report originalReport, SecurityContext securityContext,
                            ReportRunner reportRunner) {
            testTool.startpoint(correlationId, null, reportName, "Rerun start value");
            // Do not assert right here. An assert here would produce an exception that is
            // swallowed by call stack enclosing this Rerunner.
            fetchedFromStub = testTool.endpoint(correlationId, null, reportName, "Rerun end value");
            return fetchedFromStub.toString();
        }
    }

    @Test
    public void whenMessageContainsMediaTypeThenStubbedValueIsMediaType() throws Exception {
        testTool.setRerunner(new CustomRerunner());
        String correlationId = ReportRelatedTestCase.getCorrelationId();
        testTool.startpoint(correlationId, null, reportName, "Original start value");
        testTool.endpoint(correlationId, null, reportName, new MediaType("application/json"));
        Storage storage = testTool.getDebugStorage();
        Report report = ReportRelatedTestCase.findAndGetReport(testTool, storage, correlationId);
        report.setTestTool(testTool);
        report.getCheckpoints().get(1).setStub(StubType.YES.toInt());
        String rerunCorrelationId = ReportRelatedTestCase.getCorrelationId();
        testTool.rerun(rerunCorrelationId, report, null, null);
        Assert.assertEquals("application/json", fetchedFromStub.toString());
        Assert.assertTrue(fetchedFromStub instanceof MediaType);
    }

    @Test
    public void whenMessageContainsMediaTypeThenReportCanBeStored() throws Exception {
        testTool.setRerunner(new CustomRerunner());
        String correlationId = ReportRelatedTestCase.getCorrelationId();
        testTool.startpoint(correlationId, null, reportName, "Original start value");
        Map<String, Object> messageContext = new HashMap<>();
        messageContext.put("key", new MediaType("application/json"));
        testTool.endpoint(correlationId, null, reportName, "Original end value", messageContext);
    }
}