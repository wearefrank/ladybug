package org.wearefrank.ladybug.web.common;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.run.RunResult;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.wearefrank.ladybug.web.common.Util.fullMessage;

@Component
public class RunApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter
	@Autowired TestTool testTool;
	private @Setter @Autowired ReportXmlTransformer reportXmlTransformer;

	public Map<String, Object> runReport(String storageName, int storageId) throws HttpBadRequestException, HttpInternalServerErrorException {
		Map<String, Object> result = new HashMap<>();
		String errorMessage = null;
		try {
			Report report = testTool.getStorage(storageName).getReport(storageId);
			if (report != null) {
				report.setTestTool(testTool);
				ReportRunner runner = new ReportRunner();
				runner.setTestTool(testTool);
				runner.setDebugStorage(testTool.getDebugStorage());
				errorMessage = runner.run(Collections.singletonList(report), true, true);
				if (errorMessage == null) {
					RunResult runResult = runner.getResults().get(storageId);
					if (runResult.errorMessage == null) {
						Report runResultReport = runner.getRunResultReport(runResult.correlationId);
						runResultReport.setTestTool(testTool);
						result = extractRunResult(report, runResultReport, runner);
					} else {
						errorMessage = runResult.errorMessage;
					}
				}
			}
		} catch (StorageException e) {
			errorMessage = "Storage exception: " + fullMessage(e);
			log.error(errorMessage, e);
			throw new HttpInternalServerErrorException(errorMessage);
		}
		if (errorMessage != null) {
			throw new HttpBadRequestException(errorMessage);
		}
		return result;
	}

	private Map<String, Object> extractRunResult(Report report, Report runResultReport, ReportRunner runner) {
		Map<String, Object> res = new HashMap<>();
		report.setGlobalReportXmlTransformer(reportXmlTransformer);
		runResultReport.setGlobalReportXmlTransformer(reportXmlTransformer);
		runResultReport.setTransformation(report.getTransformation());
		runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
		res.put("info", ReportRunner.getRunResultInfo(report, runResultReport));
		res.put("equal", report.toXml(runner).equals(runResultReport.toXml(runner)));
		res.put("originalReport", report);
		res.put("runResultReport", runResultReport);
		res.put("originalXml", report.toXml(runner));
		res.put("runResultXml", runResultReport.toXml(runner));
		return res;
	}
}
