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
package nl.nn.testtool.storage.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import nl.nn.testtool.storage.StorageException;

public class DatabaseTestStorage extends DatabaseStorage {

	@Override
	public String getTable() {
		if (table == null) {
			return "LADYBUGTEST";
		} else {
			return table;
		}
	}

	@Override
	public List<String> getMetadataNames() {
		return new ArrayList<String>(Arrays.asList("storageId", "path", "name", "description"));
	}

	@Override
	public long getMaxStorageSize() {
		return -1L;
	}

	@Override
	@PostConstruct
	public void init() throws StorageException {
		super.init();
	}

}
