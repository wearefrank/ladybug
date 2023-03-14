package nl.nn.testtool.metadata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;

public class XpathMetadataFieldExtractorTest {
	@Test
	public void happy() throws Exception {
		Report report = getReport();
		XpathMetadataFieldExtractor instance = new XpathMetadataFieldExtractor();
		instance.setExtractFrom("last");
		instance.setXpath("/one/two");
		assertEquals("My second value", instance.extractMetadata(report));
	}

	@Test
	public void whenXpathFindsNothingThenEmptyString() throws Exception {
		Report report = getReport();
		XpathMetadataFieldExtractor instance = new XpathMetadataFieldExtractor();
		instance.setExtractFrom("last");
		instance.setXpath("/one/three");
		assertEquals("", instance.extractMetadata(report));
	}

	@Test
	public void whenXpathFindsNothingAndDefaultSetThenDefaultReturned() throws Exception {
		Report report = getReport();
		XpathMetadataFieldExtractor instance = new XpathMetadataFieldExtractor();
		instance.setExtractFrom("last");
		instance.setXpath("/one/three");
		instance.setDefaultValue("theDefault");
		assertEquals("theDefault", instance.extractMetadata(report));
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
		checkpoint.setType(Checkpoint.TYPE_STARTPOINT);
		checkpoint = new Checkpoint();
		checkpoints.add(checkpoint);
		checkpoint.setReport(report);
		checkpoint.setName("SessionKey mySessionKey");
		checkpoint.setMessage("<one><two>My second value</two></one>");
		checkpoint.setType(Checkpoint.TYPE_ENDPOINT);
		report.setCheckpoints(checkpoints);
		return report;
	}
}
