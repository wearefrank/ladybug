package org.wearefrank.ladybug.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.CheckpointType;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;

public class StatusMetadataFieldExtractorTest {
	@Test
	public void testSuccessStatus() {
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		assertEquals("Success", instance.extractMetadata(report));
	}

	@Test
	public void testSuccessStatusWithOtherSuccessLabel() {
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setSuccessLabel("OK");
		assertEquals("OK", instance.extractMetadata(report));
	}

	@Test
	public void testErrorStatus() {
		Report report = getReport(CheckpointType.ABORTPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		assertEquals("Error", instance.extractMetadata(report));
	}

	@Test
	public void whenSuccessStatusAndHaveOtherLabelThenStillSuccess() {
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setErrorLabel("Aborted");
		assertEquals("Success", instance.extractMetadata(report));
	}

	@Test
	public void whenErrorStatusAndHaveOtherLabelThenOtherLabelReturned() {
		Report report = getReport(CheckpointType.ABORTPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setErrorLabel("Aborted");
		assertEquals("Aborted", instance.extractMetadata(report));
	}

	@Test
	public void whenOriginalStatusIsSuccessThenStatusFromDelegate() {
		SessionKeyMetadataFieldExtractor delegate = new SessionKeyMetadataFieldExtractor();
		delegate.setSessionKey("anotherKey");
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setDelegate(delegate);
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		assertEquals("Some message", instance.extractMetadata(report));
	}

	@Test
	public void whenOriginalStatusIsErrorThenDelegateIgnored() {
		SessionKeyMetadataFieldExtractor delegate = new SessionKeyMetadataFieldExtractor();
		delegate.setSessionKey("anotherKey");
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setDelegate(delegate);
		Report report = getReport(CheckpointType.ABORTPOINT.toInt());
		assertEquals("Error", instance.extractMetadata(report));
	}

	@Test
	public void whenStatusLengthExceedsMaxLengthThenTruncated() {
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setMaxLength(2);
		assertEquals("Su", instance.extractMetadata(report));
	}

	@Test
	public void whenStatusLengthNotAboveMaxThenNotTruncated() {
		Report report = getReport(CheckpointType.ENDPOINT.toInt());
		StatusMetadataFieldExtractor instance = new StatusMetadataFieldExtractor();
		instance.setMaxLength("Success".length());
		assertEquals("Success", instance.extractMetadata(report));
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
		checkpoint.setType(CheckpointType.STARTPOINT.toInt());
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
