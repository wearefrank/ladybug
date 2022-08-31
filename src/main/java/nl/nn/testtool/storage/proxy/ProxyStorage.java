/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.testtool.storage.proxy;

import java.util.List;

import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.database.DatabaseStorage;
import nl.nn.testtool.storage.proofofmigration.ProofOfMigrationStorage;
import nl.nn.testtool.storage.xml.XmlStorage;

/**
 * Storage proxy class than can help Spring XML configuration to become more flexible. E.g. see #{testStorageActive} in
 * Frank!Framework's springIbisTestTool.xml
 * 
 * @author Jaco de Groot
 */
public class ProxyStorage implements CrudStorage, LogStorage {
	private @Setter Storage destination;

	@Override
	public void setName(String name) {
		destination.setName(name);
	}

	@Override
	public String getName() {
		return destination.getName();
	}

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		if (destination instanceof nl.nn.testtool.storage.file.Storage) {
			((nl.nn.testtool.storage.file.Storage)destination).setMetadataExtractor(metadataExtractor);
		} else if (destination instanceof nl.nn.testtool.storage.file.TestStorage) {
			((nl.nn.testtool.storage.file.TestStorage)destination).setMetadataExtractor(metadataExtractor);
		} else if (destination instanceof nl.nn.testtool.storage.memory.Storage) {
			((nl.nn.testtool.storage.memory.Storage)destination).setMetadataExtractor(metadataExtractor);
		} else if (destination instanceof DatabaseStorage) {
			((DatabaseStorage)destination).setMetadataExtractor(metadataExtractor);
		} else if (destination instanceof ProofOfMigrationStorage) {
			((ProofOfMigrationStorage)destination).setMetadataExtractor(metadataExtractor);
		} else if (destination instanceof nl.nn.testtool.storage.zip.Storage) {
			((nl.nn.testtool.storage.zip.Storage)destination).setMetadataExtractor(metadataExtractor);
		} else {
			throw new RuntimeException("Method setMetadataExtractor() not implemented for "
					+ destination.getClass().getName());
		}
	}

	public void init() throws StorageException {
		if (destination instanceof nl.nn.testtool.storage.file.Storage) {
			((nl.nn.testtool.storage.file.Storage)destination).init();
		} else if (destination instanceof nl.nn.testtool.storage.file.TestStorage) {
			((nl.nn.testtool.storage.file.TestStorage)destination).init();
		} else if (destination instanceof DatabaseStorage) {
			((DatabaseStorage)destination).init();
		} else if (destination instanceof ProofOfMigrationStorage) {
			((ProofOfMigrationStorage)destination).init();
		} else if (destination instanceof nl.nn.testtool.storage.diff.Storage) {
			((nl.nn.testtool.storage.diff.Storage)destination).init();
		} else if (destination instanceof XmlStorage) {
			((XmlStorage)destination).init();
		} else {
			throw new RuntimeException("Method setMetadataExtractor() not implemented for "
					+ destination.getClass().getName());
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
	public synchronized List getStorageIds() throws StorageException {
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
