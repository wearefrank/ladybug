/*
   Copyright 2020, 2022-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.storage.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.memory.MemoryCrudStorage;

// TODO of gewoon onderdeel van memory storage maken (als filename gezet dan lezen en schrijven naar bestand)?
public class Storage extends MemoryCrudStorage {
	private String reportsDirectory;

	public void setReportsDirectory(String reportsDirectory) {
		this.reportsDirectory = reportsDirectory;
	}

	@PostConstruct
	public void init() throws StorageException {
		readReports();
	}

	public synchronized void store(Report report) throws StorageException {
		super.store(report);
		writeReports();
	}

	public void update(Report report) throws StorageException {
		super.update(report);
		writeReports();
	}

	public void delete(Report report) throws StorageException {
		super.delete(report);
		writeReports();
	}

	private void readReports() throws StorageException {
		File directory = new File(reportsDirectory);
		File[] files = directory.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.startsWith("report-") && name.endsWith(".ttr")) {
							return true;
						} else {
							return false;
						}
					}
				}
		);
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// TODO iets mee doen
				e.printStackTrace();
			}
			List reportsList = new ArrayList();
//			String errorMessage = Echo2Application.getReports(fileInputStream, reportsList, log);
			// TODO checken op errorMessage?
			Report report = (Report) reportsList.get(0);
			report.setStorage(this);
			report.setStorageId(getNewStorageId());
			storageIds.add(report.getStorageId());
			reports.put(report.getStorageId(), report);
		}
	}

	private void writeReports() {
		Iterator iterator = storageIds.iterator();
		while (iterator.hasNext()) {
			FileOutputStream fileOutputStream = null;
			try {
				Integer storageId = (Integer) iterator.next();
				fileOutputStream = new FileOutputStream(reportsDirectory + "/report-" + storageId + ".ttr");
//				fileOutputStream.write(Echo2Application.getReportBytes((Report)reports.get(storageId)));
			} catch (FileNotFoundException e) {
				// TODO nog iets mee doen
				e.printStackTrace();
			} catch (IOException e) {
				// TODO nog iets mee doen
				e.printStackTrace();
			} finally {
				if (fileOutputStream != null) {
					try {
						fileOutputStream.close();
					} catch (IOException e) {
						// TODO nog iets mee doen? alleen loggen?
						e.printStackTrace();
					}
				}
			}
		}
	}

}
