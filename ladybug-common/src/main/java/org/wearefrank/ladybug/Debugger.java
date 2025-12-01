/*
   Copyright 2020, 2022, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug;

import java.util.List;

/**
 * @author Jaco de Groot
 */
public interface Debugger extends Rerunner {

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
	 * Method to be called from the Ladybug to notify the application using
	 * the Ladybug of a change in the report generator's status.
	 * @param enabled Whether the Ladybug's report generator should be enabled.
	 */
	public void updateReportGeneratorStatus(boolean enabled);
}
