/*
   Copyright 2021, 2023-2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit.rerun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import lombok.SneakyThrows;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.Rerunner;
import org.wearefrank.ladybug.SecurityContext;
import org.wearefrank.ladybug.StubType;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
@RunWith(Parameterized.class)
public class TestRerun extends ReportRelatedTestCase {
	public static final String RESOURCE_PATH = "org/wearefrank/ladybug/test/junit/rerun/";
	private static Integer i;

	@Before
	public void setUp() {
		super.setUp();
		i = 0;
	}

	@Test
	public void testRerun() throws StorageException, IOException {
		testTool.setRerunner(new Rerunner() {
			@SneakyThrows
			@Override
			public String rerun(String correlationId, Report originalReport, SecurityContext securityContext,
								ReportRunner reportRunner) {
				originalReport.getCheckpoints().get(0);
				Integer firstMessage = (Integer) originalReport.getCheckpoints().get(0).getMessageAsObject();
				assertEquals((Integer) 10, firstMessage);
				firstMessage = 100;
				addSomething(testTool, correlationId, reportName, firstMessage);
				return null;
			}
		});
		String correlationId = ReportRelatedTestCase.getCorrelationId();
		addSomething(testTool, correlationId, reportName, 10);
		assertEquals((Integer) 10, i);
		Storage storage = testTool.getDebugStorage();
		Report report = ReportRelatedTestCase.findAndGetReport(testTool, storage, correlationId);
		report.setTestTool(testTool);
		report.getCheckpoints().get(1).setStub(StubType.YES.toInt());
		String actual = report.toXml();
		actual = ReportRelatedTestCase.applyToXmlIgnores(actual, report);
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		actual = ReportRelatedTestCase.applyEstimatedMemoryUsageIgnore(actual);
		ReportRelatedTestCase.assertXml(RESOURCE_PATH, reportName, actual);
		int maxStorageIdBeforeRerun = ReportRelatedTestCase.getMaxStorageId(testTool, storage);
		assertNull(testTool.rerun(ReportRelatedTestCase.getCorrelationId(), report, null, null));
		int maxStorageIdAfterRerun = ReportRelatedTestCase.getMaxStorageId(testTool, storage);
		assertEquals(maxStorageIdAfterRerun, maxStorageIdBeforeRerun + 1);
		assertEquals((Integer) 10, i);
	}

	private static void addSomething(TestTool testTool, String correlationId, String name, Integer something) {
		something = testTool.startpoint(correlationId, null, name, something);
		i = i + something;
		i = testTool.endpoint(correlationId, null, name, i);
	}

}
