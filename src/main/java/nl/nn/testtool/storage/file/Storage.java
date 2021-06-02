/*
   Copyright 2018 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.testtool.storage.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.slf4j.Logger;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.CSVReader;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
public class Storage implements nl.nn.testtool.storage.LogStorage {
	public static final long DEFAULT_MAXIMUM_FILE_SIZE = 1024 * 1024;
	public static final int DEFAULT_MAXIMUM_BACKUP_INDEX = 9;
	private String name;
	private Reader reader = new Reader();
	private Writer writer = new Writer();


//	TODO als je public weg laat, krijg je deze bij Download all bij Reports pane (zie ook constructor bij TestStorage) (vreemd dat je deze melding zo vaak krijgt als het aantal reports dat je download):
//	java.lang.IllegalAccessException: Class sun.reflect.misc.Trampoline can not access a member of class nl.nn.testtool.storage.file.Storage with modifiers ""
//	Continuing ...
//	java.lang.RuntimeException: failed to evaluate: <unbound>=Class.new();
	public Storage() {
		reader.setMaximumBackupIndex(DEFAULT_MAXIMUM_BACKUP_INDEX);
		writer.setMaximumFileSize(DEFAULT_MAXIMUM_FILE_SIZE);
		writer.setMaximumBackupIndex(DEFAULT_MAXIMUM_BACKUP_INDEX);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
		
	public void setReportsFilename(String reportsFilename) {
		reader.setReportsFilename(reportsFilename);
		writer.setReportsFilename(reportsFilename);
	}

	public void setMetadataFilename(String metadataFilename) {
		reader.setMetadataFilename(metadataFilename);
		writer.setMetadataFilename(metadataFilename);
	}

	public void setMaximumFileSize(long maximumFileSize) {
		writer.setMaximumFileSize(maximumFileSize);
	}

	public void setMaximumBackupIndex(int maximumBackupIndex) {
		reader.setMaximumBackupIndex(maximumBackupIndex);
		writer.setMaximumBackupIndex(maximumBackupIndex);
	}

	public void setFreeSpaceMinimum(long freeSpaceMinimum) {
		writer.setFreeSpaceMinimum(freeSpaceMinimum);
	}

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		reader.setMetadataExtractor(metadataExtractor);
		writer.setMetadataExtractor(metadataExtractor);
	}

	/**
	 * Set the metadata that should be written to file. At least storageId and
	 * storageSize should be set otherwise it's not possible to read the reports
	 * from file. The specified metadata will be written to file when a
	 * report is stored. When the metadata is requested it is read from file.
	 * For additional metadata (not available in the file) the metadata
	 * extractor object is called, this will need the report to be read from
	 * file. The metadata is cached in memory. For better performance at report
	 * generation time don't make metadata persistent that takes a lot of
	 * performance to extract from the report. For better performance when the
	 * metadata is read you could do the opposite.
	 * 
	 * @param metadataNames ...
	 */
	public void setPersistentMetadata(List metadataNames) {
		writer.setPersistentMetadata(metadataNames);
	}

	public void init() throws StorageException {
		reader.init();
		writer.init(reader.getStorageIds(writer.getMetadataFileLastModified(), writer.getSynchronizeRotate()));
	}

	public void store(Report report) throws StorageException {
		report.setStorage(this);
		writer.store(report, false);
	}

	public void storeWithoutException(Report report) {
		writer.storeWithoutException(report, false);
	}

	public String getWarningsAndErrors() {
		return writer.getWarningsAndErrors();
	}

	public int getSize() throws StorageException {
		// TODO make a faster implementation?
		return getStorageIds().size();
	}

	public List getStorageIds() throws StorageException {
		return reader.getStorageIds(writer.getMetadataFileLastModified(), writer.getSynchronizeRotate());
	}

	public List getMetadata(int maxNumberOfRecords, List metadataNames,
			List searchValues, int metadataValueType) throws StorageException {
		return reader.getMetadata(maxNumberOfRecords, metadataNames, searchValues,
				metadataValueType, writer.getMetadataFileLastModified(), writer.getSynchronizeRotate());
	}

	public Report getReport(Integer storageId) throws StorageException {
		Report report = reader.getReport(storageId, writer.getSynchronizeRotate());
		report.setStorage(this);
		return report;
	}

	public void close() {
		writer.close();
	}

	protected static void closeCSVReader(CSVReader csvReader, String action, Logger log) {
		try {
			csvReader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeReader(java.io.Reader reader, String action, Logger log) {
		try {
			reader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeInputStream(InputStream inputStream, String action, Logger log) {
		try {
			inputStream.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeOutputStream(OutputStream outputStream, String action, Logger log) {
		try {
			outputStream.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeOutputStreamWriter(OutputStreamWriter outputStreamWriter, String action, Logger log) {
		try {
			outputStreamWriter.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}

	protected static void logAndThrow(Logger log, String message) throws StorageException {
		log.error(message);
		throw new StorageException(message);
	}

	protected static void logAndThrow(Logger log, Exception e, String message) throws StorageException {
		message = message + ": " + e.getMessage();
		log.error(message, e);
		throw new StorageException(message, e);
	}

	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}
}
