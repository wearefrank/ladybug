package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
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
public class XmlStorage implements LogStorage {
	private String name;
	String metadataFile;
	private MetadataHandler metadataHandler;
	private File reportsFolder;
	Logger logger = LogUtil.getLogger(XmlStorage.class);

	/**
	 * Initializes the storage. Creating necessary folders and metadata file.
	 * @throws Exception Security exception that might be thrown by JVM.
	 */
	public void init() throws Exception {
		if (!reportsFolder.exists() && !reportsFolder.mkdirs()) {
			throw new Exception("Could not create reports folder!");
		}

		if (StringUtils.isEmpty(metadataFile)) {
			metadataFile = new File(reportsFolder, "metadata.xml").getAbsolutePath();
			logger.warn("Metadatafile was not set. Using " + metadataFile);
		}
		metadataHandler = new MetadataHandler(metadataFile);
	}

	@Override
	public void storeWithoutException(Report report) {
		try {
			// Make sure we are not overriding any previous report
			// that might have been handled by another metadatahandler file.
			long storageid;
			File reportFile;
			do {
				storageid = metadataHandler.getNextStorageId();
				reportFile = new File(reportsFolder, storageid + ".xml");
			} while (reportFile.isFile());

			Metadata metadata = Metadata.fromReport(report, storageid);
			reportFile.createNewFile();

			FileWriter writer = new FileWriter(reportFile, false);
			writer.write(report.toXml());
			writer.close();
			metadataHandler.add(metadata);
		} catch (Exception e) {
			logger.error("Error while writing the report!", e);
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
		return metadataHandler.getAsListofObjects();
	}

	@Override
	public List getTreeChildren(String path) {
		return null;
	}

	@Override
	public List getStorageIds(String path) throws StorageException {
		try {
			MetadataHandler handler = new MetadataHandler(path);
			return handler.getStorageIds();
		} catch (IOException e) {
			logger.error("Exception while trying to create metadatahandler.", e);
			throw new StorageException("Exception while trying to create metadatahandler.", e);
		}
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		File reportFile = new File(reportsFolder, storageId + ".xml");
		if(!reportFile.isFile()) {
			logger.error("Report with given storage id does not exits!");
			throw new StorageException("Report with given storage id does not exits!");
		}
		try {
			Report report =  Report.fromXml(new String(Files.readAllBytes(Paths.get(reportFile.getAbsolutePath()))));
			report.setStorage(this);
			return report;
		} catch (Exception e) {
			logger.error("Could not read the report file [" + reportFile.getName() + "]", e);
			throw new StorageException("Could not read the report file [" + reportFile.getName() + "]", e);
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

	public void setReportsFolder(String reportsFolder) {
		this.reportsFolder = new File(reportsFolder);
	}

	public void setMetadataFile(String metadataFile) {
		this.metadataFile = metadataFile;
	}
}
