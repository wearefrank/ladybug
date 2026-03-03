package org.wearefrank.ladybug.test.junit.storage;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        assertEquals(2, storage.getSize());
        storage.clear();
        assertEquals(0, storage.getSize());
    }

    @Test
    public void testVariablesForCrudStorage() throws Exception {
        System.out.println(String.format("TestStorages.testVariablesForCrudStorage(): storage is of type [%s]", storage.getClass().getName()));
        if (! (storage instanceof CrudStorage)) {
            System.out.println("Skip test testVariablesForCrudStorage() for other types of storage");
            return;
        }
        createReport();
        assertEquals(1, storage.getSize());
        int storageId = storage.getStorageIds().get(0);
        Report report = storage.getReport(storageId);
        Map<String, String> variables = new HashMap<>();
        variables.put("var1", "val1");
        report.setVariables(variables);
        ((CrudStorage) storage).update(report);
        assertEquals(1, storage.getStorageIds().size());
        Report recovered = storage.getReport(storageId);
        assertEquals("val1", report.getVariables().get("var1"));
    }

    private void createReport() {
        String correlationId = getCorrelationId();
        testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, "startmessage");
        testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
    }
}
