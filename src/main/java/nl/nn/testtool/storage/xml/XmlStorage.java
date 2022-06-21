/*
   Copyright 2020-2022 WeAreFrank!

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
package nl.nn.testtool.storage.xml;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.SearchUtil;

/**
 * Handles report storage for ladybug.
 * Stores reports in xml format.
 */
public class XmlStorage implements CrudStorage {
	public static final String FILE_EXTENSION = ".report.xml";
	private String name, metadataFile, reportsFolderPath;
	private MetadataHandler metadataHandler;
	private File reportsFolder;
	Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Initializes the storage. Creating necessary folders and metadata file.
	 *
	 * @throws Exception Security exception that might be thrown by JVM.
	 */
	public void init() throws Exception {
		if (StringUtils.isEmpty(reportsFolderPath))
			throw new StorageException("Report folder path is empty. Please provide a path.");

		reportsFolder = new File(reportsFolderPath);

		if (StringUtils.isEmpty(metadataFile)) {
			metadataFile = new File(reportsFolder, "metadata.xml").getAbsolutePath();
			log.warn("Metadatafile was not set. Using " + metadataFile);
		}
		metadataHandler = new MetadataHandler(metadataFile, this, false);
		updateMetadata();
	}

	/**
	 * Stores the given report in the given file.
	 * If file does not exist, it creates a new one.
	 * Otherwise, it overwrites the contents of the file.
	 *
	 * @param report Report to be written.
	 * @param file   File to be written.
	 * @throws StorageException ...
	 */
	protected void store(Report report, File file) throws StorageException {
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			FileOutputStream outputStream = new FileOutputStream(file);
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(outputStream));
			try {
				encoder.writeObject(report);
			} finally {
				encoder.close();
				outputStream.close();
			}
		} catch (Exception e) {
			throw new StorageException("Could not write report [" + report.getCorrelationId() + "] to [" + file.getPath() + "].", e);
		}
	}

	@Override
	public void store(Report report) throws StorageException {
		try {
			// Storage uses the clone to save, because it changes reports name,
			// and it can cause unwanted side-effects on report names.
			Report copy = report.clone();
			store(copy, true);
		} catch (ClassCastException | CloneNotSupportedException e) {
			throw new StorageException("Could not clone the report for new storage.", e);
		}
	}

	/**
	 * Stores the given report in the filesystem as an XML file.
	 *
	 * @param report Report to be saved.
	 * @param addNew False for storage id to stay same. True to reassign storage id, and force new file creation.
	 * @throws StorageException
	 */
	private void store(Report report, boolean addNew) throws StorageException {
		try {
			// Make sure we are not overriding any previous report
			// that might have been handled by another metadatahandler file.
			Metadata metadata = metadataHandler.getMetadata(report.getStorageId());
			File reportFile;
			if (metadata == null || addNew) {
				File parentFolder = (report.getPath() != null) ? new File(reportsFolder, report.getPath()) : reportsFolder;
				String original_name = report.getName().replaceAll("[<>:\"\\/\\\\\\|\\?\\*]", "_");
				String filename = original_name;
				reportFile = new File(parentFolder, filename + FILE_EXTENSION);
				int i = 2;
				while (reportFile.isFile()) {
					filename = original_name + " (" + (i++) + ")";
					reportFile = new File(parentFolder, filename + FILE_EXTENSION);
				}
				report.setName(filename);
			} else {
				reportFile = new File(resolvePath(report.getStorageId()));
			}

			if (addNew || report.getStorageId() == null || metadataHandler.contains(report.getStorageId())) {
				int storageId = metadataHandler.getNextStorageId();
				while (metadataHandler.contains(storageId))
					storageId = metadataHandler.getNextStorageId();
				report.setStorageId(storageId);
			}

			store(report, reportFile);
			metadata = Metadata.fromReport(report, reportFile, reportsFolder);
			metadataHandler.add(metadata);

		} catch (IOException e) {
			throw new StorageException("Error while writing the report!", e);
		}
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		Metadata m = metadataHandler.getMetadata(storageId);
		String path = resolvePath(m.storageId);
		if (StringUtils.isEmpty(path)) {
			log.warn("Given report path is empty.");
			return null;
		}

		File reportFile = new File(path);
		if (!reportFile.isFile()) {
			log.warn("Given report path does not resolve to a file.");
			return null;
		}
		Report report = readReportFromFile(reportFile);
		if (report.getStorageId() != m.storageId)
			report.setStorageId(storageId);
		return report;
	}

	@Override
	public void update(Report report) throws StorageException {
		try {
			Metadata metadata = metadataHandler.getMetadata(report.getStorageId());
			delete(report);
			metadata.path = report.getPath();
			metadataHandler.add(metadata);
			store(report, false);
		} catch (IOException e) {
			throw new StorageException("Returned an error while updating metadata.", e);
		}
	}

	@Override
	public void delete(Report report) throws StorageException {
		try {
			String path = resolvePath(report.getStorageId());
			if (path == null) {
				log.warn("Could not find report file for report with storage id [" + report.getStorageId() + "] correlation id [" + report.getCorrelationId() + "]");
				return;
			}
			// Delete file
			File file = new File(path);
			if (!file.delete())
				throw new StorageException("Could not delete report with storage id [" + report.getStorageId() + "] correlation id [" + report.getCorrelationId() + "] at [" + path + "]");

			// Delete all parent folders which are empty.
			file = file.getParentFile();
			while (file.delete())
				file = file.getParentFile();
			metadataHandler.delete(report);
		} catch (IOException e) {
			throw new StorageException("Error while deleting the report with storage id [" + report.getStorageId() + "] correlation id [" + report.getCorrelationId() + "]", e);
		}
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getSize() throws StorageException {
		return metadataHandler.getSize();
	}

	@Override
	public List getStorageIds() throws StorageException {
		return metadataHandler.getStorageIds();
	}

	@Override
	public List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues, int metadataValueType) throws StorageException {
		updateMetadata();
		return metadataHandler.getAsListofObjects(maxNumberOfRecords, metadataNames, searchValues, metadataValueType);
	}

	@Override
	public void clear() throws StorageException {
		List<Integer> storageIds = getStorageIds();
		for (Integer storageId : storageIds) {
			delete(getReport(storageId));
		}
	}

	@Override
	public void close() {
	}

	@Override
	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	@Override
	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	protected File getReportsFolder() {
		return reportsFolder;
	}

	@Override
	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}

	/**
	 * Reads the report from the given file.
	 *
	 * @param file File to be read from.
	 * @throws StorageException ...
	 * @return Report generated from the given file.
	 */
	protected Report readReportFromFile(File file) throws StorageException {
		if (file == null || !file.isFile() || !file.getName().endsWith(XmlStorage.FILE_EXTENSION))
			return null;

		log.debug("Reading from a new file: " + file.getPath());
		FileInputStream inputStream = null;
		XMLDecoder decoder = null;
		try {
			inputStream = new FileInputStream(file);
			decoder = new XMLDecoder(new BufferedInputStream(inputStream));

			Report report = (Report) decoder.readObject();
			report.setStorage(this);

			decoder.close();
			inputStream.close();
			return report;
		} catch (Exception e) {
			if (decoder != null)
				decoder.close();
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception ignored) {
					log.error("Could not close the xml file.", ignored);
				}
			}
			throw new StorageException("Exception while deserializing data from report file.", e);
		}
	}

	/**
	 * Resolves the path of the report with given storage Id
	 *
	 * @param storageId Storage Id of the report to be resolved.
	 * @return Path of the report. If report is not in metadata, null.
	 */
	protected String resolvePath(Integer storageId) {
		Metadata metadata = metadataHandler.getMetadata(storageId);
		if (metadata == null)
			return null;

		File parentFolder = reportsFolder;
		if (StringUtils.isNotEmpty(metadata.path) && !metadata.path.equalsIgnoreCase("null"))
			parentFolder = new File(reportsFolder, metadata.path);

		return new File(parentFolder, metadata.name + FILE_EXTENSION).getPath();
	}

	public void setReportsFolder(String reportsFolder) {
		reportsFolderPath = reportsFolder;
	}

	public void setMetadataFile(String metadataFile) {
		this.metadataFile = metadataFile;
	}

	/**
	 * Resets the metadata handler by forcing it to build file from searching through directory.
	 */
	public void updateMetadata() {
		try {
			metadataHandler.updateMetadata();
		} catch (IOException ignored) {
			log.error("Exception while updating the metadata from filesystem.", ignored);
		}
	}
}
