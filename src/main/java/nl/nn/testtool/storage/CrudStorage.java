package nl.nn.testtool.storage;

import nl.nn.testtool.Report;

/**
 * Storage supporting Create, Read, Update and Delete actions.
 * 
 * @author Jaco de Groot
 */
public interface CrudStorage extends Storage {
	public void store(Report report) throws StorageException;

	public void update(Report report) throws StorageException;

	public void delete(Report report) throws StorageException;

}
