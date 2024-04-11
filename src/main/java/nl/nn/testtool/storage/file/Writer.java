/*
   Copyright 2020-2024 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.EscapeUtil;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.Import;

/**
 * @author Jaco de Groot
 */
public class Writer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final String synchronizeStore = "";
	private final String synchronizeRotate = "";
	private String reportsFilename;
	private String metadataFilename;
	private File reportsFile;
	private File metadataFile;
	private long maximumFileSize = -1;
	private int maximumBackupIndex;
	private long freeSpaceMinimum = -1;
	private FileOutputStream reportsFileOutputStream;
	private FileOutputStream metadataFileOutputStream;
	private OutputStreamWriter metadataOutputStreamWriter;
	private List<String> persistentMetadata;
	private String metadataHeader;
	private MetadataExtractor metadataExtractor;
	// TODO private maken en via een methode doen?
	protected int latestStorageId = 1;
	private long reportsFileLength;
	private long metadataFileLastModified;
	private SimpleDateFormat freeSpaceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private String lastExceptionMessage;

	protected void setReportsFilename(String reportsFilename) {
		this.reportsFilename = reportsFilename;
	}

	protected void setMetadataFilename(String metadataFilename) {
		this.metadataFilename = metadataFilename;
	}

	protected void setMaximumFileSize(long maximumFileSize) {
		this.maximumFileSize = maximumFileSize;
	}

	protected void setMaximumBackupIndex(int maximumBackupIndex) {
		this.maximumBackupIndex = maximumBackupIndex;
	}

	protected void setFreeSpaceMinimum(long freeSpaceMinimum) {
		this.freeSpaceMinimum = freeSpaceMinimum;
	}

	private long getFreeSpaceMinimum() {
		return freeSpaceMinimum;
	}

	protected void setPersistentMetadata(List<String> metadataNames) {
		persistentMetadata = metadataNames;
		metadataHeader = EscapeUtil.escapeCsv(persistentMetadata);
	}

	protected List<String> getPersistentMetadata() {
		return persistentMetadata;
	}

	protected void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}

	protected void init(List storageIds) {
		reportsFile = new File(reportsFilename);
		metadataFile = new File(metadataFilename);
		if (storageIds != null && storageIds.size() > 0) {
			Integer id = (Integer)storageIds.get(0);
			latestStorageId = id.intValue();
			latestStorageId++;
		}
		metadataFileLastModified = System.currentTimeMillis();
		if (freeSpaceMinimum == -1) {
			freeSpaceMinimum = maximumFileSize * (maximumBackupIndex + 1) * 10;
		}
	}

	protected void store(Report report, boolean preserveStorageId) throws StorageException {
		byte[] reportBytes = Export.getReportBytes(report);
		// Synchronize to keep order of storage id's in storage in incremental order
		synchronized(synchronizeStore) {
			if (!preserveStorageId) {
				Integer storageId = latestStorageId++;
				report.setStorageId(storageId);
			}
			report.setStorageSize(new Long(reportBytes.length));
			List metadataValues = new ArrayList();
			for (int i = 0; i < persistentMetadata.size(); i++) {
				String metadataName = persistentMetadata.get(i);
				metadataValues.add(metadataExtractor.getMetadata(report,
						metadataName, MetadataExtractor.VALUE_TYPE_STRING));
			}
			store(report.getName(), reportBytes, metadataValues);
		}
	}

	protected void store(String reportName, byte[] reportBytes, List<String> metadataValues) throws StorageException {
		synchronized(synchronizeStore) {
			try {
				if (reportsFileOutputStream == null) {
					if (!metadataFile.exists()) {
						openFiles(false);
						checkFreeSpace(reportName, reportBytes.length);
						writeMetadataHeader();
					} else if (validHeader()) {
						openFiles(true);
					} else {
						rotateFiles();
						openFiles(false);
						checkFreeSpace(reportName, reportBytes.length);
						writeMetadataHeader();
					}
				}
				// Use reportsFileLength instead of reportsFile.length() because
				// the length() method on a file doesn't return the correct
				// (latest) value all the time (at least with WSAD on Windows
				// XP) maybe because the output stream isn't closed after each
				// write.
				if (maximumFileSize != -1 && reportsFileLength > maximumFileSize) {
					closeFiles();
					rotateFiles();
					openFiles(false);
					checkFreeSpace(reportName, reportBytes.length);
					writeMetadataHeader();
					reportsFileLength = 0;
				}
//				if (preserveStorageId) {
//					if (report.getStorageId().intValue() >= latestStorageId) {
//						latestStorageId = report.getStorageId().intValue() + 1;
//					}
//				} else {
//					Integer storageId = latestStorageId++;
//					report.setStorageId(storageId);
//				}
//				byte[] reportBytes = getReportBytes(report);
//				report.setStorageSize(new Long(reportBytes.length));
//				List metadataValues = new ArrayList();
//				for (int i = 0; i < persistentMetadata.size(); i++) {
//					String metadataName = (String)persistentMetadata.get(i);
//					metadataValues.add(metadataExtractor.getMetadata(report,
//							metadataName, MetadataExtractor.VALUE_TYPE_STRING));
//				}
				checkFreeSpace(reportName, reportBytes.length);
				writeReportAndMetadata(reportBytes, EscapeUtil.escapeCsv(metadataValues));
			} catch(Throwable throwable) {
				StorageException storageException;
				if (throwable instanceof StorageException) {
					lastExceptionMessage = throwable.getMessage();
					storageException = (StorageException)throwable;
				} else {
					String message = "Caught unexpected throwable storing report";
					lastExceptionMessage = message + ": " + throwable.getMessage();
					log.error(message, throwable);
					storageException = new StorageException(message, throwable);
				}
				closeFiles();
				throw storageException;
			}
		}
	}

	protected void storeWithoutException(Report report, boolean preserveStorageId) {
		try {
			store(report, preserveStorageId);
		} catch(Throwable throwable) {
			lastExceptionMessage = throwable.getMessage();
			// When StorageException is should already be logged
			if (!(throwable instanceof StorageException)) {
				log.error("Caught unexpected throwable storing report", throwable);
			}
		}
	}

	protected long getMetadataFileLastModified() {
		return metadataFileLastModified;
	}

	protected String getSynchronizeRotate() {
		return synchronizeRotate;
	}

	protected void clear() throws StorageException {
		closeFiles();
		
		openFiles(false);
		writeMetadataHeader();
		reportsFileLength = 0;
		
		latestStorageId = 1;
		metadataFileLastModified = System.currentTimeMillis();
	}

	protected void close() {
		closeFiles();
	}

	private boolean validHeader() throws StorageException {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		String header = null;
		try {
			fileReader = new FileReader(metadataFile);
			bufferedReader = new BufferedReader(fileReader);
			header = bufferedReader.readLine();
		} catch(FileNotFoundException fileNotFoundException) {
			Export.logAndThrow(log, fileNotFoundException, "FileNotFoundException reading metadata header from file '" + metadataFile.getAbsolutePath() + "'");
		} catch(IOException ioException) {
			Export.logAndThrow(log, ioException, "IOException reading metadata header from file '" + metadataFile.getAbsolutePath() + "'");
		} finally {
			if (bufferedReader != null) {
				Import.closeReader(bufferedReader, "closing buffered reader after reading metadata header from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
			if (fileReader != null) {
				Import.closeReader(fileReader, "closing file reader after reading metadata header from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
		}
		if (metadataHeader.equals(header)) {
			return true;
		} else {
			return false;
		}
	}

	private void openFiles(boolean append) throws StorageException {
		try {
			reportsFileLength = reportsFile.length();
			reportsFileOutputStream = new FileOutputStream(reportsFile, append);
		} catch(IOException e) {
			Export.logAndThrow(log, e, "IOException opening reports file '" + reportsFile.getAbsolutePath() + "'");
		}
		try {
			metadataFileOutputStream = new FileOutputStream(metadataFile, append);
		} catch(IOException e) {
			Export.logAndThrow(log, e, "IOException opening metadata file '" + metadataFile.getAbsolutePath() + "'");
		}
		try {
			metadataOutputStreamWriter = new OutputStreamWriter(metadataFileOutputStream, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			Export.logAndThrow(log, e, "UnsupportedEncodingException opening metadata output stream");
		}
	}

	private void writeMetadataHeader() throws StorageException {
		try {
			metadataOutputStreamWriter.write(metadataHeader);
			metadataOutputStreamWriter.flush();
		} catch(IOException ioException) {
			Export.logAndThrow(log, ioException, "IOException writing metadata header to file '" + metadataFile.getAbsolutePath() + "'");
		}
		metadataFileLastModified = System.currentTimeMillis();
	}

	private void writeReportAndMetadata(byte[] reportBytes, String metadataCsvRecord) throws StorageException {
		try {
			reportsFileOutputStream.write(reportBytes);
			reportsFileOutputStream.flush();
			reportsFileLength = reportsFileLength + reportBytes.length;
		} catch(IOException e) {
			Export.logAndThrow(log, e, "IOException writing report to file '" + reportsFile.getAbsolutePath() + "'");
		}
		try {
			metadataOutputStreamWriter.write("\n");
			metadataOutputStreamWriter.write(metadataCsvRecord);
			metadataOutputStreamWriter.flush();
		} catch(IOException e) {
			Export.logAndThrow(log, e, "IOException writing metadata to file '" + metadataFile.getAbsolutePath() + "'");
		}
		metadataFileLastModified = System.currentTimeMillis();
	}

	private void closeFiles() {
		if (reportsFileOutputStream != null) {
			Export.closeOutputStream(reportsFileOutputStream, "closing reports file '" + reportsFile.getAbsolutePath() + "'", log);
			reportsFileOutputStream = null;
		}
		if (metadataOutputStreamWriter != null) {
			Export.closeOutputStreamWriter(metadataOutputStreamWriter, "closing metadata output stream writer", log);
			metadataOutputStreamWriter = null;
		}
		if (metadataFileOutputStream != null) {
			Export.closeOutputStream(metadataFileOutputStream, "closing metadata file output stream '" + metadataFile.getAbsolutePath() + "'", log);
			metadataFileOutputStream = null;
		}
	}

	private void rotateFiles() throws StorageException {
		synchronized(synchronizeRotate) {
			for (int i = maximumBackupIndex; i >= 0; i--) {
				rotateFile(reportsFilename, i);
				rotateFile(metadataFilename, i);
			}
		}
	}

	private void rotateFile(String filename, int i) throws StorageException {
		File oldFile;
		if (i == 0) {
			oldFile = new File(filename);
		} else {
			oldFile = new File(filename + "." + i);
		}
		if (i == maximumBackupIndex && oldFile.exists()) {
			deleteFile(oldFile);
		} else if (oldFile.exists()) {
			File newFile = new File(filename + "." + (i + 1));
			renameFile(oldFile, newFile);
		}
	}

	private static void deleteFile(File file) throws StorageException {
		if (!file.delete()) {
			Export.logAndThrow(log, "Could not delete file '" + file.getAbsolutePath() + "'");
		}
	}

	private static void renameFile(File oldFile, File newFile) throws StorageException {
		try {
			Files.move(oldFile.toPath(), newFile.toPath());
		} catch (Exception e) {
			Export.logAndThrow(log, e,
					"Could not rename file '" + oldFile.getAbsolutePath() + "' to '" + newFile.getAbsolutePath() + "'");
		}
	}

	protected void finalize() throws Throwable {
		closeFiles();
		super.finalize();
	}

	public String getWarningsAndErrors() {
		String message = getFreeSpaceWarning();
		if (lastExceptionMessage != null) {
			if (message == null) {
				message = lastExceptionMessage;
			} else {
				message = message + ". " + lastExceptionMessage;
			}
		}
		return message;
	}

	private String getFreeSpaceWarning() {
		// If file doesn't exist getFreeSpace() will return 0. Hence, check whether file exists as getFreeSpaceWarning()
		// can be called before any reports have been stored (this check isn't needed in checkFreeSpace() as it is
		// called after openFiles() has been called).
		if (reportsFile.exists()) {
			long freeSpace = getFreeSpace();
			long minimum = getFreeSpaceMinimum();
			if (maximumFileSize != -1 && freeSpace < minimum) {
				return "Running out of disk space (" + freeSpace
						+ " bytes left) (reports are not stored while free space is below " + minimum
						+ " bytes to prevent corrupt storage files)";
			}
		}
		return null;
	}

	private String checkFreeSpace(String reportName, int reportSize) throws StorageException {
		String freeSpaceError = null;
		long freeSpace = getFreeSpace();
		long minimum = getFreeSpaceMinimum();
		if (maximumFileSize != -1 && freeSpace < minimum) {
			freeSpaceError = "Report '" + reportName + "' discarded because disk space too low (" + freeSpace
					+ " bytes left) (" + freeSpaceDateFormat.format(new Date()) + ")";
		} else if (freeSpace < reportSize) {
			freeSpaceError = "Report '" + reportName + "' discarded because disk space too low (" + freeSpace
					+ " bytes left while report is " + reportSize + " bytes) ("
					+ freeSpaceDateFormat.format(new Date()) + ")";
		}
		if (freeSpaceError != null) {
			lastExceptionMessage = freeSpaceError;
			throw new StorageException(freeSpaceError);
		}
		return null;
	}

	private long getFreeSpace() {
		return reportsFile.getFreeSpace();
	}

}
