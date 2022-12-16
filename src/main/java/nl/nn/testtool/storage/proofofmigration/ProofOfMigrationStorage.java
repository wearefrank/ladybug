/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.testtool.storage.proofofmigration;

import java.lang.invoke.MethodHandles;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.database.DatabaseStorage;

/**
 * Utilize the proof of migration storage to use the Ladybug as a front-end for the proof of migration table. When
 * software is being replaced by other software (e.g. when Tibco BW is replaced by a Frank!) the new software component
 * can be deployed alongside the old component for some time and mirror all the calls to the old component and store
 * both the messages of the old and new component in the proof of migration table. Ladybug will group the records by
 * COMPONENT and CORRELATION_ID. Hence in the metadata table one record will be shown for the old component and one
 * record will be shown for the new component. When the values of the STATUS column for both records are not Success
 * their color will be red and the user can select both records and compare them.
 * 
 * @author Jaco de Groot
 */
//@Dependent disabled for Quarkus for now because of the use of JdbcTemplate
public class ProofOfMigrationStorage extends DatabaseStorage {
	private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String GROUP_BY = "group by COMPONENT, CORRELATION_ID";
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter List<String> columns;
	private @Setter boolean showErrorsOnly = false;

	@Override
	public String getName() {
		if (name == null) {
			return "proofOfMigrationStorage";
		} else {
			return name;
		}
	}

	public String getTable() {
		if (table == null) {
			return "PROOF_OF_MIGRATION";
		} else {
			return table;
		}
	}

	@Override
	public List<String> getMetadataNames() {
		return getColumns();
	}

	public List<String> getColumns() {
		if (columns == null) {
			return new ArrayList<String>(Arrays.asList(
					"ID", "TIMESTAMP", "COMPONENT", "ENDPOINT_NAME", "CORRELATION_ID", "NR OF CHECKPOINTS", "STATUS"));
		} else {
			return columns;
		}
	}

	public String getStorageIdColumn() {
		if (storageIdColumn == null) {
			return "ID";
		} else {
			return storageIdColumn;
		}
	}

	@Override
	public List<String> getIntegerColumns() {
		if (integerColumns == null) {
			return new ArrayList<String>(Arrays.asList("ID", "CHECKPOINT_NR"));
		} else {
			return integerColumns;
		}
	}

	@Override
	public List<String> getTimestampColumns() {
		if (timestampColumns == null) {
			return new ArrayList<String>(Arrays.asList("TIMESTAMP"));
		} else {
			return timestampColumns;
		}
	}

	public boolean isShowErrorsOnly() {
		return showErrorsOnly;
	}

	@Override
	protected void buildMetadataQuery(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues,
			List<String> rangeSearchValues,	SimpleDateFormat simpleDateFormat, StringBuilder query,
			List<Object> args, List<Integer> argTypes) throws StorageException {
		super.buildMetadataQuery(maxNumberOfRecords, metadataNames, searchValues, rangeSearchValues, simpleDateFormat,
				query, args, argTypes);
		if (log.isDebugEnabled()) {
			log.debug("Get metadata original query: " + query.toString());
			// E.g.: select ID, TIMESTAMP, COMPONENT, ENDPOINT_NAME, CORRELATION_ID, NR OF CHECKPOINTS, STATUS from PROOF_OF_MIGRATION order by ID desc limit 10
			// The new metadata query will be logged by the super class
			// E.g.: select min(ID) as min_id, min(TIMESTAMP), COMPONENT, min(concat(CHECKPOINT_NR, '. ', ENDPOINT_NAME)), CORRELATION_ID, count(CHECKPOINT_NR), min(STATUS) from PROOF_OF_MIGRATION where STATUS != 'Success' group by COMPONENT, CORRELATION_ID order by min_id desc limit 10
		}
		replace(query, "ID", "min(ID) as min_id");
		replace(query, "TIMESTAMP", "min(TIMESTAMP)");
		replace(query, "ENDPOINT_NAME", "min(concat(CHECKPOINT_NR, '. ', ENDPOINT_NAME))");
		replace(query, "NR OF CHECKPOINTS", "count(CHECKPOINT_NR)");
		replace(query, "STATUS", "min(STATUS)");
		replace(query, "order by ID" , GROUP_BY + " order by min_id");
		if (isShowErrorsOnly()) {
			replace(query, "group by" , "where STATUS != 'Success' group by");
		}
	}

	/*
	 * Make sure the record for old component and the record for the new component (that have the same correlation id)
	 * are displayed directly below each other. The order by ID in the query doesn't guarantee that.
	 */
	@Override
	protected void postProcessMetadataResult(List<List<Object>> metadata, int maxNumberOfRecords,
			List<String> metadataNames, List<String> searchValues, int metadataValueType) {
		int correlationIdPosition = -1;
		int position = 0;
		for (String metadataName : metadataNames) {
			if (metadataName.equals("CORRELATION_ID")) {
				correlationIdPosition = position;
			}
			position++;
		}
		if (correlationIdPosition == -1) {
			log.warn("Could not find field CORRELATION_ID");
		} else {
			for (int i = metadata.size() - 1; i > 0; i--) {
				Object correlationId = metadata.get(i).get(correlationIdPosition);
				if (!correlationId.equals(metadata.get(i - 1).get(correlationIdPosition))
						&& !(i < metadata.size() - 1
								&& correlationId.equals(metadata.get(i + 1).get(correlationIdPosition)))) {
					for (int j = i - 2; j > -1; j--) {
						if (correlationId.equals(metadata.get(j).get(correlationIdPosition))) {
							List<Object> record = metadata.remove(j);
							metadata.add(i - 1, record);
							j = 0;
						}
					}
				}
			}
		}
	}

	@Override
	protected void buildSizeQuery(StringBuilder query) throws StorageException {
		super.buildSizeQuery(query);
		if (log.isDebugEnabled()) {
			log.debug("Get size original query: " + query.toString());
			// E.g.: select count(*) from PROOF_OF_MIGRATION
			// The new size query will be logged by the super class
			// E.g.: select count(*) from (select COMPONENT, CORRELATION_ID from PROOF_OF_MIGRATION group by COMPONENT, CORRELATION_ID) as DUMMY
		}
		replace(query, "from", "from (select " + GROUP_BY.substring(9) + " from");
		query.append(" " + GROUP_BY + ") as DUMMY");
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		String query =
				"select CORRELATION_ID, COMPONENT, CHECKPOINT_NR, ENDPOINT_NAME, ACTION, MESSAGE, TIMESTAMP from "
				+ getTable() + " where CORRELATION_ID = (select CORRELATION_ID from " + getTable() + " where "
				+ getStorageIdColumn() + " = ?) and COMPONENT = (select COMPONENT from " + getTable() + " where "
				+ getStorageIdColumn() + " = ?) order by CHECKPOINT_NR";
		log.debug("Get report query: " + query);
		List<List<Object>> result = getJdbcTemplate().query(query, new Object[]{storageId, storageId},
				new int[] {Types.INTEGER, Types.INTEGER},
				(resultSet, rowNum) -> {
					List<Object> list = new ArrayList<Object>();
					list.add(resultSet.getString(1));
					list.add(resultSet.getString(2));
					list.add(resultSet.getString(3));
					list.add(resultSet.getString(4));
					list.add(resultSet.getString(5));
					list.add(resultSet.getString(6));
					list.add(resultSet.getString(7));
					return list;
				});
		Report report = new Report();
		report.setTestTool(testTool);
		report.setStorage(this);
		report.setStorageId(storageId);
		report.setName((String)result.get(0).get(1));
		report.setCorrelationId((String)result.get(0).get(0));
		int level = 0;
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		Checkpoint checkpoint;
		for (List<Object> list : result) {
			int checkpointType = Checkpoint.TYPE_ABORTPOINT;
			if ("request".equals(list.get(4))) {
				checkpointType = Checkpoint.TYPE_STARTPOINT;
			} else if ("response".equals(list.get(4))) {
				checkpointType = Checkpoint.TYPE_ENDPOINT;
			}
			checkpoint = new Checkpoint(report, null, null, list.get(2) + ". " + list.get(3), checkpointType, level);
			checkpoint.setMessage(list.get(5));
			checkpoints.add(checkpoint);
			if ("request".equals(list.get(4))) {
				level++;
			} else if ("response".equals(list.get(4))) {
				level--;
			}
		}
		checkpoint = new Checkpoint(report, null, null, "Timestamps", Checkpoint.TYPE_INFOPOINT, 0);
		StringBuilder message = new StringBuilder();
		for (List<Object> list : result) {
			message.append(list.get(2) + ". " + list.get(6) + "\n");
		}
		checkpoint.setMessage(message.toString());
		checkpoints.add(checkpoint);
		report.setCheckpoints(checkpoints);
		return report;
	}

	@Override
	public void store(Report report) throws StorageException {
		throw new StorageException("Not implemented!");
	}

	@Override
	public void update(Report report) throws StorageException {
		throw new StorageException("Not implemented!");
	}

	@Override
	public void delete(Report report) throws StorageException {
		throw new StorageException("Not implemented!");
	}

	@Override
	public List<Integer> getStorageIds() throws StorageException {
		throw new StorageException("Not implemented!");
	}

	private void replace(StringBuilder stringBuilder, String from, String to) {
		char prefix = ' ';
		char postfix = ',';
		int i = stringBuilder.indexOf(prefix + from + postfix);
		if (i == -1) {
			postfix = ' ';
			i = stringBuilder.indexOf(prefix + from + postfix);
		}
		if (i > -1) {
			stringBuilder.replace(i, i + from.length() + 2, prefix + to + postfix);
		}
	}

}
