package nl.nn.testtool.storage.xml;

import nl.nn.testtool.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Handles metadata for {@link XmlStorage}.
 */
public class MetadataHandler {
	private final String defaultFilePath = "metadata.xml";
	Set<Metadata> metadataSet;
	File metadataFile;
	long lastStorageId;
	Logger logger = LogUtil.getLogger(this.getClass());

	/**
	 * Creates a new file with the given path.
	 * If file exists, then reads the metadata from the given file.
	 * @param filePath Path of the metadata file to be created/read.
	 * @throws IOException
	 */
	public MetadataHandler(String filePath) throws IOException {
		if (StringUtils.isEmpty(filePath)) {
			logger.warn("No filepath was given for Ladybug MetadataHandler. Continuing with default [" + defaultFilePath + "]");
			filePath = defaultFilePath;
		}
		metadataSet = new HashSet<Metadata>();
		metadataFile = new File(filePath);
		if (!metadataFile.createNewFile()) {
			logger.info("Metadata for ladybug already exists. Reading from file [" + metadataFile.getName() + "] ...");
			readFromFile();
		}
	}

	/**
	 * Reads metadata from metadataFile.
	 * @throws FileNotFoundException
	 */
	private void readFromFile() throws FileNotFoundException {
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
					metadataSet.add(m);
					if(m.getStorageId() > lastStorageId)
						lastStorageId = m.getStorageId();
					stringBuilder = new StringBuilder();
				}
			}
		}
		scanner.close();
	}

	public long getNextStorageId() {
		return ++lastStorageId;
	}

	/**
	 * Adds the given metadata. And saves it to storage right away.
	 * @param m metadata to be added.
	 * @throws IOException
	 */
	public void add(Metadata m) throws IOException {
		metadataSet.add(m);
		// TODO: Find a more optimal way!
		// The problem is xml is not suitable for big data, so can't append directly.
		save();
	}

	public List<List<Object>> getAsListofObjects() {
		List<List<Object>> result = new ArrayList<List<Object>>(metadataSet.size());
		for (Metadata m : metadataSet) {
			result.add(m.toObjectList());
		}
		return result;
	}

	public List<Long> getStorageIds() {
		List<Long> ids = new ArrayList<Long>(metadataSet.size());
		for (Metadata m : metadataSet) {
			ids.add(m.getStorageId());
		}
		return ids;
	}

	public int getSize() {
		return metadataSet.size();
	}

	/**
	 * Saves the metadata list in memory to metadatafile.
	 * @throws IOException
	 */
	public void save() throws IOException {
		logger.info("Saving the metadata to file [" + metadataFile.getName() + "]...");
		FileWriter writer = new FileWriter(metadataFile, false);
		writer.append("<MetadataList>\n");
		for (Metadata m : metadataSet) {
			writer.append(m.toXml());
		}
		writer.append("<MetadataList>\n");
		writer.close();
	}
}
