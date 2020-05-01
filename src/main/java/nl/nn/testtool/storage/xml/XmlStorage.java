package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.SearchUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Handles report storage for ladybug.
 * Stores reports in xml format.
 */
public class XmlStorage implements LogStorage, CrudStorage {
	private String name, metadataFile, reportsFolderPath, stage;
	private MetadataHandler metadataHandler;
	private File reportsFolder;
	Logger logger = LogUtil.getLogger(XmlStorage.class);

	/**
	 * Initializes the storage. Creating necessary folders and metadata file.
	 *
	 * @throws Exception Security exception that might be thrown by JVM.
	 */
	public void init() throws Exception {
		getReportsFolder();
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
	 * @param path   File to be written.
	 * @throws StorageException
	 */
	private void store(Report report, String path) throws StorageException {
		try {
			File file = new File(path);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			FileWriter writer = new FileWriter(file, false);
			String xml = report.toXml(null, false, true);
			writer.write(xml);
			writer.close();
		} catch (Exception e) {
			throw new StorageException("Could not write report [" + report.getCorrelationId() + "] to [" + path + "].", e);
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
				int storageid;
				File parentFolder = (report.getPath() != null) ? new File(reportsFolder, report.getPath()) : reportsFolder;
				do {
					storageid = metadataHandler.getNextStorageId();
					reportFile = new File(parentFolder, storageid + ".xml");
				} while (reportFile.isFile());

				metadata = Metadata.fromReport(report, storageid);
			} else {
				reportFile = new File(resolvePath(report.getCorrelationId()));
			}
			store(report, reportFile.getPath());
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
		try {
			Report report = Report.fromXml(new String(Files.readAllBytes(Paths.get(reportFile.getAbsolutePath()))));
			report.setStorage(this);
			return report;
		} catch (Exception e) {
			String err = "Could not read the report file [" + reportFile.getPath() + "]";
			logger.error(err, e);
			throw new StorageException(err, e);
		}
	}

	@Override
	public void update(Report report) throws StorageException {
		try {
			String oldpath = resolvePath(report.getCorrelationId());
			metadataHandler.update(report);

			store(report, resolvePath(report.getCorrelationId()));
			if (StringUtils.isNotEmpty(oldpath))
				new File(oldpath).delete();
		} catch (IOException e) {
			throw new StorageException("Error during updating report.", e);
		}
	}

	@Override
	public void delete(Report report) throws StorageException {
		try {
			metadataHandler.delete(report);

			String path = resolvePath(report.getCorrelationId());
			if (path == null) {
				logger.warn("Could not find report file for report [" + report.getCorrelationId() + "]");
				return;
			}

			if (!new File(path).delete())
				throw new StorageException("Could not delete repot [" + report.getCorrelationId() + "] at [" + path + "]");
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
	 * Resolves and returns reports folder based on reportsFolderPath, and dtap.stage
	 * @return reports folder
	 * @throws StorageException If folder does not exist and we can not create.
	 */
	private File getReportsFolder() throws StorageException {
		if (reportsFolder != null)
			return reportsFolder;

		if (StringUtils.isEmpty(reportsFolderPath)) {
			// TODO: (For Jaco) which paths would you like to use as default?
			if (stage != null && stage.equalsIgnoreCase("LOC")) {
				reportsFolderPath = "ladybug-storage";
			} else {
				reportsFolderPath = "ladybug-storage";
			}
		}

		reportsFolder = new File(reportsFolderPath);
		if (!reportsFolder.exists() && !reportsFolder.mkdirs())
			throw new StorageException("Could not create reports folder!");

		return reportsFolder;
	}

	/**
	 * Resolves the path of the report with given correlation Id
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

		return new File(parentFolder, metadata.storageId + ".xml").getPath();
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
	public void resetMetadataFile() {
		MetadataHandler old = metadataHandler;
		try {
			metadataHandler = new MetadataHandler(metadataFile, this, true);
		} catch (IOException e) {
			logger.error("Error during metadata rebuilding. Reverting to the old version.", e);
			metadataHandler = old;
		}
	}
}
