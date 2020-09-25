/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.testtool.storage.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.CSVReader;
import nl.nn.testtool.util.SearchUtil;

import org.apache.log4j.Logger;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestStorage implements nl.nn.testtool.storage.CrudStorage {
//TODO TestStorage -> CrudStorage?
	private String name;
	private Reader reader[] = new Reader[2];
	private Writer writer[] = new Writer[2];
	private short active;

// TODO als je public weg laat, krijg je deze bij Download all in run pane en volgens mail van Peter bij openen van een rapport (zie ook constructor Storage):
//	java.lang.IllegalAccessException: Class sun.reflect.misc.Trampoline can not access a member of class nl.nn.testtool.storage.file.TestStorage with modifiers ""
//	Continuing ...
//	java.lang.RuntimeException: failed to evaluate: <unbound>=Class.new();
//	Continuing ...
	public TestStorage() {
		reader[0] = new Reader();
		reader[1] = new Reader();
		writer[0] = new Writer();
		writer[1] = new Writer();
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
		
	public void setReportsFilename(String reportsFilename) {
		reader[0].setReportsFilename(reportsFilename + ".a");
		reader[1].setReportsFilename(reportsFilename + ".b");
		writer[0].setReportsFilename(reportsFilename + ".a");
		writer[1].setReportsFilename(reportsFilename + ".b");
	}

	public void setMetadataFilename(String metadataFilename) {
		reader[0].setMetadataFilename(metadataFilename + ".a");
		reader[1].setMetadataFilename(metadataFilename + ".b");
		writer[0].setMetadataFilename(metadataFilename + ".a");
		writer[1].setMetadataFilename(metadataFilename + ".b");
	}

//	public void setMaximumFileSize(long maximumFileSize) {
//		writer.setMaximumFileSize(maximumFileSize);
//	}
//
//	public void setMaximumBackupIndex(int maximumBackupIndex) {
//		reader.setMaximumBackupIndex(maximumBackupIndex);
//		writer.setMaximumBackupIndex(maximumBackupIndex);
//	}
//
	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		reader[0].setMetadataExtractor(metadataExtractor);
		reader[1].setMetadataExtractor(metadataExtractor);
		writer[0].setMetadataExtractor(metadataExtractor);
		writer[1].setMetadataExtractor(metadataExtractor);
	}

	/**
	 * Set the metadata that should be written to file. At least storageId and
	 * storageSize should be set otherwise it's not possible to read the reports
	 * from file. The specified metadata will be written to file when a
	 * report is stored. When the metadata is requested it is read from file.
	 * For additional metadata (not available in the file) the metadata
	 * extractor object is called, this will need the report to be read from
	 * file. The metadata is cached in memory. For better performance at report
	 * generation time don't make metadata persistent that takes a lot of
	 * performance to extract from the report. For better performance when the
	 * metadata is read you could do the opposite.
	 * 
	 * @param metadataNames
	 */
	public void setPersistentMetadata(List metadataNames) {
		writer[0].setPersistentMetadata(metadataNames);
		writer[1].setPersistentMetadata(metadataNames);
	}

	public void init() throws StorageException {
		reader[0].init();
		reader[1].init();
		writer[0].init(reader[0].getStorageIds(writer[0].getMetadataFileLastModified()));
		writer[1].init(reader[1].getStorageIds(writer[1].getMetadataFileLastModified()));
		if (reader[1].getStorageIds(writer[1].getMetadataFileLastModified()).size()
				> reader[0].getStorageIds(writer[0].getMetadataFileLastModified()).size()) {
			active = 1;
		} else {
			active = 0;
		}
	}

	public void store(Report report) throws StorageException {
		report.setStorage(this);
		writer[active].store(report, false);
	}

	public void storeWithoutException(Report report) {
		writer[active].storeWithoutException(report, false);
	}

	public int getSize() throws StorageException {
		// TODO make a faster implementation?
		return getStorageIds().size();
	}

	public List getStorageIds() throws StorageException {
		return reader[active].getStorageIds(writer[active].getMetadataFileLastModified());
	}

	public void update(Report report) throws StorageException {
		update(report, false);
	}

	public void delete(Report report) throws StorageException {
		update(report, true);
	}

	private void update(Report report, boolean delete) throws StorageException {
		// TODO synchronized maken een bean property updatesEnabled maken en alleen in dat geval synchronized doen? als updatesEnabled dan geen logrotatie toestaan? documenteren dat file storage niet zo snel is met updates en deletes?
		short source = active;
		short destination;
		if (source == 0) {
			destination = 1;
		} else {
			destination = 0;
		}
		List storageIds = reader[source].getStorageIds(writer[source].getMetadataFileLastModified());
		Collections.sort(storageIds);
		Iterator iterator = storageIds.iterator();
		while (iterator.hasNext()) {
			Integer storageId = (Integer)iterator.next();
			if (storageId.equals(report.getStorageId())) {
				if (!delete) {
					writer[destination].store(report, true);
				}
			} else {
				byte[] reportBytes = reader[source].getReportBytes(storageId);
				List persistentMetadata = writer[destination].getPersistentMetadata();
				List searchValues = new ArrayList();
				searchValues.add("(" + storageId + ")"); // TODO een getMetadata maken die op exacte waarden kan zoeken zodat je er geen reg. expr. van hoeft te maken?
				for (int i = 1; i < persistentMetadata.size(); i++) {
					searchValues.add(null); // TODO is dit nodig?
				}
// TODO als je numberOfRecords op 1 zet krijg je geen resultaat, klopt dat wel?
				List metadata = reader[source].getMetadata(-1,
						persistentMetadata, searchValues ,
						MetadataExtractor.VALUE_TYPE_STRING,
						writer[source].getMetadataFileLastModified());
				writer[destination].store(report.getName(), reportBytes, (List)metadata.get(0));
			}
		}
		writer[destination].latestStorageId = writer[source].latestStorageId;
		// TODO nog checken of desitnation storage goed gevuld is?
		writer[source].clear();
		reader[source].clear();
		active = destination;
	}

	public void readFile(File file) {//TODO remove
		try {
			FileReader fileReader = new FileReader(file);
			char[] cbuf = new char[(int)file.length()];
			try {
				fileReader.read(cbuf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	// TODO ook een methode die met arrays werkt zodat je niet allemaal lists aan hoeft te maken? of een arrayToList methode gebruiken? een standaard methode van Collections o.i.d.?
	// TODO metadataValueType zou eigenlijk ook een list moeten zijn?!
	public List getMetadata(int numberOfRecords, List metadataNames,
			List searchValues, int metadataValueType) throws StorageException {
		return reader[active].getMetadata(numberOfRecords, metadataNames, searchValues,
				metadataValueType, writer[active].getMetadataFileLastModified());
	}

	// TODO moet dit niet een StorageByMetadata worden? dus deze methode weg?
	// TODO getPathComponents noemen?
	// TODO met (eigen) tree node objecten werken?
	public List getTreeChildren(String path) {
		List folders = new ArrayList();
		if ("/".equals(path)) {
			folders.add("testje1");
			folders.add("testje2");
			folders.add("testje3");
		}
		return folders;
	}
	// TODO getMetadata(String path) noemen?
	public List getStorageIds(String path) throws StorageException {
		return getStorageIds();

		// Bij ieder report van resultaat /test/* bij path /test/ prefix
		// verwijderen. Als je dan nog / in path hebt dan eerste gedeelte tot
		// die / gebruiken als folder naam om weer te geven en anders het
		// report weergeven
//		try {
//			metadata = storage.getMetadata(numberOfRecords, metadataNames,
//					searchValues, MetadataExtractor.VALUE_TYPE_GUI);
//		} catch(StorageException storageException) {
//			displayAndLogError(storageException);
//		}

	}

	public Report getReport(Integer storageId) throws StorageException {
		Report report = reader[active].getReport(storageId);
		if (report != null) {
			report.setStorage(this);
		}
		return report;
	}

	public void close() {
		writer[0].close();
		writer[1].close();
	}

	protected static void closeCSVReader(CSVReader csvReader, String action, Logger log) {
		try {
			csvReader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeReader(java.io.Reader reader, String action, Logger log) {
		try {
			reader.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeInputStream(InputStream inputStream, String action, Logger log) {
		try {
			inputStream.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeOutputStream(OutputStream outputStream, String action, Logger log) {
		try {
			outputStream.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}
	
	protected static void closeOutputStreamWriter(OutputStreamWriter outputStreamWriter, String action, Logger log) {
		try {
			outputStreamWriter.close();
		} catch(IOException e) {
			log.warn("IOException " + action, e);
		}
	}

	protected static void logAndThrow(Logger log, String message) throws StorageException {
		log.error(message);
		throw new StorageException(message);
	}

	protected static void logAndThrow(Logger log, Exception e, String message) throws StorageException {
		message = message + ": " + e.getMessage();
		log.error(message, e);
		throw new StorageException(message, e);
	}

	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}
}
