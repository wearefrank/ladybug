/*
   Copyright 2021, 2023 WeAreFrank!

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
package nl.nn.testtool.test.junit.rerun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import lombok.SneakyThrows;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.Rerunner;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.Common;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
@RunWith(Parameterized.class)
public class TestRerun {
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"File storage", (TestTool) Common.CONTEXT_FILE_STORAGE.getBean("testTool")},
			{"Memory storage", (TestTool) Common.CONTEXT_MEM_STORAGE.getBean("testTool")},
			{"Database storage", (TestTool) Common.CONTEXT_DB_STORAGE.getBean("testTool")}
		});
	}

	@Parameter(0)
	public String storageDescription;

	@Parameter(1)
	public TestTool testTool;

	private String reportName;

	public static final String RESOURCE_PATH = "nl/nn/testtool/test/junit/rerun/";
	private static Integer i;

	@Rule
	public TestName name = new TestName();

	@Before
	public void setUp() {
		reportName = Common.methodNameWithoutTestParameter(name.getMethodName());
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
				Integer firstMessage = (Integer)originalReport.getCheckpoints().get(0).getMessageAsObject();
				assertEquals((Integer)10, firstMessage);
				firstMessage = 100;
				addSomething(testTool, correlationId, reportName, firstMessage);
				return null;
			}
		});
		String correlationId = ReportRelatedTestCase.getCorrelationId();
		addSomething(testTool, correlationId, reportName, 10);
		assertEquals((Integer)10, i);
		Storage storage = testTool.getDebugStorage();
		Report report = ReportRelatedTestCase.findAndGetReport(testTool, storage, correlationId);
		report.setTestTool(testTool);
		report.getCheckpoints().get(1).setStub(Checkpoint.STUB_YES);
		String actual = report.toXml();
		actual = ReportRelatedTestCase.applyToXmlIgnores(actual, report);
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		actual = ReportRelatedTestCase.applyEstimatedMemoryUsageIgnore(actual);
		ReportRelatedTestCase.assertXml(RESOURCE_PATH, reportName, actual);
		int maxStorageIdBeforeRerun = ReportRelatedTestCase.getMaxStorageId(testTool, storage);
		assertNull(testTool.rerun(ReportRelatedTestCase.getCorrelationId(), report, null, null));
		int maxStorageIdAfterRerun = ReportRelatedTestCase.getMaxStorageId(testTool, storage);
		assertEquals(maxStorageIdAfterRerun, maxStorageIdBeforeRerun + 1);
		assertEquals((Integer)10, i);
	}

	private static void addSomething(TestTool testTool, String correlationId, String name, Integer something) {
		something = testTool.startpoint(correlationId, null, name, something);
		i = i + something;
		i = testTool.endpoint(correlationId, null, name, i);
	}

}
