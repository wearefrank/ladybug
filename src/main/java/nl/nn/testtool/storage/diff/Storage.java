package nl.nn.testtool.storage.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

// TODO of gewoon onderdeel van memory.Storage class maken (als filename gezet dan lezen en schrijven naar bestand)?
public class Storage extends nl.nn.testtool.storage.memory.Storage {
	private static Logger log = LogUtil.getLogger(Storage.class);
	private String reportsDirectory;

	public void setReportsDirectory(String reportsDirectory) {
		this.reportsDirectory = reportsDirectory;
	}

	public void init() throws StorageException {
		readReports();
	}

	public synchronized void store(Report report) {
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

	public void storeWithoutException(Report report) {
		super.storeWithoutException(report);
		writeReports();
	}

	private void readReports() {
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
			Report report = (Report)reportsList.get(0);
			report.setStorage(this);
			report.setStorageId(new Integer(storageId++));
			storageIds.add(report.getStorageId());
			reports.put(report.getStorageId(), report);
			metadata.add(new HashMap());
		}
	}

	private void writeReports() {
		Iterator iterator = storageIds.iterator();
		while (iterator.hasNext()) {
			FileOutputStream fileOutputStream = null;
			try {
				Integer storageId = (Integer)iterator.next();
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
