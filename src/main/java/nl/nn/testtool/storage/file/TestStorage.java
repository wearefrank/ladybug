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
package nl.nn.testtool.storage.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
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

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
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

	@Inject
	@Autowired
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
	 * @param metadataNames ...
	 */
	public void setPersistentMetadata(List<String> metadataNames) {
		writer[0].setPersistentMetadata(metadataNames);
		writer[1].setPersistentMetadata(metadataNames);
	}

	/**
	 * The metadataNames to be shown in the debug tab are usually not a good default for persistent metadata of the test
	 * storage but when injected it gives a chance to set the proper defaults
	 * 
	 * @param metadataNames ...
	 */
	@Inject
	@Resource(name="metadataNames")
	public void setMetadataNames(List<String> metadataNames) {
		if (writer[0].getPersistentMetadata() == null) {
			metadataNames = new ArrayList<>();
			metadataNames.add("storageId");
			metadataNames.add("storageSize");
			metadataNames.add("path");
			metadataNames.add("name");
			metadataNames.add("description");
			writer[0].setPersistentMetadata(metadataNames);
			writer[1].setPersistentMetadata(metadataNames);
		}
	}

	@PostConstruct
	public void init() throws StorageException {
		reader[0].init();
		reader[1].init();
		writer[0].init(reader[0].getStorageIds(writer[0].getMetadataFileLastModified(),
				writer[0].getSynchronizeRotate()));
		writer[1].init(reader[1].getStorageIds(writer[1].getMetadataFileLastModified(),
				writer[1].getSynchronizeRotate()));
		int size0 = reader[0].getStorageIds(writer[0].getMetadataFileLastModified(), writer[0].getSynchronizeRotate())
				.size();
		int size1 = reader[1].getStorageIds(writer[1].getMetadataFileLastModified(), writer[1].getSynchronizeRotate())
				.size();
		if (size1 > size0) {
			active = 1;
		} else {
			active = 0;
		}
	}

	@Override
	public Report store(Report report) throws StorageException {
		report.setStorage(this);
		writer[active].store(report, false);
		return report;
	}

	@Override
	public int getSize() throws StorageException {
		// TODO make a faster implementation?
		return getStorageIds().size();
	}

	@Override
	public List getStorageIds() throws StorageException {
		return reader[active].getStorageIds(writer[active].getMetadataFileLastModified(),
				writer[active].getSynchronizeRotate());
	}

	@Override
	public void update(Report report) throws StorageException {
		update(report, false);
	}

	@Override
	public void delete(Report report) throws StorageException {
		update(report, true);
	}

	private void update(Report report, boolean delete) throws StorageException {
		short source = active;
		short destination;
		if (source == 0) {
			destination = 1;
		} else {
			destination = 0;
		}
		List storageIds = reader[source].getStorageIds(writer[source].getMetadataFileLastModified(),
				writer[source].getSynchronizeRotate());
		Collections.sort(storageIds);
		Iterator iterator = storageIds.iterator();
		while (iterator.hasNext()) {
			Integer storageId = (Integer)iterator.next();
			if (storageId.equals(report.getStorageId())) {
				if (!delete) {
					writer[destination].store(report, true);
				}
			} else {
				byte[] reportBytes = reader[source].getReportBytes(storageId, writer[source].getSynchronizeRotate());
				List persistentMetadata = writer[destination].getPersistentMetadata();
				List<String> searchValues = new ArrayList<>();
				searchValues.add("(" + storageId + ")"); // TODO een getMetadata maken die op exacte waarden kan zoeken zodat je er geen reg. expr. van hoeft te maken?
				for (int i = 1; i < persistentMetadata.size(); i++) {
					searchValues.add(null); // TODO is dit nodig?
				}
// TODO als je maxNumberOfRecords op 1 zet krijg je geen resultaat, klopt dat wel?
				List metadata = reader[source].getMetadata(-1,
						persistentMetadata, searchValues ,
						MetadataExtractor.VALUE_TYPE_STRING,
						writer[source].getMetadataFileLastModified(),
						writer[source].getSynchronizeRotate());
				writer[destination].store(report.getName(), reportBytes, (List)metadata.get(0));
			}
		}
		writer[destination].latestStorageId = writer[source].latestStorageId;
		writer[source].clear();
		reader[source].clear();
		active = destination;
	}

	@Override
	public List getMetadata(int maxNumberOfRecords, List metadataNames,
			List searchValues, int metadataValueType) throws StorageException {
		return reader[active].getMetadata(maxNumberOfRecords, metadataNames, searchValues,
				metadataValueType, writer[active].getMetadataFileLastModified(), writer[active].getSynchronizeRotate());
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		Report report = reader[active].getReport(storageId, writer[active].getSynchronizeRotate());
		if (report != null) {
			report.setStorage(this);
		}
		return report;
	}

	@Override
	public void clear() throws StorageException {
		writer[active].clear();
	}

	@Override
	public void close() {
		writer[0].close();
		writer[1].close();
	}

	@Override
	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	@Override
	public List getFilterValues(String column) throws StorageException {
		return null;
	}

	@Override
	public String getUserHelp(String column) {
		return SearchUtil.getUserHelp();
	}
}
