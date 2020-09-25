/*
   Copyright 2018-2019 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.CSVReader;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.SearchUtil;

import org.apache.log4j.Logger;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Reader {
	private static Logger log = LogUtil.getLogger(Reader.class);
	private String reportsFilename;
	private String metadataFilename;
	private File reportsFile;
	private File metadataFile;
	private int maximumBackupIndex;
	private long metadataFileLastModifiedByWriter = Long.MIN_VALUE;
	private long metadataFileLastModifiedByOthers = Long.MIN_VALUE;
	private List metadataCacheReadOnly = new ArrayList();
	private List metadataCacheReadOnlyPerFile = new ArrayList();
	private MetadataExtractor metadataExtractor;
	private static final List METADATA_NAMES_STORAGE_ID = new ArrayList();
	static {
		METADATA_NAMES_STORAGE_ID.add("storageId");
	}

	protected void setReportsFilename(String reportsFilename) {
		this.reportsFilename = reportsFilename;
	}

	protected void setMetadataFilename(String metadataFilename) {
		this.metadataFilename = metadataFilename;
	}

	protected void setMaximumBackupIndex(int maximumBackupIndex) {
		this.maximumBackupIndex = maximumBackupIndex;
	}

	protected void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}

	protected void init() {
		reportsFile = new File(reportsFilename);
		metadataFile = new File(metadataFilename);
	}

	protected List getStorageIds(long metadataFileLastModifiedByWriter) throws StorageException {
		List result = new ArrayList();
		List metadata = getMetadata(-1, METADATA_NAMES_STORAGE_ID, null,
				MetadataExtractor.VALUE_TYPE_OBJECT,
				metadataFileLastModifiedByWriter);
		Iterator iterator = metadata.iterator();
		while (iterator.hasNext()) {
			List record = (List)iterator.next();
			result.add(record.get(0));
		}
		return result;
	}

	protected List getMetadata(int numberOfRecords, List metadataNames,
			List searchValues, int metadataValueType,
			long metadataFileLastModifiedByWriter) throws StorageException {
		List metadataReadOnly;
		synchronized(metadataCacheReadOnly) {
			// The last modified time of a file isn't updated until the file
			// output stream is closed (at least with WSAD on Windows XP). As
			// the writer class doesn't close the file the writer class must
			// pass this info to this class. In case the metadata file was
			// edited by hand it should also be detected.
			if (metadataFileLastModifiedByWriter != this.metadataFileLastModifiedByWriter
					|| metadataFile.lastModified() != this.metadataFileLastModifiedByOthers) {
				this.metadataFileLastModifiedByWriter = metadataFileLastModifiedByWriter;
				metadataFileLastModifiedByOthers = metadataFile.lastModified();
				List newMetadataCacheReadOnly = new ArrayList();
				for (int i = maximumBackupIndex; i >= 0; i--) {
					File file;
					if (i == 0) {
						file = metadataFile;
					} else {
						file = new File(metadataFilename + "." + i);
					}
					if (file.exists()) {
						while (metadataCacheReadOnlyPerFile.size() <= i) {
							metadataCacheReadOnlyPerFile.add(0, new ArrayList());
						}
						List oldMetadataCurrentFile = (List)metadataCacheReadOnlyPerFile.get(i);
						List metadataCurrentFile = new ArrayList();
						getMetadataOrReportLocationFromFile(metadataExtractor,
								file, oldMetadataCurrentFile,
								metadataCurrentFile, null);
						newMetadataCacheReadOnly.addAll(0, metadataCurrentFile);
						metadataCacheReadOnlyPerFile.set(i, metadataCurrentFile);
					}
				}
				metadataCacheReadOnly = newMetadataCacheReadOnly;
			}
			metadataReadOnly = metadataCacheReadOnly;
		}
		List result = new ArrayList();
		for (int i = 0; i < metadataReadOnly.size() &&  (numberOfRecords == -1 || i < numberOfRecords); i++) {
			Map metadataRecord = (Map)metadataReadOnly.get(i);
			// Check whether it's already possible to exclude this record from
			// the result (based on the search values and the already available
			// metadata) and prevent unnecessary (possible costly) metadata
			// retrieval.
			boolean exclude = false;
			if (searchValues != null) {
				List partialValues = new ArrayList();
				List partialSearchValues = new ArrayList();
				for (int j = 0; j < searchValues.size(); j++) {
					String searchValue = (String)searchValues.get(j);
					if (searchValue != null) {
						String metadataName = (String)metadataNames.get(j);
						synchronized (metadataRecord) {
							if (metadataRecord.keySet().contains(metadataName)) {
								partialValues.add(
										metadataExtractor.fromObjectToMetadataValueType(metadataName,
												metadataRecord.get(metadataName), metadataValueType)
										);
								partialSearchValues.add(searchValue);
							}
						}
					}
				}
				if (!SearchUtil.matches(partialValues, partialSearchValues)) {
					exclude = true;
					if (log.isDebugEnabled()) {
						log.debug("Exclude report based on search values (" + partialSearchValues + ") and already available metadata (" + partialValues + ")");
					}
				}
			}
			if (!exclude) {
				Report report = null;
				List resultRecord = new ArrayList();
				Iterator metadataNamesIterator = metadataNames.iterator();
				while (metadataNamesIterator.hasNext()) {
					String metadataName = (String)metadataNamesIterator.next();
					Object metadataValue;
					boolean metadataRecordContainsMetadataName;
					synchronized (metadataRecord) {
						metadataRecordContainsMetadataName = metadataRecord.keySet().contains(metadataName);
					}
					if (!metadataRecordContainsMetadataName) {
						Integer storageId;
						synchronized (metadataRecord) {
							storageId = (Integer)metadataRecord.get("storageId");
						}
						if (report == null) {
							report = getReportWithoutException(storageId);
						}
						if (report == null) {
							resultRecord = null;
							break;
						} else {
							if (log.isDebugEnabled()) {
								log.debug("Extract metadata '" + metadataName + "' for report with storage id " + storageId);
							}
							metadataValue = metadataExtractor.getMetadata(
									report, metadataName,
									MetadataExtractor.VALUE_TYPE_OBJECT);
							synchronized (metadataRecord) {
								metadataRecord.put(metadataName, metadataValue);
							}
						}
					} else {
						synchronized (metadataRecord) {
							metadataValue = metadataRecord.get(metadataName);
						}
					}
					resultRecord.add(metadataExtractor.fromObjectToMetadataValueType(metadataName, metadataValue, metadataValueType));
				}
				if (resultRecord != null && SearchUtil.matches(resultRecord, searchValues)) {
					result.add(resultRecord);
				}
			}
		}
		return result;
	}

	private Report getReportWithoutException(Integer storageId) {
		Report report = null;
		try {
			report = getReport(storageId);
		} catch(Throwable throwable) {
			if (!(throwable instanceof StorageException)) {
				log.error("Caught unexpected throwable reading report from file", throwable);
			}
		}
		return report;
	}

	protected byte[] getReportBytes(Integer storageId) throws StorageException {
		byte[] reportBytes = null;
		ReportLocation reportLocation = null;
		int foundInIndex = -1;
		for (int i = maximumBackupIndex; i >= 0 && foundInIndex == -1; i--) {
			File file;
			if (i == 0) {
				file = metadataFile;
			} else {
				file = new File(metadataFilename + "." + i);
			}
			if (file.exists()) {
				reportLocation = getMetadataOrReportLocationFromFile(null, file, null, null, storageId);
				if (reportLocation != null) {
					foundInIndex = i;
					i = -1;
				}
			}
		}
		if (foundInIndex != -1 ) {
			FileInputStream fileInputStream = null;
			try {
				File file;
				if (foundInIndex == 0) {
					file = reportsFile;
				} else {
					file = new File(reportsFilename + "." + foundInIndex);
				}
				fileInputStream = new FileInputStream(file);
				fileInputStream.skip(reportLocation.offset);
				reportBytes = new byte[reportLocation.size.intValue()];
				fileInputStream.read(reportBytes, 0, reportBytes.length);
			} catch(IOException e) {
				Storage.logAndThrow(log, e, "IOException reading report from file");
			} finally {
				if (fileInputStream != null) {
					Storage.closeInputStream(fileInputStream, "closing file input stream after reading report from file", log);
				}
			}
		}
		return reportBytes;	
	}

	protected Report getReport(Integer storageId) throws StorageException {
		Report report = null;
		byte[] reportBytes = getReportBytes(storageId);
		if (reportBytes != null) {
			ByteArrayInputStream byteArrayInputStream = null;
			GZIPInputStream gzipInputStream = null;
			ObjectInputStream objectInputStream = null;
			try {
				byteArrayInputStream = new ByteArrayInputStream(reportBytes);
				gzipInputStream = new GZIPInputStream(byteArrayInputStream);
				objectInputStream = new ObjectInputStream(gzipInputStream);
				report = (Report)objectInputStream.readObject();
				report.setStorageId(storageId);
				report.setStorageSize(new Long(reportBytes.length));
			} catch(IOException e) {
				Storage.logAndThrow(log, e, "IOException reading report from bytes");
			} catch(ClassNotFoundException e) {
				Storage.logAndThrow(log, e, "ClassNotFoundException reading report from file");
			} finally {
				if (objectInputStream != null) {
					Storage.closeInputStream(objectInputStream, "closing object input stream after reading report from file", log);
				}
				if (gzipInputStream != null) {
					Storage.closeInputStream(gzipInputStream, "closing gzip input stream after reading report from file", log);
				}
				if (byteArrayInputStream != null) {
					Storage.closeInputStream(byteArrayInputStream, "closing byte array input stream after reading report from file", log);
				}
			}
		}
		return report;	
	}

	/**
	 * Get metadata or report location from file. The old metadata may contain
	 * metadata not present in the metadata file. As it might be costly to
	 * retrieve this metadata not present in the file the records from the old
	 * metadata are copied to the new metadata for reports already present in
	 * the old metadata. Metadata for reports not already present in the old
	 * metadata is read from the metadata file.
	 */
	private ReportLocation getMetadataOrReportLocationFromFile(
			MetadataExtractor metadataExtractor, File metadataFile,
			List oldMetadata, List metadata, Integer storageIdOfReportToLocate
			) throws StorageException {
		CSVReader csvReader = null;
		FileInputStream fileInputStream = null;
		InputStreamReader inputStreamReader = null;
		LineNumberReader lineNumberReader = null;
		try {
			fileInputStream = new FileInputStream(metadataFile);
			inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			lineNumberReader = new LineNumberReader(inputStreamReader);
			int recordNumber = 0;
			csvReader = new CSVReader(lineNumberReader);
			List metadataHeaderParsed = csvReader.nextCSV();
			int storageIdIndex = -1;
			int storageSizeIndex = -1;
			for (int i = 0; i < metadataHeaderParsed.size() && (storageIdIndex == -1 || storageSizeIndex == -1); i++) {
				if ("storageId".equals(metadataHeaderParsed.get(i))) {
					storageIdIndex = i;
				} else if ("storageSize".equals(metadataHeaderParsed.get(i))) {
					storageSizeIndex = i;
				}
			}
			if (storageIdIndex != -1 && storageSizeIndex != -1) {
				long offset = 0;
				int oldMetadataIndex = -1;
				if (oldMetadata != null) {
					oldMetadataIndex = oldMetadata.size() - 1;
				}
				while (csvReader.hasMoreElements()) {
					recordNumber++;
					List list = csvReader.nextCSV();
					if (list.size() == metadataHeaderParsed.size()) {
						Integer storageId;
						Integer storageSize = null;
						try {
							storageId = new Integer((String)list.get(storageIdIndex));
							storageSize = new Integer((String)list.get(storageSizeIndex));
						} catch(NumberFormatException e) {
							storageId = null;
						}
						if (storageId != null) {
							if (storageIdOfReportToLocate != null) {
								if (storageIdOfReportToLocate.equals(storageId)) {
									ReportLocation reportLocation = new ReportLocation();
									reportLocation.offset = offset;
									reportLocation.size = storageSize;
									return reportLocation;
								} else {
									offset = offset + storageSize.intValue();
								}
							} else {
								Map oldMetadataRecord = null;
								if (oldMetadataIndex > -1) {
									oldMetadataRecord = (Map)oldMetadata.get(oldMetadataIndex);
									if (storageId.equals(oldMetadataRecord.get("storageId"))) {
										oldMetadataIndex--;
									} else {
										oldMetadataRecord = null;
									}
								}
								if (oldMetadataRecord == null) {
									if (log.isDebugEnabled()) {
										log.debug("Get metadata for report with storage id " + storageId + " from metadata file");
									}
									Map metadataRecord = new HashMap();
									for (int i = 0; i < metadataHeaderParsed.size(); i++) {
										String metadataName = (String)metadataHeaderParsed.get(i);
										Object metadataValue = list.get(i);
										metadataValue = metadataExtractor.fromStringToMetadataValueType(
												metadataName,
												(String)metadataValue,
												MetadataExtractor.VALUE_TYPE_OBJECT);
										metadataRecord.put(metadataName, metadataValue);
									}
									metadata.add(0, metadataRecord);
								} else {
									if (log.isDebugEnabled()) {
										log.debug("Reusing old metadata for report with storage id " + storageId);
									}
									metadata.add(0, oldMetadataRecord);
								}
							}
						} else {
							log.warn("Invalid metadata record (invalid storage id) found with record number: " + recordNumber);
						}
					} else {
						log.warn("Invalid metadata record (number of fields not equal to number of fields in header) found with record number: " + recordNumber);
					}
				}
			} else {
				log.warn("Invalid header in metadata file '" + metadataFile.getAbsolutePath() + "'");
			}
		} catch(IOException e) {
			Storage.logAndThrow(log, e, "IOException reading metadata from file '" + metadataFile.getAbsolutePath() + "'");
		} finally {
			if (csvReader != null) {
				Storage.closeCSVReader(csvReader, "closing cvs reader after reading metadata from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
			if (lineNumberReader != null) {
				Storage.closeReader(lineNumberReader, "closing line number reader after reading metadata from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
			if (inputStreamReader != null) {
				Storage.closeReader(inputStreamReader, "closing input stream reader after reading metadata from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
			if (fileInputStream != null) {
				Storage.closeInputStream(fileInputStream, "closing file input stream after reading metadata from file '" + metadataFile.getAbsolutePath() + "'", log);
			}
		}
		return null;
	}

	protected void clear() throws StorageException {
		metadataCacheReadOnly.clear();
		metadataCacheReadOnlyPerFile.clear();
	}

	class ReportLocation {
		protected long offset = 0;
		protected Integer size;
	}

}
