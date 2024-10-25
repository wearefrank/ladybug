/*
   Copyright 2020, 2022, 2024 WeAreFrank!, 2018 Nationale-Nederlanden

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

import nl.nn.testtool.Config;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;

/**
 * Storage (optimized) for (secure) logging storing data for a time or size limited period. This storage is targeted at
 * minimizing the overhead on the process being logged (minimal delay and interruptions) and being secure in the sense
 * that it should not be possible for a user to change the data that has been logged by the system (so malicious persons
 * cannot change their trails). Hence for production systems it is strongly advisable to use a storage that does not
 * implement {@link CrudStorage} as {@link Config#debugStorage(MetadataExtractor metadataExtractor) debug storage}.
 * Although the Debug tab does support a CrudStorage (in which case reports can be changed) this is for specific use
 * cases where people don't expect the Debug tab to tell the "truth" about what has happened in a system.
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
