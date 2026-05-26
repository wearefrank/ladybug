//package org.wearefrank.ladybug.test.junit.createreport;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.wearefrank.ladybug.*;
//
//import java.util.List;
//
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;
//
//public class TestReportParenting {
//
//    private Report report;
//    private TestTool testTool;
//
//    @Before
//    public void setup() {
//        report = new Report();
//        report.setCorrelationId("corr");
//
//        testTool = mock(TestTool.class);
//
//        when(testTool.getMaxCheckpoints()).thenReturn(1000);
//        when(testTool.getMaxMemoryUsage()).thenReturn(Long.MAX_VALUE);
//        when(testTool.getMessageEncoder()).thenReturn(new MessageEncoder());
//
//        report.setTestTool(testTool);
//
//        report.init();
//    }
//
//    @Test
//    public void testCheckpointStoresIdParentIdAndStartTime() {
//        report.checkpoint(
//                null,
//                null,
//                "parent",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "id-1",
//                "",
//                true,
//                123L
//        );
//
//        Checkpoint checkpoint = report.getCheckpoints().get(0);
//
//        assertEquals("id-1", checkpoint.getId());
//        assertNull(checkpoint.getParentId());
//        assertEquals(123L, checkpoint.getStartTime());
//    }
//
//    @Test
//    public void testChildCheckpointGetsNestedUnderParent() {
//        report.checkpoint(
//                null,
//                null,
//                "parent",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "parent-id",
//                "",
//                true,
//                100L
//        );
//
//        report.checkpoint(
//                null,
//                null,
//                "child",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "child-id",
//                "parent-id",
//                true,
//                200L
//        );
//
//        List<Checkpoint> checkpoints = report.getCheckpoints();
//
//        assertEquals(2, checkpoints.size());
//
//        Checkpoint parent = checkpoints.get(0);
//        Checkpoint child = checkpoints.get(1);
//
//        assertEquals(0, parent.getLevel());
//        assertEquals(1, child.getLevel());
//
//        assertEquals("parent-id", child.getParentId());
//    }
//
//    @Test
//    public void testOrphanGetsReparentedLater() {
//        report.checkpoint(
//                null,
//                null,
//                "orphan",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "child-id",
//                "parent-id",
//                true,
//                200L
//        );
//
//        Checkpoint orphan = report.getCheckpoints().get(0);
//
//        assertEquals(0, orphan.getLevel());
//
//        report.checkpoint(
//                null,
//                null,
//                "parent",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "parent-id",
//                "",
//                true,
//                100L
//        );
//
//        List<Checkpoint> checkpoints = report.getCheckpoints();
//
//        Checkpoint parent = checkpoints.get(0);
//        Checkpoint child = checkpoints.get(1);
//
//        assertEquals("parent", parent.getName());
//        assertEquals("orphan", child.getName());
//
//        assertEquals(0, parent.getLevel());
//        assertEquals(1, child.getLevel());
//    }
//
//    @Test
//    public void testChildrenOrderedByStartTime() {
//        report.checkpoint(
//                null,
//                null,
//                "parent",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "parent-id",
//                "",
//                true,
//                100L
//        );
//
//        report.checkpoint(
//                null,
//                null,
//                "child-2",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "child-2-id",
//                "parent-id",
//                true,
//                300L
//        );
//
//        report.checkpoint(
//                null,
//                null,
//                "child-1",
//                "message",
//                null,
//                null,
//                null,
//                null,
//                CheckpointType.STARTPOINT.toInt(),
//                1,
//                "child-1-id",
//                "parent-id",
//                true,
//                200L
//        );
//
//        List<Checkpoint> checkpoints = report.getCheckpoints();
//
//        assertEquals("parent", checkpoints.get(0).getName());
//        assertEquals("child-1", checkpoints.get(1).getName());
//        assertEquals("child-2", checkpoints.get(2).getName());
//    }
//}