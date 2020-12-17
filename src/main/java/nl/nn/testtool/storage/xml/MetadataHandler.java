/*
   Copyright 2020 WeAreFrank!

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

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.xmldecoder.XMLDecoder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
	private HashMap<Integer, Metadata> metadataMap;
	protected File metadataFile;
	private int lastStorageId = 1;
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Creates a new file with the given path.
	 * If file exists, then reads the metadata from the given file.
	 *
	 * @param filePath Path of the metadata file to be created/read.
	 * @param storage ...
	 * @param forceDiscover ...
	 * @throws IOException ...
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
			buildFromDirectory(storage.getReportsFolder(), true);
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
		// Discover reports and group them according to their original storage id.
		HashMap<Integer, HashMap<File, Report>> reports = new HashMap<>();
		for (File file : dir.listFiles()) {
			if (searchSubDirs && file.isDirectory())
				buildFromDirectory(file, true);

			if (file.isFile() && file.getName().endsWith(XmlStorage.FILE_EXTENSION)) {
				Report report = importFromFile(file, false, false);
				HashMap<File, Report> reportsForStorageId = reports.computeIfAbsent(report.getStorageId(), k -> new HashMap<>());
				reportsForStorageId.put(file, report);
			}
		}
		// For each storage id, add reports to metadataMap.
		// If there are multiple for same storage id, only one will keep it.
		// other reports will be given a storageId that does not conflict with any discovered reports.
		for (Integer storageId : reports.keySet()) {
			HashMap<File, Report> reportsForStorageId = reports.get(storageId);
			boolean originalStorageIdTaken = false;
			for (File file : reportsForStorageId.keySet()) {
				int targetStorageId = storageId;
				if (originalStorageIdTaken) {
					targetStorageId  = getNextStorageId();
					while (reports.containsKey(targetStorageId) || metadataMap.containsKey(targetStorageId)) {
						targetStorageId  = getNextStorageId();
					}
				}
				Report report = reportsForStorageId.get(file);
				report.setStorageId(targetStorageId);
				try {
					storage.store(report, file);

					Metadata metadata = Metadata.fromReport(report, file.lastModified());
					add(metadata, false);
				} catch (StorageException | IOException e) {
					logger.error("Error while updating metadata from file [" + file.getPath() + "]", e);
				}
				originalStorageIdTaken = true;
			}
		}

		save();
	}

	/**
	 * Reads metadata from metadataFile.
	 *
	 * @throws FileNotFoundException ...
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
	}

	public int getNextStorageId() {
		return ++lastStorageId;
	}

	public Metadata getMetadata(String correlationId) {
		if (StringUtils.isEmpty(correlationId))
			return null;
		for (Integer i : metadataMap.keySet()) {
			Metadata m = metadataMap.get(i);
			if (m != null && m.correlationId.equals(correlationId))
				return m;
		}
		return null;
	}

	public Metadata getMetadata(Integer storageId) {
		if (storageId == null)
			return null;
		return metadataMap.get(storageId);
	}

	public boolean contains(Integer storageId) {
		if (storageId == null)
			return false;
		return metadataMap.containsKey(storageId);
	}

	/**
	 * Adds the given metadata. And then stores it right away.
	 *
	 * @param m metadata to be added.
	 * @throws IOException ...
	 */
	public void add(Metadata m) throws IOException {
		add(m, true);
	}

	/**
	 * Adds the given metadata. And saves it, depending on saveNow parameter.
	 *
	 * @param m       metadata to be added.
	 * @param saveNow True if metadata file should be written right away.
	 * @throws IOException ...
	 */
	private void add(Metadata m, boolean saveNow) throws IOException {
		if (StringUtils.isEmpty(m.path))
			m.path = "/";
		metadataMap.put(m.storageId, m);
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
				String searchValue = searchValues.get(i);
				if ("path".equalsIgnoreCase(metadataNames.get(i))) {
					searchValue = "^" + searchValue.substring(1, searchValue.length() - 1)
							.replaceAll("/", "\\/")
							.replaceAll("\\*", ".*");
				}
				patterns.add(Pattern.compile(searchValue, Pattern.CASE_INSENSITIVE));
			} else {
				patterns.add(null);
			}
		}
		List<List<Object>> result = new ArrayList<List<Object>>(metadataMap.size());
		Iterator<Integer> iterator = metadataMap.keySet().iterator();
		while (iterator.hasNext() && result.size() < maxNumberOfRecords) {
			Integer storageId = iterator.next();
			Metadata m = metadataMap.get(storageId);

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
		return new ArrayList<>(metadataMap.keySet());
	}

	public int getSize() {
		return metadataMap.size();
	}

	/**
	 * Saves the metadata list in memory to metadatafile, if there is metadata to save.
	 *
	 * @throws IOException ...
	 */
	private void save() throws IOException {
		if (metadataMap == null || metadataMap.size() == 0) {
			if (metadataFile.exists())
				metadataFile.delete();
			return;
		}
		if (!metadataFile.exists()) {
			logger.info("Creating metadata file at location [" + metadataFile.getPath() + "]");
			metadataFile.getParentFile().mkdirs();
			metadataFile.createNewFile();
		}
		logger.debug("Saving the metadata to file [" + metadataFile.getName() + "]...");
		FileWriter writer = new FileWriter(metadataFile, false);
		writer.append("<MetadataList>\n");
		for (Integer storageId : metadataMap.keySet()) {
			writer.append(metadataMap.get(storageId).toXml());
		}
		writer.append("<MetadataList>\n");
		writer.close();
	}

	/**
	 * Removes the metadata for the given report.
	 *
	 * @param report Report to be removed.
	 * @throws IOException ...
	 */
	public void delete(Report report) throws IOException {
		metadataMap.remove(report.getStorageId());
		save();
	}

	/**
	 * Updates the metadata from the directory containing the metadata file.
	 *
	 * @throws IOException ...
	 */
	public void updateMetadata() throws IOException {
		HashMap<String, Metadata> pathMap = new HashMap<>(metadataMap.size());
		Set<Integer> duplicates = new HashSet<>();
		for (Integer c : metadataMap.keySet()) {
			Metadata m = metadataMap.get(c);
			String path = m.path + m.name + XmlStorage.FILE_EXTENSION;
			if (pathMap.containsKey(path)) {
				duplicates.add(m.storageId);
				duplicates.add(pathMap.get(path).storageId);
			}
			pathMap.put(path, m);
		}

		// Delete duplicates to make sure they are re-discovered.
		for (int storageId : duplicates) {
			Metadata m = metadataMap.remove(storageId);
			pathMap.remove(m.path + m.name + XmlStorage.FILE_EXTENSION);
		}

		Set<Integer> updatedIds = updateMetadata(storage.getReportsFolder(), pathMap, null);
		// Delete the remaining metadata.
		for (String p : pathMap.keySet()) {
			Metadata m = pathMap.get(p);
			if (!updatedIds.contains(m.storageId)) {
				logger.info("Deleting metadata with storage id [" + m.storageId + "] correlation id [" + m.correlationId + "] and path [" + m.path + m.name + "]");
				metadataMap.remove(m.storageId);
			}
		}
		save();
	}

	/**
	 * Updates the metadata from the given directory.
	 *
	 * @param dir Directory to be searched.
	 * @param map Map of old metadata with keys as paths.
	 */
	private Set<Integer> updateMetadata(File dir, HashMap<String, Metadata> map, Set<Integer> updatedIds) {
		if (updatedIds == null)
			updatedIds = new HashSet<>();

		if (dir == null || !dir.isDirectory())
			return updatedIds;

		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				updateMetadata(file, map, updatedIds);
				continue;
			} else if (!file.getName().endsWith(XmlStorage.FILE_EXTENSION)) {
				continue;
			}
			String path = "/" + storage.getReportsFolder().toPath().relativize(file.toPath()).toString().replaceAll("\\\\", "/"); // Relativize
			Metadata m = map.remove(path);
			if (m == null || m.lastModified < file.lastModified()) {
				Report report = importFromFile(file, true, true);
				try {
					storage.store(report, file);

					Metadata metadata = Metadata.fromReport(report, file.lastModified());
					add(metadata, false);
				} catch (StorageException | IOException e) {
					logger.error("Error while updating metadata from file [" + file.getPath() + "]", e);
				}
				if (report != null)
					updatedIds.add(report.getStorageId());
			}
		}
		return updatedIds;
	}

	/**
	 * Reads the report from the given file. Generates metadata and returns it.
	 *
	 * @param file File to be read from.
	 * @param update True, if the goal of import is to update. In this case file will be compared to the cached metadata.
	 * @param setMetadata True if report's storageId should be changed in case of conflicts.
	 * @return Report generated from the given file.
	 */
	private Report importFromFile(File file, boolean update, boolean setMetadata) {
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

			String path = storage.getReportsFolder().toPath().relativize(file.getParentFile().toPath()).toString() + "/";
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
			if (metadataMap.containsKey(storageId)) {
				if (update) {
					Report report1 = null;
					try {
						// Check if there's still a report in old storage id.
						report1 = storage.getReport(storageId);
					} catch (StorageException e) {
						logger.error("File could not be opened with storage id [" + storageId + "].", e);
					}

					if (report1 != null) {
						if (report1.getStorageId() == storageId) {
							while (metadataMap.containsKey(storageId))
								storageId = getNextStorageId();
						} else {
							// The metadata for this file is not up to date.
							File reportFile = new File(storage.resolvePath(storageId));

							Report report2 = importFromFile(reportFile, false, true);
							storage.store(report2, reportFile);

							Metadata metadata = Metadata.fromReport(report2, reportFile.lastModified());
							add(metadata, false);
						}
					}
				} else if (setMetadata) {
					while (metadataMap.containsKey(storageId))
						storageId = getNextStorageId();
				}
			}
			if (setMetadata) {
				if (storageId >= lastStorageId)
					lastStorageId = storageId + 1;

				report.setStorageId(storageId);
			}
			return report;
		} catch (Exception e) {
			logger.error("Exception during report deserialization.", e);
			if (decoder != null)
				decoder.close();
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception ignored) {
					logger.error("Could not close the xml file.", ignored);
				}
			}
		}
		return null;

	}
}
