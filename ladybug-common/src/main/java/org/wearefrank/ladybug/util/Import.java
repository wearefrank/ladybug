/*
   Copyright 2020-2022, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.xmldecoder.XMLDecoder;

public class Import {

	public static String importZip(InputStream inputStream, CrudStorage storage, Logger log) {
		String errorMessage = null;
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		ZipEntry zipEntry = null;
		List<ImportResult> importResults = new ArrayList<ImportResult>();
		try {
			int counter = 0;
			zipEntry = zipInputStream.getNextEntry();
			while (zipEntry != null && errorMessage == null) {
				log.debug("Process zip entry: " + zipEntry.getName());
				if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".ttr")) {
					counter++;
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
			if (counter < 1) {
				errorMessage = "No ttr files found in zip";
			}
			for(ImportResult importResult : importResults) {
				if (importResult.newStorageId != null) {
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
			}
		} catch (IOException e) {
			errorMessage = "IOException on zip import";
			log.warn(errorMessage, e);
			errorMessage = errorMessage + ": " + e.getMessage();
		} catch (StorageException e) {
			errorMessage = "StorageException on zip import";
			log.warn(errorMessage, e);
			errorMessage = errorMessage + ": " + e.getMessage();
		}
		return errorMessage;
	}

	public static ImportResult importTtr(InputStream inputStream, CrudStorage storage, Logger log) {
		GZIPInputStream gzipInputStream = null;
		XMLDecoder xmlDecoder = null;
		String version = null;
		Report report = null;
		ImportResult importResult = new ImportResult();
		try {
			gzipInputStream = new GZIPInputStream(inputStream);
			xmlDecoder = new XMLDecoder(gzipInputStream);
			version = (String)xmlDecoder.readObject();
			if (log != null) log.debug("Decoded version: " + version);
			report = (Report)xmlDecoder.readObject();
			importResult.oldStorageId = report.getStorageId();
			// Check for more than one report in the stream to be backwards
			// compatible with older Test Tool versions that wrote more than one
			// report to a ttr. See comment at the code using the XMLEncoder.
			while (report != null) {
				storage.store(report);
				report = (Report)xmlDecoder.readObject();
				if (log != null) log.debug("Decoded report: " + report.getName());
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			// This happens to be the way in which it ends for the
			// XML Object Decoder
			if (log != null) log.debug("Last report in file read");
		} catch (Throwable t) {
			importResult.errorMessage = "Caught unexpected throwable during import: " + t.getClass().getTypeName()
					+ ": " + t.getMessage();
			if (log != null) log.error(importResult.errorMessage, t);
		} finally {
			if (report != null) {
				importResult.newStorageId = report.getStorageId();
			}
			if (xmlDecoder != null) {
				xmlDecoder.close();
			}
			if (gzipInputStream != null) {
				closeInputStream(gzipInputStream, "closing gzip input stream after ttr import", log);
			}
			if (inputStream != null) {
				closeInputStream(gzipInputStream, "closing input stream after ttr import", log);
			}
		}
		return importResult;
	}

	public static Report getReport(InputStream inputStream, Integer storageId, Long storageSize, Logger log) throws StorageException {
		Report report = null;
		GZIPInputStream gzipInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			gzipInputStream = new GZIPInputStream(inputStream);
			objectInputStream = new ObjectInputStream(gzipInputStream);
			report = (Report)objectInputStream.readObject();
			report.setStorageId(storageId);
			report.setStorageSize(storageSize);
		} catch(IOException e) {
			logAndThrow(log, e, "IOException reading report " + storageId + " from bytes");
		} catch(ClassNotFoundException e) {
			logAndThrow(log, e, "ClassNotFoundException reading report " + storageId + " from file");
		} finally {
			if (objectInputStream != null) {
				closeInputStream(objectInputStream, "closing object input stream after reading report " + storageId + " from file", log);
			}
			if (gzipInputStream != null) {
				closeInputStream(gzipInputStream, "closing gzip input stream after reading report " + storageId + " from file", log);
			}
		}
		return report;
	}

	public static void closeInputStream(InputStream inputStream, String action, Logger log) {
		try {
			inputStream.close();
		} catch(IOException e) {
			if (log != null) log.warn("IOException " + action, e);
		}
	}

	public static void closeReader(java.io.Reader reader, String action, Logger log) {
		try {
			reader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}

	public static void closeCSVReader(CSVReader csvReader, String action, Logger log) {
		try {
			csvReader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}

	public static void logAndThrow(Logger log, String message) throws StorageException {
		log.error(message);
		throw new StorageException(message);
	}

	public static void logAndThrow(Logger log, Exception e, String message) throws StorageException {
		message = message + ": " + e.getMessage();
		log.error(message, e);
		throw new StorageException(message, e);
	}

}