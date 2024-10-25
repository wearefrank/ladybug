/*
   Copyright 2024 WeAreFrank!

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
package nl.nn.testtool.storage.database;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;

/**
 * Special use case database storage that can be used as debug storage IS DT ZO? MOET HIJ DAN NIET OOK LOG STOAGE IMPLEMENTEREN? 
 */
public class DatabaseCrudStorage extends DatabaseStorage implements CrudStorage {

	@Override
	public long getMaxStorageSize() {
		return -1L;
	}

	@Override
	public void update(Report report) throws StorageException {
		delete(report.getStorageId());
		store(report);
	}

	@Override
	public void delete(Report report) throws StorageException {
		delete(report.getStorageId());
	}

	protected void delete(Integer storageId) throws StorageException {
		String query = "delete from " + getTable() + " where " + getStorageIdColumn() + " = ?";
		delete(query, storageId);
	}

}
