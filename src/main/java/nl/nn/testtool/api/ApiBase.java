package nl.nn.testtool.api;

import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;

import java.util.Map;

public abstract class ApiBase {

	protected static Map<String, Storage> storages;
	protected static TestTool testTool;

	protected void setStorages(Map<String, Storage> map) {
		storages = map;
	}

	protected static Storage getStorage(String storage) throws ApiException {
		if (!storages.containsKey(storage))
			throw new ApiException("Given storage [" + storage + "] was not found.");
		return storages.get(storage);
	}

	public static void setTestTool(TestTool tool) {
		testTool = tool;
	}
}
