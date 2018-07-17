package nl.nn.testtool.echo2.util;

import java.io.InputStream;

import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.util.Import;

import org.apache.log4j.Logger;

public class Upload {

	public static String upload(String filename, InputStream inputStream, CrudStorage storage, Logger log) {
		log.debug("Process upload of file: " + filename);
		if (filename.endsWith(".zip")) {
			return Import.importZip(inputStream, storage, log);
		} else if (filename.endsWith(".ttr")) {
			return Import.importTtr(inputStream, storage, log);
		} else {
			return "File doesn't have a known file extension: " + filename;
		}
	}

}
