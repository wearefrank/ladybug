package org.wearefrank.ladybug.test.junit.createreport;

import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.*;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;

import static org.junit.Assert.*;

public class TestReportParenting extends ReportRelatedTestCase {
    private TestableReport report;

    private static class TestableReport extends Report {
        public <T> T callCheckpoint(
                String childThreadId,
                String sourceClassName,
                String name,
                T message,
                java.util.Map<String, Object> messageContext,
                StubableCode stubableCode,
                StubableCodeThrowsException stubableCodeThrowsException,
                java.util.Set<String> matchingStubStrategies,
                int checkpointType,
                int levelChangeNextCheckpoint,
                String id,
                String parentId,
                long startTime
        ) {
            return checkpoint(
                    childThreadId,
                    sourceClassName,
                    name,
                    message,
                    messageContext,
                    stubableCode,
                    stubableCodeThrowsException,
                    matchingStubStrategies,
                    checkpointType,
                    levelChangeNextCheckpoint,
                    id,
                    parentId,
                    startTime
            );
        }

        public void initialize() {
            init();
        }
    }

    @Before
    public void setup() {
        super.setUp();

        report = new TestableReport();
        report.setCorrelationId("corr");
        report.setBeingUpdated(true);
        report.setTestTool(testTool);

        report.initialize();
    }

    @Test
    public void testCheckpointStoresIdParentIdAndStartTime() {
        report.callCheckpoint(
                null,
                null,
                "parent",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "id-1",
                "",
                123L
        );

        Checkpoint checkpoint = report.getCheckpoints().get(0);

        assertEquals("id-1", checkpoint.getId());
        assertNull(checkpoint.getParentId());
        assertEquals(123L, checkpoint.getStartTime());
    }

    @Test
    public void testChildCheckpointGetsNestedUnderParent() {
        report.callCheckpoint(
                null,
                null,
                "parent",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "parent-id",
                "",
                100L
        );

        report.callCheckpoint(
                null,
                null,
                "child",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "child-id",
                "parent-id",
                200L
        );

        Checkpoint parent = report.getCheckpoints().get(0);
        Checkpoint child = report.getCheckpoints().get(1);

        assertEquals(0, parent.getLevel());
        assertEquals(1, child.getLevel());

        assertEquals("parent-id", child.getParentId());
    }

    @Test
    public void testOrphanGetsReparentedLater() {
        report.callCheckpoint(
                null,
                null,
                "orphan",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "child-id",
                "parent-id",
                200L
        );

        assertEquals(0, report.getCheckpoints().get(0).getLevel());

        report.callCheckpoint(
                null,
                null,
                "parent",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "parent-id",
                "",
                100L
        );

        Checkpoint parent = report.getCheckpoints().get(0);
        Checkpoint child = report.getCheckpoints().get(1);

        assertEquals("parent", parent.getName());
        assertEquals("orphan", child.getName());

        assertEquals(0, parent.getLevel());
        assertEquals(1, child.getLevel());
    }

    @Test
    public void testChildrenOrderedByStartTime() {
        report.callCheckpoint(
                null,
                null,
                "parent",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "parent-id",
                "",
                100L
        );

        report.callCheckpoint(
                null,
                null,
                "child-2",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "child-2-id",
                "parent-id",
                300L
        );

        report.callCheckpoint(
                null,
                null,
                "child-1",
                "message",
                null,
                null,
                null,
                null,
                CheckpointType.STARTPOINT.toInt(),
                1,
                "child-1-id",
                "parent-id",
                200L
        );

        assertEquals("parent", report.getCheckpoints().get(0).getName());
        assertEquals("child-1", report.getCheckpoints().get(1).getName());
        assertEquals("child-2", report.getCheckpoints().get(2).getName());
    }
}