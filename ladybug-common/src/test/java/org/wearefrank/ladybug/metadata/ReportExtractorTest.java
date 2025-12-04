/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.CheckpointType;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;

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
	public void whenCheckpointNameIsValidThenExtractsValue() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void whenSessionKeyIsInNameThenValueIsExtracted() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setCheckpointName("SessionKey mySessionKey");
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("http://www.egem.nl/StuF/sector/zkn/0310/creeerZaak_Lk01", extracted);
	}

	@Test
	public void whenRegexIsSetThenValueIsTransformed() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setCheckpointName("mySessionKey");
		RegexExtractionStrategy regexStrategy = new RegexExtractionStrategy("\\/([^\\/_]*)_");
		extractor.getExtractionStrategies().add(regexStrategy);
		extractor.setSessionKey(true);
		String extracted = (String) extractor.extractMetadata(report);
		assertEquals("creeerZaak", extracted);
	}

	@Test
	public void whenValueNotFoundThenDefaultShouldReturn() {
		ReportExtractor extractor = new ReportExtractor();
		extractor.setDefaultValue("myDefaultValue");
		RegexExtractionStrategy regexStrategy = new RegexExtractionStrategy("(?!x)x");
		extractor.getExtractionStrategies().add(regexStrategy);
		extractor.setSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		Object extracted = extractor.extractMetadata(report);
		assertEquals("myDefaultValue", extracted);
	}

	@Test
	public void whenNoDefaultIsSetThenNullShouldReturn() {
		ReportExtractor extractor = new ReportExtractor();
		RegexExtractionStrategy regexStrategy = new RegexExtractionStrategy("(?!x)x");
		extractor.getExtractionStrategies().add(regexStrategy);
		extractor.setSessionKey(true);
		extractor.setCheckpointName("mySessionKey");
		Object extracted = extractor.extractMetadata(report);
		assertNull(extracted);
	}

	@Test
	public void whenXpathIsSetThenPathShouldBeApplied() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		XpathExtractionStrategy xpathStrategy = new XpathExtractionStrategy("/one/two");
		extractor.getExtractionStrategies().add(xpathStrategy);
		extractor.setSessionKey(true);
		extractor.setCheckpointName("anotherKey");
		assertEquals("My value", extractor.extractMetadata(report));
	}

	@Test
	public void whenRegexAndXpathAreSetThenBothAreApplied() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		XpathExtractionStrategy xpathStrategy = new XpathExtractionStrategy("/one/two");
		extractor.getExtractionStrategies().add(xpathStrategy);
		RegexExtractionStrategy regexStrategy = new RegexExtractionStrategy("([^\\s]+)");
		extractor.getExtractionStrategies().add(regexStrategy);
		extractor.setSessionKey(true);
		extractor.setCheckpointName("anotherKey");
		assertEquals("My", extractor.extractMetadata(report));
	}

	@Test
	public void whenExtractionOrderIsIncorrectThenExtractShouldFail() throws XPathExpressionException {
		Report report = getReport();
		ReportExtractor extractor = new ReportExtractor();
		RegexExtractionStrategy regexStrategy = new RegexExtractionStrategy("([^\\s]+)");
		extractor.getExtractionStrategies().add(regexStrategy);
		XpathExtractionStrategy xpathStrategy = new XpathExtractionStrategy("/one/two");
		extractor.getExtractionStrategies().add(xpathStrategy);
		extractor.setSessionKey(true);
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
