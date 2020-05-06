package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Handles metadata for {@link XmlStorage}.
 */
public class MetadataHandler {
	private final String DEFAULT_PATH = "metadata.xml";
	private Storage storage;
	private HashMap<String, Metadata> metadataMap;
	protected File metadataFile;
	private int lastStorageId;
	private Logger logger = LogUtil.getLogger(this.getClass());

	/**
	 * Creates a new file with the given path.
	 * If file exists, then reads the metadata from the given file.
	 *
	 * @param filePath Path of the metadata file to be created/read.
	 * @throws IOException
	 */
	public MetadataHandler(String filePath, Storage storage, boolean forceDiscover) throws IOException {
		this.storage = storage;
		if (StringUtils.isEmpty(filePath)) {
			logger.warn("No filepath was given for Ladybug MetadataHandler. Continuing with default [" + DEFAULT_PATH + "]");
			filePath = DEFAULT_PATH;
		}
		metadataMap = new HashMap<>();
		metadataFile = new File(filePath);
		if (metadataFile.exists() && !forceDiscover) {
			logger.info("Metadata for ladybug already exists. Reading from file [" + metadataFile.getName() + "] ...");
			readFromFile();
		} else {
			buildFromDirectory(metadataFile.getParentFile(), true);
		}
	}

	/**
	 * Builds the metadata by searching through folders in the given directly.
	 *
	 * @param dir           Directory to be searched.
	 * @param searchSubDirs True, if subdirectories should also be searched. False, otherwise.
	 * @throws IOException
	 */
	private void buildFromDirectory(File dir, boolean searchSubDirs) throws IOException {
		if (dir == null || !dir.isDirectory())
			return;
		logger.info("Building from directory " + dir.getPath());
		for (File file : dir.listFiles()) {
			if (searchSubDirs && file.isDirectory())
				buildFromDirectory(file, searchSubDirs);

			if (file.isFile() && file.getName().endsWith(".xml")) {
				try {
					// Todo: Use input streams for memory.
					String xml = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
					Report report = Report.fromXml(xml);
					report.setStorage(this.storage);
					File newFile;
					int storageId;
					do {
						storageId = getNextStorageId();
						newFile = new File(file.getParent(), storageId + ".xml");
					} while (newFile.exists());
					Metadata metadata = Metadata.fromReport(report, storageId);
					file.renameTo(newFile);
					add(metadata, false);
				} catch (Exception e) {
				}
			}
		}

		save();
	}

	/**
	 * Reads metadata from metadataFile.
	 *
	 * @throws FileNotFoundException
	 */
	private void readFromFile() throws IOException {
		logger.info("Reading from file " + metadataFile.getPath());
		Scanner scanner = new Scanner(metadataFile);
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line.contains("<Metadata>")) {
				stringBuilder = new StringBuilder();
				stringBuilder.append(line);
			}
			while (stringBuilder.length() > 0 && scanner.hasNextLine()) {
				line = scanner.nextLine();
				stringBuilder.append(line);
				if (line.contains("</Metadata>")) {
					Metadata m = Metadata.fromXml(stringBuilder.toString());
					add(m, false);
					if (m.getStorageId() > lastStorageId)
						lastStorageId = m.getStorageId();
					stringBuilder = new StringBuilder();
				}
			}
		}
		scanner.close();
	}

	public int getNextStorageId() {
		return ++lastStorageId;
	}

	public Metadata getMetadata(String correlationId) {
		return metadataMap.get(correlationId);
	}

	public Metadata getMetadata(long storageId) {
		for (String s : metadataMap.keySet()) {
			Metadata m = metadataMap.get(s);
			if (m.storageId == storageId)
				return m;
		}
		return null;
	}

	/**
	 * Adds the given metadata. And then stores it right away.
	 *
	 * @param m metadata to be added.
	 * @throws IOException
	 */
	public void add(Metadata m) throws IOException {
		add(m, true);
	}

	/**
	 * Adds the given metadata. And saves it, depending on saveNow parameter.
	 *
	 * @param m       metadata to be added.
	 * @param saveNow True if metadata file should be written right away.
	 * @throws IOException
	 */
	public void add(Metadata m, boolean saveNow) throws IOException {
		if (m.path == null)
			m.path = "";
		metadataMap.put(m.correlationId, m);

		// TODO: Find a more optimal way!
		// The problem is xml is not suitable for big data, so can't append directly.
		if (saveNow)
			save();
	}

	public List<List<Object>> getAsListofObjects(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues, int metadataValueType) {
		if (metadataNames == null || metadataNames.size() == 0)
			return new ArrayList<>();

		if (maxNumberOfRecords < 0)
			maxNumberOfRecords = Integer.MAX_VALUE;
		List<Pattern> patterns = new ArrayList<>(metadataNames.size());
		for (int i = 0; i < metadataNames.size(); i++) {
			if (searchValues != null && StringUtils.isNotEmpty(searchValues.get(i))) {
				patterns.add(Pattern.compile(searchValues.get(i), Pattern.CASE_INSENSITIVE));
			} else {
				patterns.add(Pattern.compile(".*"));
			}
		}
		List<List<Object>> result = new ArrayList<List<Object>>(metadataMap.size());
		Iterator<String> iterator = metadataMap.keySet().iterator();
		while (iterator.hasNext() && result.size() < maxNumberOfRecords) {
			String correlationId = iterator.next();
			Metadata m = metadataMap.get(correlationId);

			boolean filterPassed = true;
			for (int i = 0; i < metadataNames.size() && filterPassed; i++) {
				filterPassed = m.fieldEquals(metadataNames.get(i), patterns.get(i));
			}
			if (filterPassed)
				result.add(m.toObjectList(metadataNames, metadataValueType));
		}
		return result;
	}

	public List<Integer> getStorageIds() {
		List<Integer> ids = new ArrayList<Integer>(metadataMap.size());
		for (String correlationId : metadataMap.keySet()) {
			ids.add(metadataMap.get(correlationId).getStorageId());
		}
		return ids;
	}

	public int getSize() {
		return metadataMap.size();
	}

	/**
	 * Saves the metadata list in memory to metadatafile.
	 *
	 * @throws IOException
	 */
	public void save() throws IOException {
		if (!metadataFile.exists()) {
			logger.info("Creating metadata file at location [" + metadataFile.getPath() + "]");
			metadataFile.getParentFile().mkdirs();
			metadataFile.createNewFile();
		}

		logger.debug("Saving the metadata to file [" + metadataFile.getName() + "]...");
		FileWriter writer = new FileWriter(metadataFile, false);
		writer.append("<MetadataList>\n");
		for (String correlationId : metadataMap.keySet()) {
			writer.append(metadataMap.get(correlationId).toXml());
		}
		writer.append("<MetadataList>\n");
		writer.close();
	}

	/**
	 * Removes the metadata for the given report.
	 *
	 * @param report Report to be removed.
	 * @throws IOException
	 */
	public void delete(Report report) throws IOException {
		metadataMap.remove(report.getCorrelationId());
		save();
	}

	/**
	 * Updates the metadata of a report.
	 *
	 * @param report Report that corresponds to the metadata to be updated.
	 * @throws IOException
	 */
	public void update(Report report) throws IOException {
		Metadata old = metadataMap.get(report.getCorrelationId());
		Metadata metadata = Metadata.fromReport(report, old.storageId);
		add(metadata);
	}
}