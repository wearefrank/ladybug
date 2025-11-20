/*
   Copyright 2024, 2025 WeAreFrank!

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
package nl.nn.testtool.storage.memory;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;

/**
 * @author Jaco de Groot
 */
public class MemoryCrudStorage extends MemoryStorage implements CrudStorage {

	@Override
	public void update(Report report) throws StorageException {
		reports.put(report.getStorageId(), report);
		metadata.remove(report.getStorageId());
	}

	@Override
	public void delete(Report report) throws StorageException {
		reports.remove(report.getStorageId());
		storageIds.remove(report.getStorageId());
		metadata.remove(report.getStorageId());
	}

}
