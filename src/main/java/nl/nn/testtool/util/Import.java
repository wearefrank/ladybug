/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.testtool.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.xmldecoder.XMLDecoder;

public class Import {

	public static String importZip(InputStream inputStream, CrudStorage storage, Logger log) {
		String errorMessage = null;
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		ZipEntry zipEntry = null;
		List<ImportResult> importResults = new ArrayList<ImportResult>();
		try {
			zipEntry = zipInputStream.getNextEntry();
			while (zipEntry != null && errorMessage == null) {
				log.debug("Process zip entry: " + zipEntry.getName());
				if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".ttr")) {
					
					// GZIPInputStream and/or XMLDecoder seem to close the
					// ZipInputStream after reading the first ZipEntry.
					// Using an intermediate ByteArrayInputStream to prevent
					// this.
					byte[] reportBytes = new byte[0];
					byte[] buffer = new byte[1000];
					int length = zipInputStream.read(buffer);
					while (length != -1) {
						byte[] tmpBytes = new byte[reportBytes.length + length];
						System.arraycopy(reportBytes, 0, tmpBytes, 0, reportBytes.length);
						System.arraycopy(buffer, 0, tmpBytes, reportBytes.length, length);
						reportBytes = tmpBytes;
						length = zipInputStream.read(buffer);
					}
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(reportBytes);
					
					ImportResult importResult = importTtr(byteArrayInputStream, storage, log);
					importResults.add(importResult);
					errorMessage = importResult.errorMessage;
				}
				zipInputStream.closeEntry();
				zipEntry = zipInputStream.getNextEntry();
			}
			
			for(ImportResult importResult : importResults) {
				Report report = storage.getReport(importResult.newStorageId);
				if(report.getInputCheckpoint().containsVariables()) {
					if(report.getInputCheckpoint().updateVariables(importResults)) {
						storage.update(report);
						log.debug("Updated report ["+report.getFullPath()+"]'s input variable(s) with target report's updated storageId(s)");
					} else {
						log.warn("Could not update report ["+report.getFullPath()+"]'s input variables on uploading - please review manually");
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return errorMessage;
	}

	// TODO closeXMLEncoder methode (en anderen?) gebruiken?
	public static ImportResult importTtr(InputStream inputStream, CrudStorage storage, Logger log) {
		String errorMessage = null;
		GZIPInputStream gzipInputStream = null;
		XMLDecoder xmlDecoder = null;
		String version = null;
		Report report = null;
		ImportResult importResult = new ImportResult();
		try {
			gzipInputStream = new GZIPInputStream(inputStream);
			xmlDecoder = new XMLDecoder(gzipInputStream);
			version = (String)xmlDecoder.readObject();
			log.debug("Decoded version: " + version);
			report = (Report)xmlDecoder.readObject();
			importResult.oldStorageId = report.getStorageId();
			// Check for more than one report in the stream to be backwards
			// compatible with older Test Tool versions that wrote more than one
			// report to a ttr. See comment at the code using the XMLEncoder.
			while (report != null) {
				storage.store(report);
				report = (Report)xmlDecoder.readObject();
				log.debug("Decoded report: " + report.getName());
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			// This happens to be the way in which it ends for the
			// XML Object Decoder
			log.debug("Last report in file read");
		} catch (Throwable t) {
			importResult.errorMessage = "Caught unexpected throwable during import: " + t.getMessage();
			log.error(errorMessage, t);
		} finally {
			importResult.newStorageId = report.getStorageId();
			if (xmlDecoder != null) {
				xmlDecoder.close();
			}
			if (gzipInputStream != null) {
				try {
					gzipInputStream.close();
				} catch(IOException e) {
					log.error("IOException closing gzipInputStream", e);
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch(IOException e) {
					log.error("IOException closing inputStream", e);
				}
			}
		}
		return importResult;
	}
}