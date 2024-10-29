/*
   Copyright 2020-2022 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.util.List;

import nl.nn.testtool.Report;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
public interface Storage {
    public static final int FILTER_RESET = 0;
    public static final int FILTER_SELECT = 1;

	public void setName(String name);

	public String getName();

	public int getSize() throws StorageException;

	public List getStorageIds() throws StorageException;

	default boolean isCrudStorage() {
		return this instanceof CrudStorage;
	}

	/**
	 * Get a list of metadata records. A metadata record is also a list and
	 * contains the metadata for a specific report.
	 * 
	 * @param maxNumberOfRecords  the maximum number of records to return
	 * @param metadataNames ...
	 * @param searchValues see {@link SearchUtil}
	 * @param metadataValueType ...
	 * @throws StorageException ...
	 * @return ...
	 */
	public List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames,
			List<String> searchValues, int metadataValueType) throws StorageException;

	/**
	 * Get a report with the specified id. Returns null when the report could not be found. The report returned should
	 * always be a new object so different calls for the same storageId will not get a reference to the same object* and
	 * interfere with each other when they change the returned report. 
	 * 
	 * @param storageId ...
	 * @throws StorageException ...
	 * @return ...
	 */
	public Report getReport(Integer storageId) throws StorageException;

	public void clear() throws StorageException;

	public void close();

	public int getFilterType(String column);

	public List getFilterValues(String column) throws StorageException;

	public String getUserHelp(String column);
}
