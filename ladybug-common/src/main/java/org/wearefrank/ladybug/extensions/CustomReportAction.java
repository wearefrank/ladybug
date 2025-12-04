/*
   Copyright 2023, 2025 WeAreFrank!

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
package org.wearefrank.ladybug.extensions;

import java.util.List;

import org.wearefrank.ladybug.Report;

/**
 * When a bean is present that implements this interface a button will be added to the Test tab that will call this
 * bean when the button is pressed
 *
 * @author Jaco de Groot
 */
public interface CustomReportAction {

	/**
	 * The text to display on the button
	 *
	 * @return The text to display on the button
	 */
	public String getButtonText();

	/**
	 * This method is called when button is pressed
	 *
	 * @param reports The selected reports
	 * @return A success and/or error message to display to the user
	 */
	public CustomReportActionResult handleReports(List<Report> reports);

}
