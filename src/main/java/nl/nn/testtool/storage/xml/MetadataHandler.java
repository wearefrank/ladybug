package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
import nl.nn.testtool.util.LogUtil;
import nl.nn.xmldecoder.XMLDecoder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles metadata for {@link XmlStorage}.
 */
public class MetadataHandler {
	private final String DEFAULT_PATH = "metadata.xml";
	private XmlStorage storage;
	private HashMap<String, Metadata> metadataMap;
	protected File metadataFile;
	private int lastStorageId = 1;
	private final Logger logger = LogUtil.getLogger(this.getClass());

	/**
	 * Creates a new file with the given path.
	 * If file exists, then reads the metadata from the given file.
	 *
	 * @param filePath Path of the metadata file to be created/read.
	 * @throws IOException
	 */
	public MetadataHandler(String filePath, XmlStorage storage, boolean forceDiscover) throws IOException {
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
			buildFromDirectory(metadataFile.getParentFile(), true, null);
		}
	}

	/**
	 * Builds the metadata by searching through folders in the given directly.
	 *
	 * @param dir           Directory to be searched.
	 * @param searchSubDirs True, if subdirectories should also be searched. False, otherwise.
	 * @param registeredIds Set of registered storage Ids to avoid collision.
	 * @throws IOException
	 */
	private void buildFromDirectory(File dir, boolean searchSubDirs, Set<Integer> registeredIds) throws IOException {
		if (dir == null || !dir.isDirectory())
			return;
		if (registeredIds == null)
			registeredIds = new HashSet<>();

		logger.info("Building from directory " + dir.getPath());
		for (File file : dir.listFiles()) {
			if (searchSubDirs && file.isDirectory())
				buildFromDirectory(file, searchSubDirs, registeredIds);

			if (file.isFile() && file.getName().endsWith(XmlStorage.FILE_EXTENSION)) {
				addFromFile(file, registeredIds);
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
		if (!metadataFile.exists())
			return;

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

		updateMetadata();
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
	private void add(Metadata m, boolean saveNow) throws IOException {
		if (StringUtils.isEmpty(m.path))
			m.path = "/";
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
				patterns.add(null);
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
	private void save() throws IOException {
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
	 * Updates the metadata from the directory containing the metadata file.
	 *
	 * @throws IOException
	 */
	public void updateMetadata() throws IOException {
		HashMap<String, Metadata> pathMap = new HashMap<>(metadataMap.size());
		Set<Integer> registeredIds = new HashSet<>(metadataMap.size());
		for (String c : metadataMap.keySet()) {
			Metadata m = metadataMap.get(c);
			String path = m.path + m.name + XmlStorage.FILE_EXTENSION;
			pathMap.put(path, m);
			registeredIds.add(m.storageId);
		}
		Set<String> updatedIds = updateMetadata(metadataFile.getParentFile(), pathMap, registeredIds);
		// Delete the remaining metadata.
		for (String p : pathMap.keySet()) {
			Metadata m = pathMap.get(p);
			if (!updatedIds.contains(m.correlationId)) {
				logger.info("Deleting metadata with correlation id [" + m.correlationId + "] and path [" + m.path + m.name + "]");
				metadataMap.remove(m.correlationId);
			}
		}
		save();
	}

	/**
	 * Updates the metadata from the given directory.
	 *
	 * @param dir           Directory to be searched.
	 * @param map           Map of old metadata with keys as paths.
	 * @param registeredIds Set of registered storage ids to avoid collision.
	 */
	private Set<String> updateMetadata(File dir, HashMap<String, Metadata> map, Set<Integer> registeredIds) {
		if (dir == null || !dir.isDirectory())
			return new HashSet<>();
		Set<String> ids = new HashSet<>();
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				ids.addAll(updateMetadata(file, map, registeredIds));
				continue;
			} else if (!file.getName().endsWith(XmlStorage.FILE_EXTENSION)) {
				continue;
			}
			String path = "/" + metadataFile.getParentFile().toPath().relativize(file.toPath()).toString().replaceAll("\\\\", "/"); // Relativize
			Metadata m = map.remove(path);
			if (m == null || m.lastModified < file.lastModified()) {
				ids.add(addFromFile(file, registeredIds).getCorrelationId());
			}
		}
		return ids;
	}

	/**
	 * Reads the report from the given file. Generates metadata and saves it.
	 *
	 * @param file         File to be read from.
	 * @param forbiddenIds Set of storage ids that should not be set for this report. Set to null for allowing all values.
	 */
	private Report addFromFile(File file, Set<Integer> forbiddenIds) {
		if (file == null || !file.isFile() || !file.getName().endsWith(XmlStorage.FILE_EXTENSION))
			return null;

		logger.debug("Adding from a new file: " + file.getPath());
		FileInputStream inputStream = null;
		XMLDecoder decoder = null;
		try {
			inputStream = new FileInputStream(file);
			decoder = new XMLDecoder(new BufferedInputStream(inputStream));

			Report report = (Report) decoder.readObject();
			report.setStorage(this.storage);

			String path = metadataFile.getParentFile().toPath().relativize(file.getParentFile().toPath()).toString() + "/";
			if (StringUtils.isNotEmpty(path)) {
				path = path.replaceAll("\\\\", "/");
				if (!path.endsWith("/"))
					path += "/";
			}
			if (!path.startsWith("/"))
				path = "/" + path;
			report.setPath(path);

			String filename = file.getName();
			report.setName(filename.substring(0, filename.length() - XmlStorage.FILE_EXTENSION.length()));

			int storageId = report.getStorageId() == 0 ? getNextStorageId() : report.getStorageId();
			if (forbiddenIds != null && metadataMap.containsKey(report.getCorrelationId()) && metadataMap.get(report.getCorrelationId()).storageId != report.getStorageId()) {
				while (forbiddenIds.contains(storageId))
					storageId = getNextStorageId();

				forbiddenIds.add(storageId);
			}
			if (storageId >= lastStorageId)
				lastStorageId = storageId + 1;

			report.setStorageId(storageId);

			storage.store(report, file);

			Metadata metadata = Metadata.fromReport(report, file.lastModified());
			add(metadata, false);
			return report;
		} catch (Exception e) {
			if (decoder != null)
				decoder.close();
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception ignored) {
				}
			}
		}
		return null;

	}
}
