package nl.nn.testtool.metadata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;

public class SessionKeyMetadataFieldExtractorTest {
	private Report report;

	@Before
	public void setUp() {
		TestTool testTool = new TestTool();
		report = new Report();
		List<Checkpoint> checkpoints = new ArrayList<>();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey anotherKey");
		checkpoint.setMessage("Some message");
		checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey mySessionKey");
		checkpoint.setMessage("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01");
		report.setCheckpoints(checkpoints);
	}

	@Test
	public void getSessionKey() {
		SessionKeyMetadataFieldExtractor extractor = new SessionKeyMetadataFieldExtractor();
		extractor.setSessionKey("mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void getSessionKeyAndApplyRegex() {
		SessionKeyMetadataFieldExtractor extractor = new SessionKeyMetadataFieldExtractor();
		extractor.setSessionKey("mySessionKey");
		extractor.setRegex("\\/([^\\/_]*)_");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("creeerZaak", extracted);
	}
}
