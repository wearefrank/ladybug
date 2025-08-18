/*
   Copyright 2021-2025 WeAreFrank!

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
package nl.nn.testtool.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.annotation.MultipartConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.test.TestComponent;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.extensions.CustomReportAction;
import nl.nn.testtool.extensions.CustomReportActionResult;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.memory.MemoryCrudStorage;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/report")
@MultipartConfig
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class ReportApi extends ApiBase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;
	private @Setter @Inject @Autowired Views views;
	private @Setter @Inject @Autowired Optional<CustomReportAction> customReportAction;

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GetMapping(value = "/{storage}/{storageId}/", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReport(@PathVariable("storage") String storageName,
									   @PathVariable("storageId") int storageId,
									   @RequestParam(name = "xml", defaultValue = "false") boolean xml,
									   @RequestParam(name = "globalTransformer", defaultValue = "false") boolean globalTransformer) {
		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				return new ResponseEntity<>("Could not find report with id [" + storageId + "]", HttpStatus.NOT_FOUND);
			if (globalTransformer) {
				if (reportXmlTransformer != null)
					report.setGlobalReportXmlTransformer(reportXmlTransformer);
			}

			HashMap<String, Object> map = new HashMap<>(1);
			map.put("report", report);
			map.put("xml", report.toXml());

			return ResponseEntity.ok(map);

		} catch (Exception e) {
			return new ResponseEntity("Exception while getting report [" + storageId + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), HttpStatus.NOT_FOUND);
		}
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
	 * @return            ...
	 */
	@GetMapping(value = "/{storage}/{storageId}/checkpoints/uids", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getCheckpointUids(	@PathVariable("storage") String storageName,
										@PathVariable("storageId") int storageId,
										@RequestParam(name = "view") String viewName,
										@RequestParam(name = "invert") boolean invert
										) {
		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				return new ResponseEntity<>("Could not find report with id [" + storageId + "]", HttpStatus.NOT_FOUND);
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
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return new ResponseEntity("Exception while getting report [" + storageId + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Returns the reports for the given storage and ids.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GetMapping(value = "/{storage}/", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReports(@PathVariable("storage") String storageName,
							   @RequestParam(name = "storageIds") List<Integer> storageIds,
							   @RequestParam(name = "xml", defaultValue = "false") boolean xml,
							   @RequestParam(name = "globalTransformer", defaultValue = "false") boolean globalTransformer) {
		try {
			Storage storage = testTool.getStorage(storageName);
			HashMap<Integer, HashMap<String, Object>> map = new HashMap<>();

			for (int storageId : storageIds) {
				Report report = getReport(storage, storageId);
				if (report == null)
					return new ResponseEntity("Could not find report with id [" + storageId + "]", HttpStatus.NOT_FOUND);
				if (globalTransformer) {
					if (reportXmlTransformer != null)
						report.setGlobalReportXmlTransformer(reportXmlTransformer);
				}

				HashMap<String, Object> reportMap = new HashMap<>(1);
				reportMap.put("report", report);
				reportMap.put("xml", report.toXml());

				map.put(storageId, reportMap);
			}

			return ResponseEntity.ok(map);

		} catch (Exception e) {
			return new ResponseEntity<>("Exception while getting report [" + storageIds + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Deletes the report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds  Storage id's of the reports to delete
	 * @return "Ok" if deleted properly, "Not implemented" if storage does not support deletion, "Not found" if report does not exist.
	 */
	@DeleteMapping(value = "/{storage}")
	public ResponseEntity<?> deleteReport(@PathVariable("storage") String storageName, @RequestParam(name = "storageIds") List<Integer> storageIds) {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageName + "] does not implement delete function.";
			log.warn(msg);
			return new ResponseEntity(msg, HttpStatus.NOT_IMPLEMENTED);
		}
		List<String> errorMessages = new ArrayList<>();
		for (int storageId : storageIds) {
			try {
				Report report = getReport(storage, storageId);
				if (report == null)
					return new ResponseEntity<>("Could not find report with storage id [" + storageId + "]", HttpStatus.NOT_FOUND);
				((CrudStorage) storage).delete(report);
			} catch (StorageException e) {
				errorMessages.add("Could not delete report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
			}
		}
		if (!errorMessages.isEmpty()) {
			return ResponseEntity.internalServerError().body(errorMessages);
		}
		return ResponseEntity.ok().build();
	}

	@DeleteMapping(value = "/all/{storage}")
	public ResponseEntity<?> deleteAllReports(@PathVariable("storage") String storageName) {
		Storage storage = testTool.getStorage(storageName);
		List<String> errorMessages = new ArrayList<>();
		try {
			storage.clear();
		} catch(StorageException e) {
			errorMessages.add(String.format("Could not clear storage [%s], reason: %s", storage.getName(), e.getMessage()));
			log.error("Failed to clear storage [{}]", storage.getName(), e);
		}
		if (!errorMessages.isEmpty()) {
			return ResponseEntity.internalServerError().body(errorMessages);
		}
		return ResponseEntity.ok().build();
	}

	/**
	 * Get the n latest reports in the storage.
	 *
	 * @param storageName Name of the storage.
	 * @param number Number of latest reports to retrieve.
	 * @return the n latest reports.
	 */
	@GetMapping(value = "/latest/{storage}/{numberOfReports}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getLatestReports(@PathVariable("storage") String storageName, @PathVariable("numberOfReports") int number) {
		try {
			Storage storage = testTool.getStorage(storageName);
			List<List<Object>> metadata = storage.getMetadata(-1, Arrays.asList("storageId", "endTime"),
					Arrays.asList(null, null), MetadataExtractor.VALUE_TYPE_OBJECT);
			int amount = Math.min(metadata.size(), number);
			if (amount < 1)
				return ResponseEntity.badRequest().body("Either the number of reports requested [" + number + "] and/or the size of reports available [" + metadata.size() + "] is 0");
			metadata.sort(Comparator.comparingLong(o -> (Long) o.get(1)));
			ArrayList<Report> reports = new ArrayList<>(amount);
			for (int i = 1; i <= amount; i++) {
				reports.add(getReport(storage, (Integer) metadata.get(metadata.size() - i).get(0)));
			}
			return ResponseEntity.ok(reports);
		} catch (StorageException e) {
			return ResponseEntity.internalServerError().body("Could not retrieve latest [" + number + "] reports - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Update the report with the given values..
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param map Map containing ["name" or "path" or "variables" or "description" or "transformation" or "checkpointId and "checkpointMessage"].
	 * @return The updated report.
	 */
	@PostMapping(value = "/{storage}/{storageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReport(@PathVariable("storage") String storageName, @PathVariable("storageId") int storageId, Map<String, String> map) {
		String[] fields = new String[]{"name", "path", "variables", "description", "transformation", "checkpointId", "checkpointMessage", "stub", "stubStrategy"};
		if (map.isEmpty() || !mapContainsOnly(map, null, fields))
			return ResponseEntity.badRequest().body("No new values or incorrect values have been given for report with storageId [" + storageId + "] - detailed error message - Values given are:\n" + map);
		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				return new ResponseEntity("Could not find report with storageId [" + storageId + "]", HttpStatus.NOT_FOUND);
			report.setName(map.get("name"));
			report.setPath(TestComponent.normalizePath(map.get("path")));
			report.setDescription(map.get("description"));
			report.setTransformation(map.get("transformation"));
			report.setStubStrategy(map.get("stubStrategy"));

			String variablesJson = map.get("variables");
			Map<String, String> variablesMap = new HashMap<>();
			if (variablesJson != null && !variablesJson.isEmpty()) {
				ObjectMapper mapper = new ObjectMapper();
				variablesMap = mapper.readValue(variablesJson, new TypeReference<Map<String, String>>() { });
			}
			report.setVariables(variablesMap);

			if (StringUtils.isNotEmpty(map.get("checkpointId"))) {
				if (StringUtils.isNotEmpty(map.get("stub"))) {
					report.getCheckpoints().get(Integer.parseInt(map.get("checkpointId"))).setStub(Integer.parseInt(map.get("stub")));
				} else {
					report.getCheckpoints().get(Integer.parseInt(map.get("checkpointId"))).setMessage(map.get("checkpointMessage"));
				}
			}

			HashMap<String, Serializable> result = new HashMap<>(3);
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

			result.put("xml", report.toXml());
			result.put("storageUpdated", storageUpdated);
			result.put("report", report);
			return ResponseEntity.ok(result);
		} catch (StorageException | JsonProcessingException e) {
			return ResponseEntity.internalServerError().body("Could not apply transformation to report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Return transformation of a report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @return Response containing a map containing transformation.
	 */
	@GetMapping(value = "/transformation/{storage}/{storageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReportTransformation(@PathVariable("storage") String storageName, @PathVariable("storageId") int storageId) {
		try {
			Storage storage = testTool.getStorage(storageName);
			String transformation = getReport(storage, storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return ResponseEntity.ok(map);
		} catch (StorageException e) {
			return ResponseEntity.internalServerError().body("Could not retrieve transformation of report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Copy the reports from the given storages and ids to the given target storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param sources Map [String, Integer] where keys are storage names and integers are storage ids for the reports to be copied.
	 * @return The copied report.
	 */
	@PutMapping(value = "/store/{storage}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> copyReport(@PathVariable("storage") String storageName, @RequestBody Map<String, List<Integer>> sources) {
		Storage target = testTool.getStorage(storageName);
		Map<String, String> exceptions = new HashMap<>();
		ArrayList<Report> reports = new ArrayList<>();
		for (String src : sources.keySet()) {
			try {
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
			} catch (ApiException e) {
				exceptions.put(src, e.getMessage());
			}
		}
		// TODO: Find a better error response code.
		if (exceptions.size() > 0)
			return ResponseEntity.badRequest().body("Exceptions have been thrown when trying to copy report - detailed error message - Exceptions:\n" + exceptions);
		return ResponseEntity.ok(reports);
	}

	/**
	 * Upload the given report to storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param attachment Attachment containing report.
	 * @return The response of uploading a file.
	 */
	@PostMapping(value = "/upload/{storage}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<?> uploadFile(@PathVariable("storage") String storageName, @RequestPart("file") MultipartFile attachment) {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			return ResponseEntity.internalServerError().body("Given storage [" + storage.getName() + "] is not a Crud Storage. Therefore no reports can be added externally.");
		}
		CrudStorage crudStorage = (CrudStorage) storage;
		String filename = attachment.getOriginalFilename();
		String errorMessage = null;
		try {
			InputStream in = attachment.getInputStream();
			errorMessage = Upload.upload(filename, in, crudStorage, log);
		} catch (IOException e) {
			errorMessage = e.getMessage();
		}
		if (StringUtils.isEmpty(errorMessage)) {
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.badRequest().body(errorMessage);
	}

	/**
	 * Uploads the given report to in-memory storage it, parses it and then returns the report in json format.
	 *
	 * @param attachment Attachment containing report.
	 * @return List of serialized report objects.
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFileReport(@RequestPart("file") MultipartFile attachment) {
		CrudStorage storage = new MemoryCrudStorage();
		String filename = attachment.getOriginalFilename();
		String errorMessage = null;
		try {
			InputStream in = attachment.getInputStream();
			errorMessage = Upload.upload(filename, in, storage, log);
		} catch(IOException e) {
			errorMessage = e.getMessage();
		}
		if (StringUtils.isNotEmpty(errorMessage))
			return ResponseEntity.badRequest().body(errorMessage);
		try {
			Iterator storageIdsIterator = storage.getStorageIds().iterator();
			ArrayList<Report> reports = new ArrayList<>(storage.getStorageIds().size());
			while (storageIdsIterator.hasNext()) {
				Report report = getReport(storage, ((Integer) storageIdsIterator.next()));
				reports.add(report);
			}
			return ResponseEntity.ok(reports);
		} catch (StorageException e) {
			return ResponseEntity.internalServerError().body("Could not retrieve parsed reports from in-memory storage - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Download the given reports.
	 *
	 * @param storageName Name of the storage.
	 * @param exportReportParam "true" or "1" to save the serialized version of report.
	 * @param exportReportXmlParam "true" or "1" to save Xml version of report.
	 * @param storageIds List of storage ids to download.
	 * @return The response when downloading a file.
	 */
	@GetMapping(value = "/download/{storage}/{exportReport}/{exportReportXml}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> downloadFile(@PathVariable("storage") String storageName, @PathVariable("exportReport") String exportReportParam,
								 @PathVariable("exportReportXml") String exportReportXmlParam, @RequestParam(name = "id") List<Integer> storageIds) {
		Storage storage = testTool.getStorage(storageName);
		if (storageIds == null || storageIds.isEmpty())
			return ResponseEntity.badRequest().body("No storage ids have been provided");
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
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Disposition", "attachment; filename=" + export.getSuggestedFilename());
			Resource resource = new FileSystemResource(export.getTempFile());
			if (!resource.exists()) {
				throw new StorageException("Temp file did not exist: " + export.getTempFile().getName());
			}
			return ResponseEntity.ok()
					.headers(responseHeaders)
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(resource);
		} catch (StorageException e) {
			return ResponseEntity.internalServerError().body("Exception while requesting reports with ids [" + storageIds + "] from the storage. - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Copy or move report files in the same storage to different paths.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage ids of the reports to be moved.
	 * @param map Map containing "path" and "action". Actions could be "copy" or "move".
	 * @return The response of updating the Path.
	 */
	@PutMapping(value = "/move/{storage}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updatePath(@PathVariable("storage") String storageName, @RequestParam(name = "storageIds") List<Integer> storageIds, @RequestBody Map<String, String> map) {
		CrudStorage storage = (CrudStorage) testTool.getStorage(storageName);
		String path = map.get("path");
		String action = map.get("action");
		if (StringUtils.isEmpty(action) || StringUtils.isEmpty(path))
			return ResponseEntity.badRequest().body("[action] and [path] are both required in the request body.");
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
					return ResponseEntity.badRequest().body("Action parameter can only be either [copy] or [move]");
				}
			} catch (StorageException e) {
				return ResponseEntity.internalServerError().body("Storage exception with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
			} catch (CloneNotSupportedException e) {
				return ResponseEntity.internalServerError().body("Cloning exception for report with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
			}
		}

		return ResponseEntity.ok().build();
	}

	/**
	 * Cloning the reports with the given parameters.
	 *
	 * @param storageName Storage id of the report to be cloned.
	 * @param storageId Name of the target storage.
	 * @param map Map containing csv for cloning.
	 * @return The response of cloning the report.
	 */
	@PostMapping(value = "/move/{storageName}/{storageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> cloneReport(@PathVariable("storageName") String storageName, @PathVariable("storageId") int storageId, @RequestBody Map<String, String> map) {
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
				return ResponseEntity.badRequest().body("No variables found in input message; press again to confirm");
			}
		} catch (StorageException e) {
			log.error("Exception while cloning the report", e);
			return ResponseEntity.badRequest().body("Report could not be found. - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
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
		return ResponseEntity.ok(exceptions);
	}

	/**
	 * Returns the report and sets the testTool bean on the report.
	 * 
	 * @param storage Storage to get the report from.
	 * @param storageId Storage id of the report.
	 * @return Report.
	 * @throws StorageException ...
	 */
	public Report getReport(Storage storage, Integer storageId) throws StorageException {
		Report report = storage.getReport(storageId);
		if (report != null)  report.setTestTool(testTool);
		return report;
	}

	@GetMapping(value = "warningsAndErrors/{storage}", produces = MediaType.TEXT_PLAIN_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getWarningsAndErrors(
			@PathVariable("storage") String storageName
	) {
		Storage rawStorage = testTool.getStorage(storageName);
		if (! (rawStorage instanceof LogStorage)) {
			return null;
		}
		LogStorage storage = (LogStorage) rawStorage;
		return ResponseEntity.ok(storage.getWarningsAndErrors());
	}

	@PostMapping(value = "/customreportaction")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> processCustomReportAction(@RequestParam(name = "storage") String storageName, List<Integer> reportIds) {
		Storage storage = testTool.getStorage(storageName);
		List<Report> reports = new ArrayList<>();
		for (int storageId : reportIds) {
			try {
				Report report = getReport(storage, storageId);
				if (report == null)
					return new ResponseEntity<String>("Could not find report with storage id [" + storageId + "]", HttpStatus.NOT_FOUND);
				reports.add(report);
			} catch (StorageException e) {
				e.printStackTrace();
			}
		}
		if (customReportAction == null) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "No custom report action defined.");
			return ResponseEntity.badRequest().body(errorResponse);
		}
		CustomReportActionResult customReportActionResult = customReportAction.get().handleReports(reports);
		Map<String, String> response = new HashMap<>();
		response.put("success", customReportActionResult.getSuccessMessage());
		response.put("error", customReportActionResult.getErrorMessage());
		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "/variables/", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> fetchVariables() {
		Map<String, String> variables = new HashMap<>();
		String buttonText = (customReportAction.orElse(null) != null) ? customReportAction.get().getButtonText() : null;
		variables.put("customReportActionButtonText", buttonText);
		return ResponseEntity.ok(variables);
	}
}
