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
package nl.nn.testtool.echo2.util;

import java.io.InputStream;

import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.util.Import;

import org.apache.logging.log4j.Logger;

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
