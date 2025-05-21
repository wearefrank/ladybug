/*
   Copyright 2020-2025 WeAreFrank!

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
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.LinkMethodType;
import nl.nn.testtool.Report;
import nl.nn.testtool.StubType;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.memory.MemoryCrudStorage;
import nl.nn.testtool.util.XmlUtil;

/**
 * Store reports in xml format on the file system.
 */
public class XmlStorage extends MemoryCrudStorage {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String FILE_EXTENSION = ".report.xml";
	private @Setter String reportsFolder;
	private List<Long> lastModifieds = new ArrayList<>();

	@Override
	public Integer getInitialStorageId() {
		if (initialStorageId == null) {
			// Reserve lower storage id numbers for human editable report xml (e.g. a user can use a variable
			// ${checkpoint(287#13)} in report A and attribute StorageId="287" in report B to use data in report A from
			// checkpoint number 13 of report B)
			return 1000000;
		} else {
			return initialStorageId;
		}
	}

	@PostConstruct
	public void init() throws StorageException {
		if (reportsFolder == null) {
			throw new StorageException("Reports folder not set");
		}
		if (!new File(reportsFolder).isDirectory()) {
			throw new StorageException("Report folder is not a directory: " + reportsFolder);
		}
	}

	@Override
	public void store(Report report) throws StorageException {
		report.setStorage(this);
		report.setStorageId(getNewStorageId());
		String name = report.getName().replaceAll("[<>:\"\\/\\\\\\|\\?\\*]", "_");
		report.setName(name);
		int i = 2;
		while (getFile(report).isFile()) {
			report.setName(name + " (" + (i++) + ")");
		}
		store(report, getFile(report));
	}

	@Override
	public synchronized Report getReport(Integer storageId) throws StorageException {
		getReports();
		return super.getReport(storageId);
	}

	@Override
	public void update(Report report) throws StorageException {
		delete(report);
		store(report, getFile(report));
	}

	@Override
	public void delete(Report report) throws StorageException {
		File file = getFile(report);
		if (file.exists() && !XmlUtil.isJavaBeansXml(file)) {
			throw new StorageException("Deleting human editable report xml file not supported yet");
		}
		// Delete file
		if (!file.delete()) {
			throw new StorageException("Could not delete report with storage id [" + report.getStorageId() + "] correlation id [" + report.getCorrelationId() + "] at [" + file + "]");
		}
		// Delete all parent folders which are empty.
		file = file.getParentFile();
		while (file.delete()) {
			file = file.getParentFile();
		}
	}

	@Override
	public synchronized List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames,
			List<String> searchValues, int metadataValueType) throws StorageException {
		getReports();
		return super.getMetadata(maxNumberOfRecords, metadataNames, searchValues, metadataValueType);
	}

	@Override
	public void clear() throws StorageException {
		List<Integer> storageIds = getStorageIds();
		for (Integer storageId : storageIds) {
			delete(getReport(storageId));
		}
	}

	private File getFile(Report report) {
		File folder;
		if (report.getPath() == null) {
			folder = new File(reportsFolder);
		} else {
			folder = new File(reportsFolder, report.getPath());
		}
		return new File(folder, report.getName() + FILE_EXTENSION);
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
		if (file.exists() && !XmlUtil.isJavaBeansXml(file)) {
			throw new StorageException("Storing human editable report xml file not supported yet");
		}
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

	protected synchronized Map<Integer, Report> getReports() throws StorageException {
		List<Long> lastModifiedsLatest = new ArrayList<Long>();
		readReports(new File(reportsFolder), null, storageIds, lastModifiedsLatest, this);
		if (lastModifieds == null || !lastModifieds.equals(lastModifiedsLatest)) {
			log.debug("Read reports from reports folder: " + reportsFolder);
			super.reset();
			lastModifieds = new ArrayList<Long>();
			readReports(new File(reportsFolder), reports, storageIds, lastModifieds, this);
		} else {
			log.debug("Use cached reports");
		}
		return reports;
	}

	private static void readReports(File dir, Map<Integer, Report> reports, List<Integer> storageIds,
			List<Long> lastModifieds, Storage storage) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				readReports(file, reports, storageIds, lastModifieds, storage);
			}
			if (file.isFile() && file.getName().endsWith(XmlStorage.FILE_EXTENSION)) {
				if (lastModifieds != null) {
					lastModifieds.add(file.lastModified());
				}
				if (reports != null) {
					try {
						Report report = readReportFromFile(file, storage);
						if (report.getStorageId() == null) {
							// Can be null for human editable report xml file, prevent npe's in code using getStorageId()
							int newStorageId = 0;
							for (Integer storageId : storageIds) {
								if (storageId < newStorageId) {
									newStorageId = storageId;
								}
							}
							newStorageId--;
							report.setStorageId(newStorageId);
						}
						reports.put(report.getStorageId(), report);
						storageIds.add(report.getStorageId());
					} catch (StorageException exception) {
						log.warn("Exception while reading report [" + file.getPath() + "] during build from directory.");
					}
				}
			}
		}
	}

	/**
	 * Reads the report from the given file.
	 *
	 * @param file File to be read from.
	 * @param storage Storage to associate with the report.
	 * @throws StorageException ...
	 * @return Report generated from the given file.
	 */
	protected static Report readReportFromFile(File file, Storage storage) throws StorageException {
		if (file == null || !file.isFile() || !file.getName().endsWith(XmlStorage.FILE_EXTENSION)) return null;
		if (XmlUtil.isJavaBeansXml(file)) {
			log.debug("Read java bean report xml file: " + file.getPath());
			FileInputStream inputStream = null;
			XMLDecoder decoder = null;
			try {
				inputStream = new FileInputStream(file);
				decoder = new XMLDecoder(new BufferedInputStream(inputStream));
				Report report = (Report)decoder.readObject();
				report.setStorage(storage);
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
			report.setStorage(storage);
			NamedNodeMap attributes = reportXml.getAttributes();
			if (attributes != null) {
				Node storageIdNode = attributes.getNamedItem("StorageId");
				if (storageIdNode != null) {
					report.setStorageId(Integer.valueOf(storageIdNode.getTextContent()));
				}
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

	private static String nodeContentToString(Node node) {
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

	private static boolean isNonWhiteSpace(Node node) {
		return !(node.getNodeType() == Node.TEXT_NODE && StringUtils.isBlank(node.getNodeValue()));
	}

}
