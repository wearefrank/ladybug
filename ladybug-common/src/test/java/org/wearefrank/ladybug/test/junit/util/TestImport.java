/*
   Copyright 2021, 2024-2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit.util;

import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.memory.MemoryCrudStorage;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.wearefrank.ladybug.util.Import;
import org.wearefrank.ladybug.util.ImportResult;

/**
 * @author Jaco de Groot
 */
public class TestImport {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Test
	public void testImport() throws StorageException, IOException {
		assertImport(TestExport.RESOURCE_PATH, "test");
	}

	public static void assertImport(String resourcePath, String testCaseName) throws StorageException, IOException {
		String string = ReportRelatedTestCase.getResource(resourcePath, testCaseName + "Export-expected.xml", false);
		string = string
				.replace("IGNORE-STORAGE-ID", "0")
				.replace("IGNORE-START-TIME", "1")
				.replace("IGNORE-END-TIME", "2");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
		gzipOutputStream.write(string.getBytes(ReportRelatedTestCase.DEFAULT_CHARSET));
		gzipOutputStream.close();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		MemoryCrudStorage storage = new MemoryCrudStorage();
		ImportResult result = Import.importTtr(inputStream, storage, log);
		assertNull(result.getErrorMessage());
		Report report = storage.getReport(result.getNewStorageId());
		ReportRelatedTestCase.assertReport(report, resourcePath, testCaseName + "Import");
	}

}
