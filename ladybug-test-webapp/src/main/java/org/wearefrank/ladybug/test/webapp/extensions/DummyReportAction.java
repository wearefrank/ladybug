/*
   Copyright 2023 WeAreFrank!

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
package org.wearefrank.ladybug.test.webapp.extensions;

import java.util.List;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.extensions.CustomReportAction;
import org.wearefrank.ladybug.extensions.CustomReportActionResult;

public class DummyReportAction implements CustomReportAction {

	@Override
	public String getButtonText() {
		return "Custom action";
	}

	@Override
	public CustomReportActionResult handleReports(List<Report> reports) {
		CustomReportActionResult customReportActionResult = new CustomReportActionResult();
		customReportActionResult.setSuccessMessage("Success for reports: " + reports);
		customReportActionResult.setErrorMessage("Failure!");
		return customReportActionResult;
	}

}
