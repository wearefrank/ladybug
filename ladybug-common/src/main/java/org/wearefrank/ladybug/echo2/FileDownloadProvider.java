/*
   Copyright 2020, 2022, 2024, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.echo2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nextapp.echo2.app.filetransfer.DownloadProvider;

/**
 * @author Jaco de Groot
 */
public class FileDownloadProvider implements DownloadProvider {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private File file;
	private String fileName;

	public FileDownloadProvider(File file, String fileName) {
		this.file = file;
		this.fileName = fileName;
	}

	public String getContentType() {
		return "application/octet-stream";
	}

	public String getFileName() {
		return fileName;
	}

	public int getSize() {
		return (int) file.length();
	}

	public void writeFile(OutputStream outputStream) throws IOException {
		log.debug("Start download for file: " + fileName);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int i = fileInputStream.read(buffer);
			while (i != -1) {
				outputStream.write(buffer, 0, i);
				i = fileInputStream.read(buffer);
			}
		} catch (IOException e) {
			// Log exception.
			log.error("IOException during download", e);
			// Throw exception as it is allowed by the method signature.
			throw e;
		} finally {
			try {
				outputStream.close();
				fileInputStream.close();
				file.delete();
			} catch (IOException e) {
				// Log exception.
				log.error("IOException closing streams", e);
				// Throw exception as it is allowed by the method signature.
				throw e;
			}
		}
	}

}
