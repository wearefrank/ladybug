/*
   Copyright 2023-2025 WeAreFrank!

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

public class DatabaseTestStorage extends DatabaseCrudStorage {

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
		// All fields queried by the frontend through the metadata api need to be present. Typically the following URL
		// is called by the frontend:
		// /api/metadata/Test/?metadataNames=storageId&metadataNames=name&metadataNames=path&metadataNames=description&metadataNames=variables
		return new ArrayList<String>(Arrays.asList("storageId", "path", "name", "description", "variables"));
	}

}
