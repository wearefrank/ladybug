/*
   Copyright 2020-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.storage.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.util.SearchUtil;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jaco de Groot
 */
public class MemoryStorage implements Storage {
	protected @Setter @Getter String name;
	protected Map<Integer, Report> reports;
	protected List<Integer> storageIds;
	protected Map<Integer, Map<Integer, Map<String, Object>>> metadata;
	protected @Setter @Getter Integer initialStorageId = 0;
	protected @Setter @Inject @Autowired MetadataExtractor metadataExtractor;

	public MemoryStorage() {
		// Initialize variables in the constructor for places where MemoryStorage is used without being initialized by
		// Spring or Quarkus. Use a separate reset() method as a convenient way for child classes to reset the variables.
		this.reset();
	}

	protected void reset() {
		reports = new HashMap<Integer, Report>();
		storageIds = new ArrayList<Integer>();
		metadata = new HashMap<Integer, Map<Integer, Map<String, Object>>>();
	}

	public synchronized void store(Report report) throws StorageException {
		report.setStorage(this);
		report.setStorageId(getNewStorageId());
		reports.put(report.getStorageId(), report);
		storageIds.add(report.getStorageId());
	}

	@Override
	public synchronized Report getReport(Integer storageId) throws StorageException {
		Report report = reports.get(storageId);
		if (report != null) {
			try {
				report = report.clone();
				report.setStorageId(storageId);
				report.setStorage(this);
			} catch (CloneNotSupportedException e) {
				throw new StorageException("Could not clone report", e);
			}
		}
		return report;
	}

	@Override
	public int getSize() throws StorageException {
		return storageIds.size();
	}

	@Override
	public synchronized List<Integer> getStorageIds() throws StorageException {
		return new ArrayList<Integer>(storageIds);
	}

	@Override
	public synchronized List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames,
													   List<String> searchValues, int metadataValueType) throws StorageException {
		List<List<Object>> result = new ArrayList<List<Object>>();
		for (int i = 0; i < storageIds.size() && (maxNumberOfRecords == -1 || i < maxNumberOfRecords); i++) {
			Map<Integer, Map<String, Object>> metadataRecordPerType = metadata.get(storageIds.get(i));
			if (metadataRecordPerType == null) {
				metadataRecordPerType = new HashMap<Integer, Map<String, Object>>();
				metadata.put(storageIds.get(i), metadataRecordPerType);
			}
			Map<String, Object> metadataRecord = metadataRecordPerType.get(metadataValueType);
			if (metadataRecord == null) {
				metadataRecord = new HashMap<String, Object>();
				metadataRecordPerType.put(metadataValueType, metadataRecord);
			}
			List<Object> resultRecord = new ArrayList<Object>();
			Iterator<String> metadataNamesIterator = metadataNames.iterator();
			while (metadataNamesIterator.hasNext()) {
				String metadataName = (String) metadataNamesIterator.next();
				Object metadataValue;
				if (!metadataRecord.keySet().contains(metadataName)) {
					Report report = getReport((Integer) storageIds.get(i));
					metadataValue = metadataExtractor.getMetadata(report, metadataName, metadataValueType);
					metadataRecord.put(metadataName, metadataValue);
				} else {
					metadataValue = metadataRecord.get(metadataName);
				}
				resultRecord.add(metadataValue);
			}
			if (SearchUtil.matches(resultRecord, searchValues)) {
				result.add(resultRecord);
			}
		}
		return result;
	}

	@Override
	public void clear() throws StorageException {
		reports.clear();
		storageIds.clear();
		metadata.clear();
	}

	@Override
	public void close() {
	}

	@Override
	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	@Override
	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	@Override
	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}

	protected int getNewStorageId() throws StorageException {
		int newStorageId = getInitialStorageId();
		for (Integer storageId : getStorageIds()) {
			if (storageId >= newStorageId) {
				newStorageId = storageId + 1;
			}
		}
		return newStorageId;
	}

}
