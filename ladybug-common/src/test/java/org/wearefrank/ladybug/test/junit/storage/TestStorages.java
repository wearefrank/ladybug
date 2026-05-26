package org.wearefrank.ladybug.test.junit.storage;

import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class TestStorages extends ReportRelatedTestCase {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Storage storage;

    @Before
    public void setUp() {
        super.setUp();
        storage = testTool.getDebugStorage();
        assertNotNull(storage);
    }

    @Test
    public void testClearStorage() throws Exception {
        // Workaround because annotation org.junit.jupiter.api.condition.DisabledIfSystemProperty
        // did not work.
        if (System.getProperty("keep.all.ladybug.reports.from.unit.tests") == null) {
            System.out.println("Test not skipped: testClearStorage()");
            createReport();
            createReport();
            assertNotEquals(0, storage.getSize());
            storage.clear();
            assertEquals(0, storage.getSize());
        } else {
            System.out.println("Test skipped: testClearStorage()");
        }
    }

    private void createReport() {
        String correlationId = getCorrelationId();
        testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, "startmessage");
        testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
    }
}
