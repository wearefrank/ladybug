/*
   Copyright 2020, 2022, 2024 WeAreFrank!, 2018, 2019 Nationale-Nederlanden

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
package nl.nn.testtool.echo2.reports;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nextapp.echo2.app.filetransfer.UploadEvent;
import nextapp.echo2.app.filetransfer.UploadListener;
import nl.nn.testtool.echo2.test.TestComponent;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.memory.MemoryCrudStorage;

/**
 * @author Jaco de Groot
 */
public class ReportUploadListener implements UploadListener {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	ReportsComponent reportsComponent;
	TestComponent testComponent;
	CrudStorage storage;

	public ReportUploadListener() {
	}

	public void setReportsComponent(ReportsComponent reportsComponent) {
		this.reportsComponent = reportsComponent;
	}

	public void setTestComponent(TestComponent testComponent) {
		this.testComponent = testComponent;
	}

	public void setStorage(CrudStorage storage) {
		this.storage = storage;
	}

	public void fileUpload(UploadEvent uploadEvent) {
		List reports = new ArrayList();
//		String errorMessage = DownUpLoad.getReports(uploadEvent.getFileName(), uploadEvent.getInputStream(), reports, log);
//		// TODO checken of errorMessage != null?
//		for (int i = 0; i < reports.size(); i++) {
//			Report report = (Report)reports.get(i);
//			if (reportsComponent != null) {
//				reportsComponent.openReport(report, true);
//			} else {
//				try {
//					storage.store(report);
//				} catch (StorageException e) {
//					// TODO iets doen, errorMessage vullen?
//					e.printStackTrace();
//				}
//			}
//		}
		CrudStorage storage = null;
		if (reportsComponent != null) {
			// TODO in reportsComponent memory storage bijhouden en gebruiken
			// voor download en upload (niet telkens nieuwe aanmaken maar
			// synchroon houden met open reports)?
			storage = new MemoryCrudStorage();
		}
		if (testComponent != null) {
			storage = this.storage;
		}
		String errorMessage = Upload.upload(uploadEvent.getFileName(), uploadEvent.getInputStream(), storage, log);
		if (reportsComponent != null) {
			try {
				List storageIds = storage.getStorageIds();
				for (int i = storageIds.size() - 1; i > -1; i--) {
					reportsComponent.openReport(storage.getReport((Integer)storageIds.get(i)),
							ReportsComponent.OPEN_REPORT_ALLOWED, false, true);
				}
			} catch (StorageException e) {
				// TODO iets doen, errorMessage vullen?
				e.printStackTrace();
			}
		}

		if (errorMessage != null) {
			// TODO generieker maken zodat het ook voor TestComponent werkt
			if (reportsComponent != null) {
				reportsComponent.displayAndLogError(errorMessage);
			}
			if (testComponent != null) {
				testComponent.displayAndLogError(errorMessage);
			}
		}
		// TODO generieker maken zodat het ook voor TestComponent werkt
		if (reportsComponent != null) {
			reportsComponent.getUploadOptionsWindow().setVisible(false);
		}
		if (testComponent != null) {
			testComponent.getUploadOptionsWindow().setVisible(false);
			testComponent.refresh();
		}
	}

	public void invalidFileUpload(UploadEvent uploadEvent) {
		String message = "Invalid file upload: " + uploadEvent.getFileName()
				+ ", " + uploadEvent.getContentType() + ", " + uploadEvent.getSize();
		log.error(message);
		// TODO generieker maken zodat het ook voor TestComponent werkt
		if (reportsComponent != null) {
			reportsComponent.displayError(message);
			reportsComponent.getUploadOptionsWindow().setVisible(false);
		}
	}

}
