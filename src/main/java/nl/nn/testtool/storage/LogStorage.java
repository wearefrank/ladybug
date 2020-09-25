/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.testtool.storage;

import nl.nn.testtool.Report;

/**
 * Storage (optimized) for logging. Targeted at minimizing the overhead on the
 * process being logged (minimal delay and interruptions).
 * 
 * @author Jaco de Groot
 */
public interface LogStorage extends Storage {

	public void storeWithoutException(Report report);

	/**
	 * Get warnings and errors that need user attention like file system (almost) full or exceptions while storing
	 * reports.
	 * 
	 * @return the message to show to the user or null when everything is fine
	 */
	public String getWarningsAndErrors();
}
