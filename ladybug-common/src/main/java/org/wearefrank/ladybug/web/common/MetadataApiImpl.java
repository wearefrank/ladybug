/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.common;

import jakarta.inject.Inject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetadataApiImpl {
	private @Setter
	@Inject
	@Autowired TestTool testTool;

	public List<LinkedHashMap<String, String>> getMetadataList(String storageName,
															   List<String> metadataNames,
															   int limit,
															   List<String> filterHeaders,
															   List<String> filterParams) throws HttpInternalServerErrorException {
		List<String> searchValues = new ArrayList<>();
		for (String field : metadataNames) {
			boolean changed = false;
			for (int filterHeaderIndex = 0; filterHeaderIndex < filterHeaders.size(); filterHeaderIndex++) {
				if (filterHeaders.get(filterHeaderIndex).equals(field)) {
					searchValues.add(filterParams.get(filterHeaderIndex));
					changed = true;
				}
			}
			if (!changed) {
				searchValues.add(null);
			}
		}
		// Get storage, search for metadata, and return the results.
		Storage storage = testTool.getStorage(storageName);
		List<List<Object>> records = null;
		try {
			records = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_GUI);
		} catch(Exception e) {
			throw new HttpInternalServerErrorException(e);
		}
		List<LinkedHashMap<String, String>> metadata = new ArrayList<>();
		for (List<Object> record : records) {
			LinkedHashMap<String, String> metadataItem = new LinkedHashMap<>();
			metadataItem.put("storageId", record.get(0).toString());
			for (int i = 1; i < metadataNames.size(); i++) {
				String metadataValue = null;
				if (record.get(i) != null) {
					metadataValue = record.get(i).toString();
				}
				metadataItem.put(metadataNames.get(i), metadataValue);
			}
			metadata.add(metadataItem);
		}
		return metadata;
	}

	public Map<String, String> getUserHelp(String storageName, List<String> metadataNames) {
		Map<String, String> userHelp = new LinkedHashMap<>();
		Storage storage = testTool.getStorage(storageName);
		for (String field : metadataNames) {
			userHelp.put(field, storage.getUserHelp(field));
		}
		return userHelp;
	}

	public int getMetadataCount(String storageName) throws HttpInternalServerErrorException {
		try {
			Storage storage = testTool.getStorage(storageName);
			return storage.getSize();
		} catch(StorageException e) {
			throw new HttpInternalServerErrorException(e);
		}
	}
}
