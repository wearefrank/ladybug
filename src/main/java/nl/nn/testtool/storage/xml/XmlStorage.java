/*
   Copyright 2020-2024 WeAreFrank!

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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jakarta.annotation.PostConstruct;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.LinkMethodType;
import nl.nn.testtool.Report;
import nl.nn.testtool.StubType;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.SearchUtil;
import nl.nn.testtool.util.XmlUtil;

/**
 * Handles report storage for ladybug.
 * Stores reports in xml format.
 */
public class XmlStorage implements CrudStorage {
	public static final String FILE_EXTENSION = ".report.xml";
	private String name, metadataFile, reportsFolderPath;
	private MetadataHandler metadataHandler;
	private File reportsFolder;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Initializes the storage. Creating necessary folders and metadata file.
	 *
	 * @throws StorageException ...
	 */
	@PostConstruct
	public void init() throws StorageException {
		if (StringUtils.isEmpty(reportsFolderPath))
			throw new StorageException("Report folder path is empty. Please provide a path.");

		reportsFolder = new File(reportsFolderPath);

		if (StringUtils.isEmpty(metadataFile)) {
			metadataFile = new File(reportsFolder, "metadata.xml").getAbsolutePath();
			log.warn("Metadatafile was not set. Using " + metadataFile);
		}
		try {
			metadataHandler = new MetadataHandler(metadataFile, this, false);
		} catch (IOException e) {
			throw new StorageException("Could not initialize xml storage", e);
		}
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
			if (!reportFile.exists() || XmlUtil.isJavaBeansXml(reportFile)) {
				if (addNew || report.getStorageId() == null || metadataHandler.contains(report.getStorageId())) {
					int storageId = metadataHandler.getNextStorageId();
					while (metadataHandler.contains(storageId))
						storageId = metadataHandler.getNextStorageId();
					report.setStorageId(storageId);
				}
				store(report, reportFile);
				metadata = Metadata.fromReport(report, reportFile, reportsFolder);
				metadataHandler.add(metadata);
			} else {
				throw new StorageException("Storing human editable report xml file not supported yet");
			}
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
		Report report = readReportFromFile(reportFile, metadataHandler);
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
			if (!file.exists() || XmlUtil.isJavaBeansXml(file)) {
				if (!file.delete())
					throw new StorageException("Could not delete report with storage id [" + report.getStorageId() + "] correlation id [" + report.getCorrelationId() + "] at [" + path + "]");
				// Delete all parent folders which are empty.
				file = file.getParentFile();
				while (file.delete())
					file = file.getParentFile();
				metadataHandler.delete(report);
			} else {
				throw new StorageException("Deleting human editable report xml file not supported yet");
			}
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
	protected Report readReportFromFile(File file, MetadataHandler metadataHandler) throws StorageException {
		if (file == null || !file.isFile() || !file.getName().endsWith(XmlStorage.FILE_EXTENSION)) return null;
		if (XmlUtil.isJavaBeansXml(file)) {
			log.debug("Read java bean report xml file: " + file.getPath());
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
		} else {
			log.debug("Read human editable report xml file: " + file.getPath());
			Node reportXml;
			try {
				reportXml = XmlUtil.fileToNode(file);
			} catch (SAXException | IOException | ParserConfigurationException e) {
				throw new StorageException("Exception while reading human editable report xml file.", e);
			}
			Report report = new Report();
			report.setStorage(this);
			NamedNodeMap attributes = reportXml.getAttributes();
			if (attributes != null) {
				Node nameNode = attributes.getNamedItem("Name");
				if (nameNode != null) {
					report.setName(nameNode.getTextContent());
				}
				Node descriptionNode = attributes.getNamedItem("Description");
				if (descriptionNode != null) {
					report.setDescription(descriptionNode.getTextContent());
				}
				Node stubStrategyNode = attributes.getNamedItem("StubStrategy");
				if (stubStrategyNode != null) {
					report.setStubStrategy(stubStrategyNode.getTextContent());
				}
				Node linkMethodNode = attributes.getNamedItem("LinkMethod");
				if (linkMethodNode != null) {
					report.setLinkMethod(linkMethodNode.getTextContent());
				} else {
					report.setLinkMethod(LinkMethodType.NTH_NAME_AND_TYPE.toString());
				}
			}
			// XmlStorage/MetadataHandler has been built with the idea that the storageId can be stored in the report
			// and saved to disk and retrieved from the report when read from disk again but for now the following seems
			// to work.
			report.setStorageId(metadataHandler.getNextStorageId());
			report.setName(file.getName().substring(0, file.getName().length() - FILE_EXTENSION.length()));
			List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
			int level = 0;
			for (int i = 0; i < reportXml.getChildNodes().getLength(); i++) {
				Node reportChildNode = reportXml.getChildNodes().item(i);
				if ("Checkpoint".equals(reportChildNode.getNodeName())) {
					attributes = reportChildNode.getAttributes();
					if (attributes != null) {
						Node nameNode = attributes.getNamedItem("Name");
						if (nameNode != null) {
							String name = nameNode.getTextContent();
							int type = -1;
							Node typeNode = attributes.getNamedItem("Type");
							if (typeNode != null) {
								type = CheckpointType.valueOfString(typeNode.getTextContent()).toInt();
							}
							if (type != -1) {
								Checkpoint checkpoint = new Checkpoint(null, null, null, name, type, level);
								checkpoint.setMessage(nodeContentToString(reportChildNode));
								checkpoint.setReport(report);
								checkpoints.add(checkpoint);
								if (type == CheckpointType.STARTPOINT.toInt()) {
									level++;
								} else if (type == CheckpointType.ENDPOINT.toInt()
										|| type == CheckpointType.ABORTPOINT.toInt()) {
									level--;
								}
								Node stubNode = attributes.getNamedItem("Stub");
								if (stubNode != null) {
									checkpoint.setStub(StubType.valueOfString(stubNode.getTextContent()).toInt());
								}
							}
						}
					}
				} else if ("Transformation".equals(reportChildNode.getNodeName())) {
					report.setTransformation(nodeContentToString(reportChildNode));
				}
			}
			report.setCheckpoints(checkpoints);
			return report;
		}
	}

	private String nodeContentToString(Node node) {
		StringBuffer message = new StringBuffer();
		NodeList nodes = node.getChildNodes();
		for (int j = 0; j < nodes.getLength(); j++) {
			// Ignore whitespace at the beginning and end
			if ((j > 0 && j < nodes.getLength() - 1) || isNonWhiteSpace(nodes.item(j))) {
				message.append(XmlUtil.nodeToString(nodes.item(j)));
			}
		}
		return message.toString();
	}

	private boolean isNonWhiteSpace(Node node) {
		return !(node.getNodeType() == Node.TEXT_NODE && StringUtils.isBlank(node.getNodeValue()));
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
