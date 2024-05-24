/*
   Copyright 2020-2024 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
public class Storage implements CrudStorage, LogStorage {
	protected String name;
	protected Map<Integer, Report> reports;
	protected List<Integer> storageIds;
	protected Map<Integer, Map<Integer, Map<String, Object>>> metadata;
	private int initialStorageId = 0;
	protected int storageId;
	protected @Inject @Autowired MetadataExtractor metadataExtractor;
	
	public Storage() {
		reports = new HashMap<Integer, Report>();
		storageIds = new ArrayList<Integer>();
		metadata = new HashMap<Integer, Map<Integer, Map<String, Object>>>();
	}

	/**
	 * Allows test code to use large storage ids, distinguishing storage ids from values with another meaning
	 */
	@PostConstruct
	private void setInitialStorageId() {
		storageId = initialStorageId;
	}

	public void setInitialStorageId(int value) {
		this.initialStorageId = value;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}

	@Override
	public synchronized void store(Report report) {
		report.setStorage(this);
		report.setStorageId(storageId++);
		reports.put(report.getStorageId(), report);
		storageIds.add(report.getStorageId());
	}

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

	@Override
	public void storeWithoutException(Report report) {
		store(report);
	}

	@Override
	public int getSize() {
		return storageIds.size();
	}

	@Override
	public synchronized List<Integer> getStorageIds() {
		return new ArrayList<Integer>(storageIds);
	}

	@Override
	public synchronized List getMetadata(int maxNumberOfRecords, List metadataNames,
			List searchValues, int metadataValueType) throws StorageException {
		List<Object> result = new ArrayList<Object>();
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
				String metadataName = (String)metadataNamesIterator.next();
				Object metadataValue;
				if (!metadataRecord.keySet().contains(metadataName)) {
					Report report = getReport((Integer)storageIds.get(i));
					metadataValue = metadataExtractor.getMetadata(report,
							metadataName, metadataValueType);
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
	public synchronized Report getReport(Integer storageId) throws StorageException {
		Report report = (Report)reports.get(storageId);
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

	@Override
	public String getWarningsAndErrors() {
		return null;
	}
}
