/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool;

import java.util.List;

import nl.nn.testtool.run.ReportRunner;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface Debugger {

	public List<String> getStubStrategies();

	public String getDefaultStubStrategy();

	/**
	 * Checks whether a checkpoint will be stubbed so code can be skipped.
	 * 
	 * @param checkpoint  the checkpoint that may be stubbed
	 * @param strategy    the used subbing strategy
	 * @return            <code>true</code> when this checkpoint will be stubbed 
	 */
	public boolean stub(Checkpoint checkpoint, String strategy);

	/**
	 * Rerun a previous generated report. This method should at least trigger
	 * the same first checkpoint as has been triggered when the original report
	 * was created.
	 *  
	 * @param correlationId   the correlationId to be used
	 * @param originalReport  the original report that should be rerun
	 * @return                an error message when an error occurred 
	 */
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner);
	
	/**
	 * Method to be called from the Ladybug to notify the application using
	 * the Ladybug of a change in the report generator's status.
	 * @param enabled Whether the Ladybug's report generator should be enabled.
	 */
	public void updateReportGeneratorStatus(boolean enabled);
}
