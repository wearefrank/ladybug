/*
   Copyright 2025, 2026 WeAreFrank!

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
package org.wearefrank.ladybug.web.springmvc.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.annotation.MultipartConfig;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.util.ExportResult;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.HttpNotFoundException;
import org.wearefrank.ladybug.web.common.HttpNotImplementedException;
import org.wearefrank.ladybug.web.common.ReportApiImpl;
import org.wearefrank.ladybug.web.common.ReportUpdateRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/report")
@MultipartConfig
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})

public class ReportApi {
	@Autowired
	private @Setter ReportApiImpl delegate;

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GetMapping(value = "/{storage}/{storageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReport(@PathVariable("storage") String storageName,
									   @PathVariable("storageId") int storageId,
									   @RequestParam(name = "xml", defaultValue = "false") boolean xml,
									   @RequestParam(name = "globalTransformer", defaultValue = "false") boolean globalTransformer) {
		try {
			Map<String, Object> result = delegate.getReport(storageName, storageId, xml, globalTransformer);
			return ResponseEntity.ok(result);
		} catch (HttpNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
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
			List<String> result = delegate.getCheckpointUids(storageName, storageId, viewName, invert);
			return ResponseEntity.ok(result);
		} catch (HttpNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
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
	@GetMapping(value = "/{storage}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReports(@PathVariable("storage") String storageName,
										@RequestParam(name = "storageIds") List<Integer> storageIds,
										@RequestParam(name = "xml", defaultValue = "false") boolean xml,
										@RequestParam(name = "globalTransformer", defaultValue = "false") boolean globalTransformer) {
		try {
			Map<Integer, Map<String, Object>> result = delegate.getReports(storageName, storageIds, xml, globalTransformer);
			return ResponseEntity.ok(result);
		} catch(HttpNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@DeleteMapping(value = "/{storage}")
	public ResponseEntity<?> deleteReport(@PathVariable("storage") String storageName, @RequestParam(name = "storageIds") List<Integer> storageIds) {
		try {
			delegate.deleteReport(storageName, storageIds);
		} catch (HttpNotImplementedException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_IMPLEMENTED);
		} catch(HttpNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
		return ResponseEntity.ok().build();
	}

	@DeleteMapping(value = "/all/{storage}")
	public ResponseEntity<?> deleteAllReports(@PathVariable("storage") String storageName) {
		try {
			delegate.deleteAllReports(storageName);
			return ResponseEntity.ok().build();
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping(value = "/latest/{storage}/{numberOfReports}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getLatestReports(@PathVariable("storage") String storageName, @PathVariable("numberOfReports") int number) {
		try {
			List<Report> result = delegate.getLatestReports(storageName, number);
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	/**
	 * Update the report with the given values..
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param req Bean with field updates.
	 * @return The updated report.
	 */
	@PostMapping(value = "/{storage}/{storageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReport(@PathVariable("storage") String storageName, @PathVariable("storageId") int storageId, @RequestBody ReportUpdateRequest req) {
		try {
			Map<String, Serializable> result = delegate.updateReport(storageName, storageId, req);
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpNotFoundException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
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
			Map<String, String> result = delegate.getReportTransformation(storageName, storageId);
			return ResponseEntity.ok(result);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
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
		try {
			List<Report> result = delegate.copyReport(storageName, sources);
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/upload/{storage}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<?> uploadFile(@PathVariable("storage") String storageName, @RequestPart("file") MultipartFile attachment) {
		try {
			delegate.uploadFile(storageName, () -> {
				String filename = attachment.getOriginalFilename();
				try {
					InputStream in = attachment.getInputStream();
					return new ReportApiImpl.AttachmentBeingRead(filename, in);
				} catch (IOException e) {
					return new ReportApiImpl.AttachmentBeingRead(filename, null, e);
				}
			});
			return ResponseEntity.ok().build();
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Uploads the given report to in-memory storage it, parses it and then returns the report in json format.
	 *
	 * @param attachment Attachment containing report.
	 * @return List of serialized report objects.
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFileReport(@RequestPart("file") MultipartFile attachment) {
		try {
			List<Report> result = delegate.getFileReport(() -> {
				String filename = attachment.getOriginalFilename();
				try {
					InputStream in = attachment.getInputStream();
					return new ReportApiImpl.AttachmentBeingRead(filename, in);
				} catch(IOException e) {
					return new ReportApiImpl.AttachmentBeingRead(filename, null, e);
				}
			});
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
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
		try {
			ExportResult result = delegate.downloadFile(storageName, exportReportParam, exportReportXmlParam, storageIds);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Disposition", "attachment; filename=" + result.getSuggestedFilename());
			Resource resource = new FileSystemResource(result.getTempFile());
			if (!resource.exists()) {
				throw new HttpInternalServerErrorException("Temp file did not exist: " + result.getTempFile().getName());
			}
			return ResponseEntity.ok()
					.headers(responseHeaders)
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(resource);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
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
		try {
			delegate.updatePath(storageName, storageIds, map);
			return ResponseEntity.ok().build();
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
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
		try {
			List<String> result = delegate.cloneReport(storageName, storageId, map);
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping(value = "warningsAndErrors/{storage}", produces = MediaType.TEXT_PLAIN_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getWarningsAndErrors(
			@PathVariable("storage") String storageName
	) {
		return ResponseEntity.ok(delegate.getWarningsAndErrors(storageName));
	}

	@PostMapping(value = "/customreportaction")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> processCustomReportAction(@RequestParam(name = "storage") String storageName, List<Integer> reportIds) {
		try {
			Map<String, String> result = delegate.processCustomReportAction(storageName, reportIds);
			return ResponseEntity.ok(result);
		} catch(HttpNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping(value = "/variables", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> fetchVariables() {
		return ResponseEntity.ok(delegate.fetchVariables());
	}
}
