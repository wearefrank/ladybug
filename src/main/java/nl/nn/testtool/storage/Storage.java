/*
   Copyright 2018 Nationale-Nederlanden

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

	/**
	 * Get a list of metadata records. A metadata record is also a list and
	 * contains the metadata for a specific report.
	 * 
	 * @param maxNumberOfRecords  the maximum number of records to return
	 */
	// TODO bij de implementaties ook numberOfRecords -> maxNumberOfRecords
	// TODO andere params ook documenteren (voor searchValues verwijzen naar SearchUtil.matches(resultRecord, searchValues)?)
	public List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames,
			List<String> searchValues, int metadataValueType) throws StorageException;

	// TODO een StorageByMetadata en StorageByFolders maken?
	public List getTreeChildren(String path);
	public List getStorageIds(String path) throws StorageException;

	/**
	 * Get a report with the specified id. Returns null when the report could
	 * not be found.
	 */
	public Report getReport(Integer storageId) throws StorageException;
	
	public void close();

	public int getFilterType(String column);

	public List getFilterValues(String column) throws StorageException;

	public String getUserHelp(String column);
}
