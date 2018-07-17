/*
 * Created on 30-Mar-10
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool.storage.file;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.EscapeUtil;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Writer {
	private static Logger log = LogUtil.getLogger(Writer.class);
	private final String synchronizeStore = "";
	private String reportsFilename;
	private String metadataFilename;
	private File reportsFile;
	private File metadataFile;
	private long maximumFileSize = -1;
	private int maximumBackupIndex;
	private FileOutputStream reportsFileOutputStream;
	private FileOutputStream metadataFileOutputStream;
	private OutputStreamWriter metadataOutputStreamWriter;
	private List persistentMetadata;
	private String metadataHeader;
	private MetadataExtractor metadataExtractor;
	// TODO private maken en via een methode doen?
	protected int latestStorageId = 1;
	private long reportsFileLength;
	private long metadataFileLastModified;

	
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

	protected void setPersistentMetadata(List metadataNames) {
		persistentMetadata = metadataNames;
		metadataHeader = EscapeUtil.escapeCsv(persistentMetadata);
	}

	protected List getPersistentMetadata() {
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
	}

	protected void store(Report report, boolean preserveStorageId) throws StorageException {
		if (!preserveStorageId) {
			Integer storageId = new Integer(latestStorageId++);
			report.setStorageId(storageId);
		}
		byte[] reportBytes = getReportBytes(report);
		report.setStorageSize(new Long(reportBytes.length));
		List metadataValues = new ArrayList();
		for (int i = 0; i < persistentMetadata.size(); i++) {
			String metadataName = (String)persistentMetadata.get(i);
			metadataValues.add(metadataExtractor.getMetadata(report,
					metadataName, MetadataExtractor.VALUE_TYPE_STRING));
		}
		store(reportBytes, metadataValues);
	}

	protected void store(byte[] reportBytes, List metadataValues) throws StorageException {
		synchronized(synchronizeStore) {
			try {
				if (reportsFileOutputStream == null) {
					if (!metadataFile.exists()) {
						openFiles(false);
						writeMetadataHeader();
					} else if (validHeader()) {
						openFiles(true);
					} else {
						rotateFiles();
						openFiles(false);
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
					writeMetadataHeader();
					reportsFileLength = 0;
				}
//				if (preserveStorageId) {
//					if (report.getStorageId().intValue() >= latestStorageId) {
//						latestStorageId = report.getStorageId().intValue() + 1;
//					}
//				} else {
//					Integer storageId = new Integer(latestStorageId++);
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
				writeReportAndMetadata(reportBytes, EscapeUtil.escapeCsv(metadataValues));
			} catch(Throwable throwable) {
				StorageException storageException;
				if (throwable instanceof StorageException) {
					storageException = (StorageException)throwable;
				} else {
					String message = "Caught unexpected throwable storing report";
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
			if (!(throwable instanceof StorageException)) {
				log.error("Caught unexpected throwable storing report", throwable);
			}
		}
	}

	protected long getMetadataFileLastModified() {
		return metadataFileLastModified;
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
			Storage.logAndThrow(log, fileNotFoundException, "FileNotFoundException reading metadata header from file '" + metadataFile.getAbsolutePath() + "'");
		} catch(IOException ioException) {
			Storage.logAndThrow(log, ioException, "IOException reading metadata header from file '" + metadataFile.getAbsolutePath() + "'");
		} finally {
			if (bufferedReader != null) {
				Storage.closeReader(bufferedReader, "closing buffered reader after reading metadata header from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
			if (fileReader != null) {
				Storage.closeReader(fileReader, "closing file reader after reading metadata header from file '" + metadataFile.getAbsolutePath() + "'", log);
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
			Storage.logAndThrow(log, e, "IOException opening reports file '" + reportsFile.getAbsolutePath() + "'");
		}
		try {
			metadataFileOutputStream = new FileOutputStream(metadataFile, append);
		} catch(IOException e) {
			Storage.logAndThrow(log, e, "IOException opening metadata file '" + metadataFile.getAbsolutePath() + "'");
		}
		try {
			metadataOutputStreamWriter = new OutputStreamWriter(metadataFileOutputStream, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			Storage.logAndThrow(log, e, "UnsupportedEncodingException opening metadata output stream");
		}
	}
	
	private void writeMetadataHeader() throws StorageException {
		try {
			metadataOutputStreamWriter.write(metadataHeader);
			metadataOutputStreamWriter.flush();
		} catch(IOException ioException) {
			Storage.logAndThrow(log, ioException, "IOException writing metadata header to file '" + metadataFile.getAbsolutePath() + "'");
		}
		metadataFileLastModified = System.currentTimeMillis();
	}

	private void writeReportAndMetadata(byte[] reportBytes, String metadataCsvRecord) throws StorageException {
		try {
			reportsFileOutputStream.write(reportBytes);
			reportsFileOutputStream.flush();
			reportsFileLength = reportsFileLength + reportBytes.length;
		} catch(IOException e) {
			Storage.logAndThrow(log, e, "IOException writing report to file '" + reportsFile.getAbsolutePath() + "'");
		}
		try {
			metadataOutputStreamWriter.write("\n");
			metadataOutputStreamWriter.write(metadataCsvRecord);
			metadataOutputStreamWriter.flush();
		} catch(IOException e) {
			Storage.logAndThrow(log, e, "IOException writing metadata to file '" + metadataFile.getAbsolutePath() + "'");
		}
		metadataFileLastModified = System.currentTimeMillis();
	}

	private void closeFiles() {
		if (reportsFileOutputStream != null) {
			Storage.closeOutputStream(reportsFileOutputStream, "closing reports file '" + reportsFile.getAbsolutePath() + "'", log);
			reportsFileOutputStream = null;
		}
		if (metadataOutputStreamWriter != null) {
			Storage.closeOutputStreamWriter(metadataOutputStreamWriter, "closing metadata output stream writer", log);
			metadataOutputStreamWriter = null;
		}
		if (metadataFileOutputStream != null) {
			Storage.closeOutputStream(metadataFileOutputStream, "closing metadata file output stream '" + metadataFile.getAbsolutePath() + "'", log);
			metadataFileOutputStream = null;
		}
	}

	private void rotateFiles() throws StorageException {
		rotateFile(reportsFilename);
		rotateFile(metadataFilename);
	}

	private void rotateFile(String filename) throws StorageException {
		for (int i = maximumBackupIndex; i > 0; i--) {
			File oldFile = new File(filename + "." + i);
			if (i == maximumBackupIndex && oldFile.exists()) {
				deleteFile(oldFile);
			} else if (oldFile.exists()) {
				File newFile = new File(filename + "." + (i + 1));
				renameFile(oldFile, newFile);
			}
		}
		File oldFile = new File(filename);
		File newFile = new File(filename + ".1");
		renameFile(oldFile, newFile);
	}
	
	private static void deleteFile(File file) throws StorageException {
		if (!file.delete()) {
			Storage.logAndThrow(log, "Could not delete file '" + file.getAbsolutePath() + "'");
		}
	}
	
	private static void renameFile(File oldFile, File newFile) throws StorageException {
		if (!oldFile.renameTo(newFile)) {
			Storage.logAndThrow(log, "Could not rename file '" + oldFile.getAbsolutePath() + "' to '" + newFile + "'");
		}
	}

	private static byte[] getReportBytes(Report report) throws StorageException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		try {
			gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			objectOutputStream = new ObjectOutputStream(gzipOutputStream);
			objectOutputStream.writeObject(report);
		} catch(IOException e) {
			Storage.logAndThrow(log, e, "IOException storing report");
		} finally {
			if (objectOutputStream != null) {
				Storage.closeOutputStream(objectOutputStream, "closing object output stream after getting report bytes", log);
			}
			if (gzipOutputStream != null) {
				Storage.closeOutputStream(gzipOutputStream, "closing gzip output stream after getting report bytes", log);
			}
			Storage.closeOutputStream(byteArrayOutputStream, "closing byte array output stream after getting report bytes", log);
		}
		return byteArrayOutputStream.toByteArray();
	}

	protected void finalize() throws Throwable {
		closeFiles();
		super.finalize();
	}

}
