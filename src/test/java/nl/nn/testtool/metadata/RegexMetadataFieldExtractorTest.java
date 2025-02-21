package nl.nn.testtool.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;

public class RegexMetadataFieldExtractorTest {
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
		checkpoint.setName("emptyCheckpoint");
		checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey mySessionKey");
		checkpoint.setMessage("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01");
		report.setCheckpoints(checkpoints);
	}

	@Test
	public void getCheckpointMessage() {
		RegexMetadataFieldExtractor extractor = new RegexMetadataFieldExtractor();
		extractor.setCheckpointName("SessionKey mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void getCheckpointAndApplyRegex() {
		RegexMetadataFieldExtractor extractor = new RegexMetadataFieldExtractor();
		extractor.setCheckpointName("SessionKey mySessionKey");
		extractor.setRegex("\\/([^\\/_]*)_");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("creeerZaak", extracted);
	}

	@Test
	public void failToMatchRegex() {
		RegexMetadataFieldExtractor extractor = new RegexMetadataFieldExtractor();
		extractor.setDefaultValue("myDefaultValue");
		extractor.setCheckpointName("SessionKey mySessionKey");
		extractor.setRegex("(?!x)x");
		Object extracted = extractor.extractMetadata(report);
		assertEquals("myDefaultValue", extracted);	
	}

	@Test
	public void noRegexMatchWithoutDefaultValue() {
		RegexMetadataFieldExtractor extractor = new RegexMetadataFieldExtractor();
		extractor.setCheckpointName("SessionKey mySessionKey");
		extractor.setRegex("(?!x)x");
		Object extracted = extractor.extractMetadata(report);
        assertNull(extracted);
	}

	@Test
	public void getEmptyCheckpointMessage() {
		RegexMetadataFieldExtractor extractor = new RegexMetadataFieldExtractor();
		extractor.setCheckpointName("emptyCheckpoint");
		String extracted = (String) extractor.extractMetadata(report);
		assertNull(extracted);
	}
}
