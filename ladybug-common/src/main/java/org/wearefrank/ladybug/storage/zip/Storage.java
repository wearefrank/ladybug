/*
   Copyright 2020-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.storage.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.memory.MemoryStorage;

public class Storage extends MemoryStorage {
	private File file = new File("C:\\Temp\\tt.zip");

	@Override
	public synchronized void store(Report report) throws StorageException {
		ZipEntry zipEntry = new ZipEntry(report.getStorageId().toString());
		ZipOutputStream zipOutputStream = null;
		try {
			zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			// TODO iets mee doen
			e.printStackTrace();
		}
		try {
			zipOutputStream.putNextEntry(zipEntry);
		} catch (IOException e) {
			// TODO iets mee doen
			e.printStackTrace();
		}
		super.store(report);
	}

	public void update(Report report) throws StorageException {
		throw new StorageException("Not implemented (yet)");
	}

	public void delete(Report report) throws StorageException {
		throw new StorageException("Not implemented (yet)");
	}

	@Override
	public synchronized Report getReport(Integer storageId) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
		} catch (ZipException e) {
			// TODO iets mee doen
			e.printStackTrace();
		} catch (IOException e) {
			// TODO iets mee doen
			e.printStackTrace();
		}
		ZipEntry zipEntry = zipFile.getEntry(storageId.toString());
//		zipEntry.
		return (Report) reports.get(storageId);
	}

	@Override
	public void clear() throws StorageException {
		throw new StorageException("Not implemented (yet)");
	}

}
