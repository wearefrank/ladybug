package nl.nn.testtool.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import nl.nn.testtool.CheckpointType;
import org.junit.Before;
import org.junit.Test;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;

import javax.xml.xpath.XPathExpressionException;

public class ReportExtractorTest {
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
		ReportExtractor extractor = new ReportExtractor();
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void getSessionKeyAsCheckpoint() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setCheckpointName("SessionKey mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void applyRegexToCheckpointMessage() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		extractor.setRegexPattern("\\/([^\\/_]*)_");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("creeerZaak", extracted);
	}

	@Test
	public void checkDefaultValue() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setDefaultValue("myDefaultValue");
		extractor.setRegexPattern("(?!x)x");
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		Object extracted = extractor.extractMetadata(report);
		assertEquals("myDefaultValue", extracted);	
	}

	@Test
	public void noRegexMatchWithoutDefaultValue() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setRegexPattern("(?!x)x");
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		Object extracted = extractor.extractMetadata(report);
		assertNull(extracted);
	}

	@Test
	public void xpathExtraction() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		extractor.setXpathExpression("/one/two");
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("anotherKey");
		assertEquals("My value", extractor.extractMetadata(report));
	}

	@Test
	public void xpathAndRegex() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		extractor.setXpathExpression("/one/two");
		extractor.setRegexPattern("([^\\s]+)");
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("anotherKey");
		assertEquals("My", extractor.extractMetadata(report));
	}

	@Test
	public void xpathAndRegexWrongOrder() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		extractor.setRegexPattern("([^\\s]+)");
		extractor.setXpathExpression("/one/two");
		extractor.setIsSessionKey(true);
		extractor.setCheckpointName("anotherKey");
		assertNull(extractor.extractMetadata(report));
	}

	private Report getReport() {
		TestTool testTool = new TestTool();
		Report report = new Report();
		List<Checkpoint> checkpoints = new ArrayList<>();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey anotherKey");
		checkpoint.setMessage("<one><two>My value</two></one>");
		checkpoint.setType(CheckpointType.STARTPOINT.toInt());
		checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey mySessionKey");
		checkpoint.setMessage("<one><two>My second value</two></one>");
		checkpoint.setType(CheckpointType.ENDPOINT.toInt());
		report.setCheckpoints(checkpoints);
		return report;
	}
}
