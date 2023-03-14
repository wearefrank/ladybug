package nl.nn.testtool.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;

public class StatusMetadataFieldExtractorTest {
	@Test
	public void testSuccessStatus() {
		Report report = getReport(Checkpoint.TYPE_ENDPOINT);
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		assertEquals("Success", instance.extractMetadata(report));
	}

	@Test
	public void testErrorStatus() {
		Report report = getReport(Checkpoint.TYPE_ABORTPOINT);
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		assertEquals("Error", instance.extractMetadata(report));
	}

	@Test
	public void whenSuccessStatusAndHaveOtherLabelThenStillSuccess() {
		Report report = getReport(Checkpoint.TYPE_ENDPOINT);
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setOtherLabelForError("Aborted");
		assertEquals("Success", instance.extractMetadata(report));
	}

	@Test
	public void whenErrorStatusAndHaveOtherLabelThenOtherLabelReturned() {
		Report report = getReport(Checkpoint.TYPE_ABORTPOINT);
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setOtherLabelForError("Aborted");
		assertEquals("Aborted", instance.extractMetadata(report));
	}

	@Test
	public void whenOriginalStatusIsSuccessThenStatusFromDelegate() {
		SessionKeyMetadataFieldExtractor delegate = new SessionKeyMetadataFieldExtractor();
		delegate.setSessionKey("anotherKey");
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setDelegate(delegate);
		Report report = getReport(Checkpoint.TYPE_ENDPOINT);
		assertEquals("Some message", instance.extractMetadata(report));
	}

	@Test
	public void whenOriginalStatusIsErrorThenDelegateIgnored() {
		SessionKeyMetadataFieldExtractor delegate = new SessionKeyMetadataFieldExtractor();
		delegate.setSessionKey("anotherKey");
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setDelegate(delegate);
		Report report = getReport(Checkpoint.TYPE_ABORTPOINT);
		assertEquals("Error", instance.extractMetadata(report));		
	}

	private Report getReport(int typeOfLastCheckpoint) {
		TestTool testTool = new TestTool();
		Report report = new Report();
		List<Checkpoint> checkpoints = new ArrayList<>();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey anotherKey");
		checkpoint.setMessage("Some message");
		checkpoint.setType(Checkpoint.TYPE_STARTPOINT);
		checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey mySessionKey");
		checkpoint.setMessage("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01");
		checkpoint.setType(typeOfLastCheckpoint);
		report.setCheckpoints(checkpoints);
		return report;
	}
}
