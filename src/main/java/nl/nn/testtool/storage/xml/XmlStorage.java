package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.SearchUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Handles report storage for ladybug.
 * Stores reports in xml format.
 */
public class XmlStorage implements LogStorage, CrudStorage {
	public static final String FILE_EXTENSION = ".report.xml";
	private String name, metadataFile, reportsFolderPath;
	private MetadataHandler metadataHandler;
	private File reportsFolder;
	Logger logger = LogUtil.getLogger(XmlStorage.class);

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
			logger.warn("Metadatafile was not set. Using " + metadataFile);
		}
		metadataHandler = new MetadataHandler(metadataFile, this, false);
	}

	/**
	 * Stores the given report in the given file.
	 * If file does not exist, it creates a new one.
	 * Otherwise, it overwrites the contents of the file.
	 *
	 * @param report Report to be written.
	 * @param file   File to be written.
	 * @throws StorageException
	 */
	private void store(Report report, File file) throws StorageException {
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
	public void storeWithoutException(Report report) {
		try {
			store(report);
		} catch (StorageException e) {
			logger.error("Error while writing the report!", e);
		}
	}

	@Override
	public void store(Report report) throws StorageException {
		try {
			// Make sure we are not overriding any previous report
			// that might have been handled by another metadatahandler file.
			Metadata metadata = metadataHandler.getMetadata(report.getCorrelationId());
			File reportFile;
			if (metadata == null) {
				File parentFolder = (report.getPath() != null) ? new File(reportsFolder, report.getPath()) : reportsFolder;
				String filename = report.getName();
				reportFile = new File(parentFolder, filename + FILE_EXTENSION);
				int i = 2;
				while (reportFile.isFile()) {
					filename = report.getName() + " (" + (i++) + ")";
					reportFile = new File(parentFolder, filename + FILE_EXTENSION);
				}
				report.setName(filename);
			} else {
				reportFile = new File(resolvePath(report.getCorrelationId()));
			}
			store(report, reportFile);
			metadata = Metadata.fromReport(report, metadataHandler.getNextStorageId(), reportFile.lastModified());
			metadataHandler.add(metadata);

		} catch (IOException e) {
			throw new StorageException("Error while writing the report!", e);
		}
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		Metadata m = metadataHandler.getMetadata(storageId);
		String path = resolvePath(m.correlationId);
		if (StringUtils.isEmpty(path))
			throw new StorageException("Could not resolve path of report.");

		File reportFile = new File(path);
		if (!reportFile.isFile()) {
			logger.error("Report with given storage id does not exits!");
			throw new StorageException("Report with given storage id does not exits!");
		}
		FileInputStream inputStream = null;
		XMLDecoder decoder = null;
		try {
			inputStream = new FileInputStream(reportFile);
			decoder = new XMLDecoder(new BufferedInputStream(inputStream));
			Report report = (Report) decoder.readObject();
			report.setStorage(this);
			inputStream.close();
			decoder.close();
			return report;
		} catch (Exception e) {
			if (decoder != null)
				decoder.close();

			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ioException) {
					throw new StorageException("Could not close the file input stream.", ioException);
				}
			}
			throw new StorageException("Could not read the report file [" + reportFile.getPath() + "]", e);
		}
	}

	@Override
	public void update(Report report) throws StorageException {
		delete(report);
		store(report);
	}

	@Override
	public void delete(Report report) throws StorageException {
		try {
			String path = resolvePath(report.getCorrelationId());
			if (path == null) {
				logger.warn("Could not find report file for report [" + report.getCorrelationId() + "]");
				return;
			}

			if (!new File(path).delete())
				throw new StorageException("Could not delete repot [" + report.getCorrelationId() + "] at [" + path + "]");

			metadataHandler.delete(report);
		} catch (IOException e) {
			throw new StorageException("Error while deleting the report [" + report.getCorrelationId() + "]", e);
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
		return metadataHandler.getAsListofObjects(maxNumberOfRecords, metadataNames, searchValues, metadataValueType);
	}

	@Override
	public List getTreeChildren(String path) {
		return null;
	}

	@Override
	public List getStorageIds(String path) throws StorageException {
		try {
			MetadataHandler handler = new MetadataHandler(path, this, false);
			return handler.getStorageIds();
		} catch (IOException e) {
			logger.error("Exception while trying to create metadatahandler.", e);
			throw new StorageException("Exception while trying to create metadatahandler.", e);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public int getFilterType(String column) {
		return 0;
	}

	@Override
	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	@Override
	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}

	/**
	 * Resolves the path of the report with given correlation Id
	 *
	 * @param correlationId Correlation Id of the report to be resolved.
	 * @return Path of the report. If report is not in metadata, null.
	 */
	private String resolvePath(String correlationId) {
		Metadata metadata = metadataHandler.getMetadata(correlationId);
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
		} catch (IOException ignored) {}
	}
}
