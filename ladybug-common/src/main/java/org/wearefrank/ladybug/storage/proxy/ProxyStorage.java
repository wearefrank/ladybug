/*
   Copyright 2022-2025 WeAreFrank!

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
package org.wearefrank.ladybug.storage.proxy;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;

/**
 * Storage proxy class than can help Spring XML configuration to become more flexible (e.g. see #{testStorageActive} in
 * Frank!Framework's springIbisTestTool.xml) and use an alternative storage when getSize() on the preferred storage
 * throws an exception. This can for example be used to check whether the Ladybug tables are available and if not use
 * file storage instead of database storage (see proxy storage usage in Frank!Framework's springIbisTestTool.xml).
 * 
 * @author Jaco de Groot
 */
public class ProxyStorage implements CrudStorage, LogStorage {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Getter String name;
	private @Setter Storage destination;
	private @Setter Storage alternativeDestination;

	@PostConstruct
	public void init() throws StorageException {
		try {
			destination.getSize();
		} catch(StorageException e) {
			if (alternativeDestination != null) {
				log.debug("Could not init " + destination.getName() + " will use " + alternativeDestination.getName()
						+ " instead");
				destination = alternativeDestination;
			} else {
				throw e;
			}
		}
	}

	@Override
	public synchronized void store(Report report) throws StorageException {
		((CrudStorage)destination).store(report);
	}

	@Override
	public void update(Report report) throws StorageException {
		((CrudStorage)destination).update(report);
	}

	@Override
	public void delete(Report report) throws StorageException {
		((CrudStorage)destination).delete(report);
	}

	@Override
	public void storeWithoutException(Report report) {
		((LogStorage)destination).storeWithoutException(report);
	}

	@Override
	public int getSize() throws StorageException {
		return destination.getSize();
	}

	@Override
	public synchronized List<Integer> getStorageIds() throws StorageException {
		return destination.getStorageIds();
	}

	@Override
	public synchronized List getMetadata(int maxNumberOfRecords, List metadataNames,
			List searchValues, int metadataValueType) throws StorageException {
		return destination.getMetadata(maxNumberOfRecords, metadataNames, searchValues, metadataValueType);
	}

	@Override
	public synchronized Report getReport(Integer storageId) throws StorageException {
		return destination.getReport(storageId);
	}

	@Override
	public void clear() throws StorageException {
		destination.clear();
	}

	@Override
	public void close() {
		destination.close();
	}

	@Override
	public int getFilterType(String column) {
		return destination.getFilterType(column);
	}

	@Override
	public List getFilterValues(String column) throws StorageException {
		return destination.getFilterValues(column);
	}

	@Override
	public String getUserHelp(String column) {
		return destination.getUserHelp(column);
	}

	@Override
	public String getWarningsAndErrors() {
		return ((LogStorage)destination).getWarningsAndErrors();
	}
}
