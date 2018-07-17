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
	public List<List<String>> getMetadata(int maxNumberOfRecords, List<String> metadataNames,
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
