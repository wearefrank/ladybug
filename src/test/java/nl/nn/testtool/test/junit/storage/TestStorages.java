package nl.nn.testtool.test.junit.storage;

import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;

import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestStorages extends ReportRelatedTestCase {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Storage storage;

    @Before
    @Override
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

        Awaitility.await()
            .atMost(1500, TimeUnit.MILLISECONDS)
            .until(testTool::getNumberOfReportsInProgress, IsEqual.equalTo(0L));

        Awaitility.await()
            .atMost(1500, TimeUnit.MILLISECONDS)
            .until(storage::getSize, IsEqual.equalTo(2));

        storage.clear();

        Awaitility.await()
            .atMost(1500, TimeUnit.MILLISECONDS)
            .until(storage::getSize, IsEqual.equalTo(0));
    }

    private void createReport() {
        String correlationId = getCorrelationId();
        testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, "startmessage");
        testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
    }
}
