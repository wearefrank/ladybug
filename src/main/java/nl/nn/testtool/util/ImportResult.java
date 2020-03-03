package nl.nn.testtool.util;

public class ImportResult {
	int oldStorageId;
	int newStorageId;
	String errorMessage;

	public int getOldStorageId() {
		return oldStorageId;
	}
	
	public int getNewStorageId() {
		return newStorageId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}