/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.testtool.test.junit.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.memory.Storage;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.Import;
import nl.nn.testtool.util.ImportResult;

/**
 * @author Jaco de Groot
 */
public class TestImport extends TestCase {

	public void testImport() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, StorageException {
		Report report = new Report();
		report.setStorageId(123);
		report.setCorrelationId("456");
		report.setName("Import test");
		report.setDescription("Import test description");
		report.setPath("Import test path");
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		checkpoints.add(new Checkpoint(report, "threadName1", "sourceClassName1", "name1", 1, 2));
		checkpoints.add(new Checkpoint(report, "threadName2", "sourceClassName2", "name1", 3, 4));
		report.setCheckpoints(checkpoints);
		assertImport(TestExport.RESOURCE_PATH, getName(), report);
	}

	public static void assertImport(String path, String testCaseName, Report report) throws IOException, StorageException {
		byte[] bytes = Export.getReportBytes(report);
		InputStream inputStream = new ByteArrayInputStream(bytes);
		Storage storage = new Storage();
		ImportResult result = Import.importTtr(inputStream, storage, null);
		assertNull(result.getErrorMessage());
		report = storage.getReport(result.getNewStorageId());
		ReportXmlTransformer reportXmlTransformer = new ReportXmlTransformer();
		reportXmlTransformer.setXslt(ReportRelatedTestCase.getResource(ReportRelatedTestCase.RESOURCE_PATH, ReportRelatedTestCase.ASSERT_REPORT_XSLT, null));
		report.setReportXmlTransformer(reportXmlTransformer);
		String actual = report.toXml();
		ReportRelatedTestCase.assertXml(path, testCaseName, actual);
	}

}
