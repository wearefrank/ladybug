/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.echo2.test.TestComponent;
import org.wearefrank.ladybug.echo2.util.Upload;
import org.wearefrank.ladybug.extensions.CustomReportAction;
import org.wearefrank.ladybug.extensions.CustomReportActionResult;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.memory.MemoryCrudStorage;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.wearefrank.ladybug.util.Export;
import org.wearefrank.ladybug.util.ExportResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ReportApiImpl {
	public static final class AttachmentBeingRead {
		public AttachmentBeingRead(String filename, InputStream in) {
			this.filename = filename;
			this.in = in;
			this.error = null;
		}

		public AttachmentBeingRead(String filename, InputStream in, IOException error) {
			this.filename = filename;
			this.in = in;
			this.error = error;
		}

		@Getter
		private String filename;

		@Getter
		private InputStream in;

		@Getter
		private IOException error;
	}

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter
	@Inject
	@Autowired TestTool testTool;
	private @Setter
	@Inject
	@Autowired ReportXmlTransformer reportXmlTransformer;
	private @Setter
	@Inject
	@Autowired Views views;
	private @Setter
	@Inject
	@Autowired Optional<CustomReportAction> customReportAction;

	public Map<String, Object> getReport(String storageName,
										 int storageId,
										 boolean xml,
										 boolean globalTransformer) throws HttpNotFoundException {
		Storage storage = testTool.getStorage(storageName);
		Report report = null;
		try {
			report = getReport(storage, storageId);
		} catch(Exception e) {
			throw new HttpNotFoundException(e);
		}
		if (report == null)
			throw new HttpNotFoundException("Could not find report with id [" + storageId + "]");

		if (globalTransformer) {
			if (reportXmlTransformer != null)
				report.setGlobalReportXmlTransformer(reportXmlTransformer);
		}

		HashMap<String, Object> map = new HashMap<>(1);
		map.put("report", report);
		map.put("xml", report.toXml());

		return map;
	}

	/**
	 * Get a list of uids for the checkpoints of a specific report. Currently only implementing getting the uids of the
	 * checkpoints to show or hide according to a specific view (trying to apply the best practice that path params are
	 * used to identify a specific resource or resources, while query parameters are used to sort/filter those resources
	 * (see https://stackoverflow.com/questions/30967822/when-do-i-use-path-params-vs-query-params-in-a-restful-api)
	 *
	 * @param storageName ...
	 * @param storageId   ...
	 * @param viewName    name of the view that determines which checkpoints to show/return and which to hide/exclude
	 * @param invert      when true return the checkpoints to hide and exclude the checkpoint to show
	 * @return ...
	 */
	public List<String> getCheckpointUids(String storageName,
										  int storageId,
										  String viewName,
										  boolean invert
	) throws HttpNotFoundException {
		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				throw new HttpNotFoundException("Could not find report with id [" + storageId + "]");
			List<String> response = new ArrayList<String>();
			for (View view : views) {
				if (view.getName().equals(viewName)) {
					for (Checkpoint checkpoint : report.getCheckpoints()) {
						if (view.showCheckpoint(report, checkpoint)) {
							if (!invert) {
								response.add(checkpoint.getUid());
							}
						} else {
							if (invert) {
								response.add(checkpoint.getUid());
							}
						}
					}
					break;
				}
			}
			return response;
		} catch (Exception e) {
			throw new HttpNotFoundException("Exception while getting report [" + storageId + "] from storage [" + storageName + "]", e);
		}
	}

	public Map<Integer, Map<String, Object>> getReports(String storageName,
														List<Integer> storageIds,
														boolean xml,
														boolean globalTransformer) throws HttpNotFoundException {
		try {
			Storage storage = testTool.getStorage(storageName);
			Map<Integer, Map<String, Object>> map = new HashMap<>();

			for (int storageId : storageIds) {
				Report report = getReport(storage, storageId);
				if (report == null)
					throw new HttpNotFoundException("Could not find report with id [" + storageId + "]");

				if (globalTransformer) {
					if (reportXmlTransformer != null)
						report.setGlobalReportXmlTransformer(reportXmlTransformer);
				}

				Map<String, Object> reportMap = new HashMap<>(1);
				reportMap.put("report", report);
				reportMap.put("xml", report.toXml());

				map.put(storageId, reportMap);
			}

			return map;

		} catch (Exception e) {
			throw new HttpNotFoundException("Exception while getting report [" + storageIds + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public void deleteReport(String storageName, List<Integer> storageIds) throws HttpNotFoundException, HttpNotImplementedException, HttpInternalServerErrorException {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageName + "] does not implement delete function.";
			log.warn(msg);
			throw new HttpNotImplementedException(msg);
		}
		List<String> errorMessages = new ArrayList<>();
		for (int storageId : storageIds) {
			try {
				Report report = getReport(storage, storageId);
				if (report == null)
					throw new HttpNotFoundException("Could not find report with storage id [" + storageId + "]");
				((CrudStorage) storage).delete(report);
			} catch (StorageException e) {
				errorMessages.add("Could not delete report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new HttpInternalServerErrorException(errorMessages.stream().collect(Collectors.joining("\n")));
		}
	}

	public void deleteAllReports(String storageName) throws HttpInternalServerErrorException {
		Storage storage = testTool.getStorage(storageName);
		List<String> errorMessages = new ArrayList<>();
		try {
			storage.clear();
		} catch (StorageException e) {
			errorMessages.add(String.format("Could not clear storage [%s], reason: %s", storage.getName(), e.getMessage()));
			log.error("Failed to clear storage [{}]", storage.getName(), e);
		}
		if (!errorMessages.isEmpty()) {
			throw new HttpInternalServerErrorException(errorMessages.stream().collect(Collectors.joining("\n")));
		}
	}

	public List<Report> getLatestReports(String storageName, int number) throws HttpBadRequestException, HttpInternalServerErrorException {
		try {
			Storage storage = testTool.getStorage(storageName);
			List<List<Object>> metadata = storage.getMetadata(-1, Arrays.asList("storageId", "endTime"),
					Arrays.asList(null, null), MetadataExtractor.VALUE_TYPE_OBJECT);
			int amount = Math.min(metadata.size(), number);
			if (amount < 1)
				throw new HttpBadRequestException("Either the number of reports requested [" + number + "] and/or the size of reports available [" + metadata.size() + "] is 0");

			metadata.sort(Comparator.comparingLong(o -> (Long) o.get(1)));
			ArrayList<Report> reports = new ArrayList<>(amount);
			for (int i = 1; i <= amount; i++) {
				reports.add(getReport(storage, (Integer) metadata.get(metadata.size() - i).get(0)));
			}
			return reports;
		} catch (StorageException e) {
			throw new HttpInternalServerErrorException("Could not retrieve latest [" + number + "] reports - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public Map<String, Serializable> updateReport(String storageName, int storageId, Map<String, String> map) throws HttpBadRequestException, HttpNotFoundException, HttpInternalServerErrorException {
		String[] fields = new String[]{"name", "path", "variables", "description", "transformation", "checkpointId", "checkpointMessage", "stub", "stubStrategy"};
		if (map.isEmpty() || !Util.mapContainsOnly(map, null, fields))
			throw new HttpBadRequestException("No new values or incorrect values have been given for report with storageId [" + storageId + "] - detailed error message - Values given are:\n" + map);

		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null) {
				throw new HttpNotFoundException("Could not find report with storageId ["
						+ storageId + "]");
			}

			if (map.containsKey("name")) report.setName(map.get("name"));
			if (map.containsKey("path")) report.setPath(TestComponent.normalizePath(map.get("path")));
			if (map.containsKey("description")) report.setDescription(map.get("description"));
			if (map.containsKey("transformation")) report.setTransformation(map.get("transformation"));
			if (map.containsKey("stubStrategy")) report.setStubStrategy(map.get("stubStrategy"));

			if (map.containsKey("variables")) {
				String variablesJson = map.get("variables");
				Map<String, String> variablesMap = new HashMap<>();
				if (variablesJson != null && !variablesJson.isEmpty()) {
					ObjectMapper mapper = new ObjectMapper();
					variablesMap = mapper.readValue(variablesJson, new TypeReference<Map<String, String>>() {
					});
				}
				if (variablesMap.size() > 0) {
					report.setVariables(variablesMap);
				} else {
					report.setVariables(null);
				}
			}

			if (map.containsKey("checkpointId")) {
				if (StringUtils.isNotEmpty(map.get("stub"))) {
					report.getCheckpoints().get(Integer.parseInt(map.get("checkpointId"))).setStub(Integer.parseInt(map.get("stub")));
				} else {
					report.getCheckpoints().get(Integer.parseInt(map.get("checkpointId"))).setMessage(map.get("checkpointMessage"));
				}
			}

			report.flushCachedXml();
			boolean storageUpdated = false;
			if (storage instanceof CrudStorage) {
				CrudStorage crudStorage = (CrudStorage) storage;
				crudStorage.update(report);
				storageUpdated = true;
			} else {
				if (reportXmlTransformer != null)
					report.setGlobalReportXmlTransformer(reportXmlTransformer);
			}

			HashMap<String, Serializable> result = new HashMap<>(3);
			result.put("xml", report.toXml());
			result.put("storageUpdated", storageUpdated);
			result.put("report", report);
			return result;
		} catch (StorageException | JsonProcessingException e) {
			throw new HttpInternalServerErrorException("Could not update report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public Map<String, String> getReportTransformation(String storageName, int storageId) throws HttpInternalServerErrorException {
		try {
			Storage storage = testTool.getStorage(storageName);
			String transformation = getReport(storage, storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return map;
		} catch (StorageException e) {
			throw new HttpInternalServerErrorException("Could not retrieve transformation of report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public List<Report> copyReport(String storageName, Map<String, List<Integer>> sources) throws HttpBadRequestException {
		Storage target = testTool.getStorage(storageName);
		Map<String, String> exceptions = new HashMap<>();
		ArrayList<Report> reports = new ArrayList<>();
		for (String src : sources.keySet()) {
			Storage srcStorage = testTool.getStorage(src);

			for (int storageId : sources.get(src)) {
				try {
					Report report = getReport(srcStorage, storageId);
					((CrudStorage) target).store(report);
					reports.add(report);
				} catch (Exception exception) {
					exceptions.put(src + "_" + storageId, Arrays.toString(exception.getStackTrace()));
					log.error("Could not copy the report. #Exceptions for request: " + exceptions, exception);
				}
			}
		}
		// TODO: Find a better error response code.
		if (exceptions.size() > 0)
			throw new HttpBadRequestException("Exceptions have been thrown when trying to copy report - detailed error message - Exceptions:\n" + exceptions);
		return reports;
	}

	public void uploadFile(String storageName, Supplier<AttachmentBeingRead> supplier) throws HttpInternalServerErrorException, HttpBadRequestException {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			throw new HttpInternalServerErrorException("Given storage [" + storage.getName() + "] is not a Crud Storage. Therefore no reports can be added externally.");
		}
		CrudStorage crudStorage = (CrudStorage) storage;
		AttachmentBeingRead attachmentBeingRead = supplier.get();
		String errorMessage = null;
		if (attachmentBeingRead.getError() == null) {
			errorMessage = Upload.upload(attachmentBeingRead.getFilename(), attachmentBeingRead.getIn(), crudStorage, log);
		} else {
			errorMessage = attachmentBeingRead.getError().getMessage();
		}
		if (!StringUtils.isEmpty(errorMessage)) {
			throw new HttpBadRequestException(errorMessage);
		}
	}

	public List<Report> getFileReport(Supplier<AttachmentBeingRead> supplier) throws HttpBadRequestException, HttpInternalServerErrorException {
		CrudStorage storage = new MemoryCrudStorage();
		AttachmentBeingRead attachmentBeingRead = supplier.get();
		String errorMessage = Upload.upload(attachmentBeingRead.filename, attachmentBeingRead.in, storage, log);
		if (StringUtils.isNotEmpty(errorMessage))
			throw new HttpBadRequestException(errorMessage);
		try {
			Iterator<Integer> storageIdsIterator = storage.getStorageIds().iterator();
			List<Report> reports = new ArrayList<>(storage.getStorageIds().size());
			while (storageIdsIterator.hasNext()) {
				Report report = getReport(storage, ((Integer) storageIdsIterator.next()));
				reports.add(report);
			}
			return reports;
		} catch (StorageException e) {
			throw new HttpInternalServerErrorException("Could not retrieve parsed reports from in-memory storage - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public ExportResult downloadFile(String storageName, String exportReportParam, String exportReportXmlParam, List<Integer> storageIds) throws HttpBadRequestException, HttpInternalServerErrorException {
		Storage storage = testTool.getStorage(storageName);
		if (storageIds == null || storageIds.isEmpty())
			throw new HttpBadRequestException("No storage ids have been provided");
		boolean exportReport = exportReportParam.equalsIgnoreCase("true") || exportReportParam.equals("1");
		boolean exportReportXml = exportReportXmlParam.equalsIgnoreCase("true") || exportReportXmlParam.equals("1");
		try {
			ExportResult export;
			if (storageIds.size() == 1) {
				Report report = getReport(storage, storageIds.get(0));
				export = Export.export(report, exportReport, exportReportXml);
			} else {
				export = Export.export(storage, storageIds, exportReport, exportReportXml);
			}
			return export;
		} catch (StorageException e) {
			throw new HttpInternalServerErrorException("Exception while requesting reports with ids [" + storageIds + "] from the storage. - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	/**
	 * Copy or move report files in the same storage to different paths.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage ids of the reports to be moved.
	 * @param map Map containing "path" and "action". Actions could be "copy" or "move".
	 */
	public void updatePath(String storageName, List<Integer> storageIds, Map<String, String> map) throws HttpBadRequestException, HttpInternalServerErrorException {
		CrudStorage storage = (CrudStorage) testTool.getStorage(storageName);
		String path = map.get("path");
		String action = map.get("action");
		if (StringUtils.isEmpty(action) || StringUtils.isEmpty(path))
			throw new HttpBadRequestException("[action] and [path] are both required in the request body.");

		for (int storageId : storageIds) {
			try {
				Report original = getReport(storage, storageId);
				if ("copy".equalsIgnoreCase(action)) {
					Report clone = original.clone();
					clone.setPath(path);
					storage.store(clone);
				} else if ("move".equalsIgnoreCase(action)) {
					original.setPath(path);
					storage.update(original);
				} else {
					throw new HttpBadRequestException("Action parameter can only be either [copy] or [move]");
				}
			} catch (StorageException e) {
				throw new HttpInternalServerErrorException("Storage exception with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
			} catch (CloneNotSupportedException e) {
				throw new HttpInternalServerErrorException("Cloning exception for report with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
			}
		}
	}

	public List<String> cloneReport(String storageName, int storageId, Map<String, String> map) throws HttpBadRequestException {
		CrudStorage storage = (CrudStorage) testTool.getStorage(storageName);
		Report original;
		try {
			original = getReport(storage, storageId);
			String previousMessage = original.getInputCheckpoint().getMessage();
			boolean force = "true".equalsIgnoreCase(map.getOrDefault("force", "false"))
					|| "1".equalsIgnoreCase(map.getOrDefault("force", "false"));
			original.getInputCheckpoint().setMessage(map.get("message"));
			if (!original.getInputCheckpoint().containsVariables() && !force) {
				original.getInputCheckpoint().setMessage(previousMessage);
				throw new HttpBadRequestException("No variables found in input message; press again to confirm");
			}
		} catch (StorageException e) {
			log.error("Exception while cloning the report", e);
			throw new HttpBadRequestException("Report could not be found. - detailed error message - " + e + Arrays.toString(e.getStackTrace()), e);
		}

		Scanner scanner = new Scanner(map.get("csv"));
		String firstLine = null;
		boolean originalSet = false;
		ArrayList<String> exceptions = new ArrayList<>();
		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if (StringUtils.isEmpty(nextLine) && nextLine.startsWith("#"))
				continue;
			if (firstLine == null) {
				firstLine = nextLine;
			} else {
				try {
					if (originalSet) {
						Report clone = original.clone();
						clone.setVariablesCsv(firstLine + "\n" + nextLine);
						storage.store(clone);
					} else {
						originalSet = true;
						original.setVariablesCsv(firstLine + "\n" + nextLine);
						storage.update(original);
					}
				} catch (CloneNotSupportedException | StorageException e) {
					exceptions.add(e.getMessage() + " Caused by: " + e.getCause() + " For line: [" + nextLine + "]");
				}
			}
		}
		scanner.close();
		return exceptions;
	}

	/**
	 * Returns the report and sets the testTool bean on the report.
	 *
	 * @param storage   Storage to get the report from.
	 * @param storageId Storage id of the report.
	 * @return Report.
	 * @throws StorageException ...
	 */
	public Report getReport(Storage storage, Integer storageId) throws StorageException {
		Report report = storage.getReport(storageId);
		if (report != null) report.setTestTool(testTool);
		return report;
	}

	public String getWarningsAndErrors(String storageName) {
		Storage rawStorage = testTool.getStorage(storageName);
		if (! (rawStorage instanceof LogStorage)) {
			return null;
		}
		LogStorage storage = (LogStorage) rawStorage;
		return storage.getWarningsAndErrors();
	}

	public Map<String, String> processCustomReportAction(String storageName, List<Integer> reportIds) throws HttpNotFoundException, HttpBadRequestException, HttpInternalServerErrorException {
		Storage storage = testTool.getStorage(storageName);
		List<Report> reports = new ArrayList<>();
		for (int storageId : reportIds) {
			try {
				Report report = getReport(storage, storageId);
				if (report == null)
					throw new HttpNotFoundException("Could not find report with storage id [" + storageId + "]");
				reports.add(report);
			} catch (StorageException e) {
				e.printStackTrace();
				throw new HttpInternalServerErrorException(e);
			}
		}
		if (customReportAction == null) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "No custom report action defined.");
			throw new HttpBadRequestException(errorResponse.toString());
		}
		CustomReportActionResult customReportActionResult = customReportAction.get().handleReports(reports);
		Map<String, String> response = new HashMap<>();
		response.put("success", customReportActionResult.getSuccessMessage());
		response.put("error", customReportActionResult.getErrorMessage());
		return response;
	}

	public Map<String, String> fetchVariables() {
		Map<String, String> variables = new HashMap<>();
		String buttonText = (customReportAction.orElse(null) != null) ? customReportAction.get().getButtonText() : null;
		variables.put("customReportActionButtonText", buttonText);
		return variables;
	}
}