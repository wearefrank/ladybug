package nl.nn.testtool.test.junit.storage;

import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestStorages extends ReportRelatedTestCase {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Storage storage;

    @Before
    public void setUp() {
        super.setUp();
        storage = testTool.getDebugStorage();
        assertNotNull(storage);
        try {
            storage.clear();
            assertEquals(0, storage.getSize());
        } catch(Exception e) {
            log.error("Exception while clearing storage: ", e);
        }
    }

    @Test
    public void testClearStorage() throws Exception {
        createReport();
        createReport();
        assertEquals(0, testTool.getNumberOfReportsInProgress());
        assertEquals(2, storage.getSize());
        storage.clear();
        assertEquals(0, storage.getSize());
    }

    private void createReport() {
        String correlationId = getCorrelationId();
        testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, "startmessage");
        testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
    }
}
