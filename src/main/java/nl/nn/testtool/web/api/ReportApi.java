/*
   Copyright 2021-2022 WeAreFrank!

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

import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.test.TestComponent;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import java.util.Scanner;

@Path("/")
public class ReportApi extends ApiBase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter TestTool testTool;
	private @Setter ReportXmlTransformer reportXmlTransformer;

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GET
	@Path("/report/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storageName,
							  @PathParam("storageId") int storageId,
							  @QueryParam("xml") @DefaultValue("false") boolean xml,
							  @QueryParam("globalTransformer") @DefaultValue("false") boolean globalTransformer) {
		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Could not find report with id [" + storageId + "]").build();

			if (globalTransformer) {
				if (reportXmlTransformer != null)
					report.setGlobalReportXmlTransformer(reportXmlTransformer);
			}

			HashMap<String, Object> map = new HashMap<>(1);
			map.put("report", report);
			map.put("xml", report.toXml());

			return Response.ok(map).build();

		} catch (Exception e) {
			return Response.status(Response.Status.NOT_FOUND).entity("Exception while getting report [" + storageId + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GET
	@Path("/report/{storage}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReports(@PathParam("storage") String storageName,
							   @QueryParam("storageIds") ArrayList<Integer> storageIds,
							   @QueryParam("xml") @DefaultValue("false") boolean xml,
							   @QueryParam("globalTransformer") @DefaultValue("false") boolean globalTransformer) {
		try {
			Storage storage = testTool.getStorage(storageName);
			HashMap<Integer, HashMap<String, Object>> map = new HashMap<>();

			for (int storageId : storageIds) {
				Report report = getReport(storage, storageId);
				if (report == null)
					return Response.status(Response.Status.NOT_FOUND).entity("Could not find report with id [" + storageId + "]").build();

				if (globalTransformer) {
					if (reportXmlTransformer != null)
						report.setGlobalReportXmlTransformer(reportXmlTransformer);
				}

				HashMap<String, Object> reportMap = new HashMap<>(1);
				reportMap.put("report", report);
				reportMap.put("xml", report.toXml());

				map.put(storageId, reportMap);
			}

			return Response.ok(map).build();

		} catch (Exception e) {
			return Response.status(Response.Status.NOT_FOUND).entity("Exception while getting report [" + storageIds + "] from storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}
	/**
	 * Deletes the report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report to be deleted.
	 * @return "Ok" if deleted properly, "Not implemented" if storage does not support deletion, "Not found" if report does not exist.
	 */
	@DELETE
	@Path("/report/{storage}/{storageId}")
	public Response deleteReport(@PathParam("storage") String storageName, @PathParam("storageId") int storageId) {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageName + "] does not implement delete function.";
			log.warn(msg);
			return Response.status(Response.Status.NOT_IMPLEMENTED).entity(msg).build();
		}
		try {
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Could not find report with storage id [" + storageId + "]").build();
			((CrudStorage) storage).delete(report);
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not delete report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
		return Response.ok().build();
	}

	/**
	 * Get the n latest reports in the storage.
	 *
	 * @param storageName Name of the storage.
	 * @param number Number of latest reports to retrieve.
	 * @return the n latest reports.
	 */
	@GET
	@Path("/report/latest/{storage}/{numberOfReports}")
	public Response getLatestReports(@PathParam("storage") String storageName, @PathParam("numberOfReports") int number) {
		try {
			Storage storage = testTool.getStorage(storageName);
			List<List<Object>> metadata = storage.getMetadata(-1, Arrays.asList("storageId", "endTime"),
					Arrays.asList(null, null), MetadataExtractor.VALUE_TYPE_OBJECT);
			int amount = Math.min(metadata.size(), number);
			if (amount < 1)
				return Response.status(Response.Status.BAD_REQUEST).entity("Either the number of reports requested [" + number + "] and/or the size of reports available [" + metadata.size() + "] is 0").build();

			metadata.sort(Comparator.comparingLong(o -> (Long) o.get(1)));
			ArrayList<Report> reports = new ArrayList<>(amount);
			for (int i = 1; i <= amount; i++) {
				reports.add(getReport(storage, (Integer) metadata.get(metadata.size() - i).get(0)));
			}
			return Response.ok(reports).build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not retrieve latest [" + number + "] reports - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Update the report with the given values..
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param map Map containing ["name" or "path" or "variables" or "description" or "transformation" or "checkpointId and "checkpointMessage"]
	 * @return The updated report.
	 */
	@POST
	@Path("/report/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReport(@PathParam("storage") String storageName, @PathParam("storageId") int storageId, Map<String, String> map) {
		String[] fields = new String[]{"name", "path", "variables", "description", "transformation", "checkpointId", "checkpointMessage", "stub"};
		if (map.isEmpty() || !mapContainsOnly(map, null, fields))
			return Response.status(Response.Status.BAD_REQUEST).entity("No new values or incorrect values have been given for report with storageId [" + storageId + "] - detailed error message - Values given are:\n" + map).build();

		try {
			Storage storage = testTool.getStorage(storageName);
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Could not find report with storageId [" + storageId + "]").build();
			if (StringUtils.isNotEmpty(map.get("name")))
				report.setName(map.get("name"));

			if (StringUtils.isNotEmpty(map.get("path")))
				report.setPath(TestComponent.normalizePath(map.get("path")));

			if (StringUtils.isNotEmpty(map.get("description")))
				report.setDescription(map.get("description"));

			if (StringUtils.isNotEmpty(map.get("transformation")))
				report.setTransformation(map.get("transformation"));

			String variables = map.get("variables");
			if (variables != null && !variables.equals(report.getVariableCsv())) {
				report.setVariableCsv(variables);
			}

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
			return Response.ok(result).build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not apply transformation to report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Return transformation of a report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @return Response containing a map containing transformation.
	 */
	@GET
	@Path("/report/transformation/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("storage") String storageName, @PathParam("storageId") int storageId) {
		try {
			Storage storage = testTool.getStorage(storageName);
			String transformation = getReport(storage, storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return Response.ok(map).build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not retrieve transformation of report with storageId [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Copy the reports from the given storages and ids to the given target storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param sources Map [String, Integer] where keys are storage names and integers are storage ids for the reports to be copied.
	 * @return The copied report.
	 */
	@PUT
	@Path("/report/store/{storage}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copyReport(@PathParam("storage") String storageName, Map<String, List<Integer>> sources) {
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
			return Response.status(Response.Status.BAD_REQUEST).entity("Exceptions have been thrown when trying to copy report - detailed error message - Exceptions:\n" + exceptions).build();
		return Response.ok(reports).build();
	}

	/**
	 * Upload the given report to storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param attachment Attachment containing report.
	 * @return The response of uploading a file.
	 */
	@POST
	@Path("/report/upload/{storage}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@PathParam("storage") String storageName, @Multipart("file") Attachment attachment) {
		Storage storage = testTool.getStorage(storageName);
		if (!(storage instanceof CrudStorage)) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Given storage [" + storage.getName() + "] is not a Crud Storage. Therefore no reports can be added externally.").build();
		}
		CrudStorage crudStorage = (CrudStorage) storage;

		String filename = attachment.getContentDisposition().getParameter("filename");
		InputStream in = attachment.getObject(InputStream.class);
		String errorMessage = Upload.upload(filename, in, crudStorage, log);
		if (StringUtils.isEmpty(errorMessage)) {
			return Response.ok().build();
		}
		return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
	}

	/**
	 * Uploads the given report to in-memory storage it, parses it and then returns the report in json format.
	 *
	 * @param attachment Attachment containing report.
	 * @return List of serialized report objects.
	 */
	@POST
	@Path("/report/upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getFileReport(@Multipart("file") Attachment attachment) {
		CrudStorage storage = new nl.nn.testtool.storage.memory.Storage();
		String filename = attachment.getContentDisposition().getParameter("filename");
		InputStream in = attachment.getObject(InputStream.class);
		String errorMessage = Upload.upload(filename, in, storage, log);
		if (StringUtils.isNotEmpty(errorMessage))
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		try {
			Iterator storageIdsIterator = storage.getStorageIds().iterator();
			ArrayList<Report> reports = new ArrayList<>(storage.getStorageIds().size());
			while (storageIdsIterator.hasNext()) {
				Report report = getReport(storage, ((Integer) storageIdsIterator.next()));
				reports.add(report);
			}
			return Response.ok(reports).build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not retrieve parsed reports from in-memory storage - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
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
	@GET
	@Path("/report/download/{storage}/{exportReport}/{exportReportXml}")
	@Produces("application/octet-stream")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response downloadFile(@PathParam("storage") String storageName, @PathParam("exportReport") String exportReportParam,
								 @PathParam("exportReportXml") String exportReportXmlParam, @QueryParam("id") List<Integer> storageIds) {
		Storage storage = testTool.getStorage(storageName);
		if (storageIds == null || storageIds.isEmpty())
			return Response.status(Response.Status.BAD_REQUEST).entity("No storage ids have been provided").build();
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
			Response.ResponseBuilder response = Response.ok(export.getTempFile(), MediaType.APPLICATION_OCTET_STREAM);
			response.header("Content-Disposition", "attachment; filename=" + export.getSuggestedFilename());
			return response.build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while requesting reports with ids [" + storageIds + "] from the storage. - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Copy or move report files in the same storage to different paths.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report to be moved.
	 * @param map Map containing "path" and "action". Actions could be "copy" or "move".
	 * @return The response of updating the Path.
	 */
	@PUT
	@Path("/report/move/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePath(@PathParam("storage") String storageName, @PathParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = (CrudStorage) testTool.getStorage(storageName);
		String path = map.get("path");
		String action = map.get("action");
		if (StringUtils.isEmpty(action) || StringUtils.isEmpty(path))
			return Response.status(Response.Status.BAD_REQUEST).entity("[action] and [path] are both required in the request body.").build();

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
				return Response.status(Response.Status.BAD_REQUEST).entity("Action parameter can only be either [copy] or [move]").build();
			}

			return Response.ok().build();
		} catch (StorageException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Storage exception with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		} catch (CloneNotSupportedException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Cloning exception for report with storage id [" + storageId + "] in storage [" + storageName + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Cloning the reports with the given parameters.
	 *
	 * @param storageId Storage id of the report to be cloned.
	 * @param map Map containing csv for cloning.
	 * @return The response of cloning the report.
	 */
	@POST
	@Path("/report/move/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cloneReport(@QueryParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = testTool.getTestStorage();
		Report original;
		try {
			original = getReport(storage, storageId);
			String previousMessage = original.getInputCheckpoint().getMessage();
			boolean force = "true".equalsIgnoreCase(map.getOrDefault("force", "false"))
					|| "1".equalsIgnoreCase(map.getOrDefault("force", "false"));
			original.getInputCheckpoint().setMessage(map.get("message"));
			if (!original.getInputCheckpoint().containsVariables() && !force) {
				original.getInputCheckpoint().setMessage(previousMessage);
				return Response.status(Response.Status.BAD_REQUEST).entity("No variables found in input message; press again to confirm").build();
			}
		} catch (StorageException e) {
			log.error("Exception while cloning the report", e);
			return Response.status(Response.Status.BAD_REQUEST).entity("Report could not be found. - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
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
						clone.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.store(clone);
					} else {
						originalSet = true;
						original.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.update(original);
					}
				} catch (CloneNotSupportedException | StorageException e) {
					exceptions.add(e.getMessage() + " Caused by: " + e.getCause() + " For line: [" + nextLine + "]");
				}
			}
		}
		scanner.close();
		return Response.ok().entity(exceptions).build();
	}

	/**
	 * Returns the report and sets the testtool with the bean named "testTool".
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
}
