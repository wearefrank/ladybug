/*
   Copyright 2022-2023 WeAreFrank!

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
	// Use i. (instead of m.) to help the query optimizer to start with the IDS table instead of the MESSAGES table
	private static final String GROUP_BY = "group by COMPONENT, i.CORRELATION_ID";
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter List<String> columns;
	private @Setter boolean showErrorsOnly = false;
	// Number and timestamp columns enable range searching
	protected @Setter List<String> integerColumns;
	protected @Setter List<String> longColumns;
	protected @Setter List<String> timestampColumns;

	@Override
	public String getName() {
		if (name == null) {
			return "proofOfMigrationStorage";
		} else {
			return name;
		}
	}

	@Override
	public String getTable() {
		if (table == null) {
			return "MESSAGES";
		} else {
			return table;
		}
	}

	@Override
	public boolean isCheckTableExists() {
		if (checkTableExists == null) {
			return false;
		} else {
			return checkTableExists;
		}
	}

	@Override
	public List<String> getMetadataNames() {
		return getColumns();
	}

	public List<String> getColumns() {
		if (columns == null) {
			return new ArrayList<String>(Arrays.asList(
					"ID", "TIMESTAMP", "COMPONENT", "ENDPOINT NAME", "CONVERSATION ID", "CORRELATION ID", "NR OF CHECKPOINTS", "STATUS"));
		} else {
			return columns;
		}
	}

	@Override
	public String getStorageIdColumn() {
		if (storageIdColumn == null) {
			return "ID";
		} else {
			return storageIdColumn;
		}
	}

	@Override
	public long getMaxStorageSize() {
		if (maxStorageSize == null) {
			return -1L;
		} else {
			return maxStorageSize;
		}
	}

	@Override
	public boolean isInteger(String metadataName) {
		if (integerColumns == null) {
			return metadataName.equals("ID") || metadataName.equals("NR OF CHECKPOINTS");
		} else {
			return integerColumns.contains(metadataName);
		}
	}

	@Override
	public boolean isLong(String metadataName) {
		if (integerColumns == null) {
			return false;
		} else {
			return integerColumns.contains(metadataName);
		}
	}

	@Override
	public boolean isTimestamp(String metadataName) {
		if (timestampColumns == null) {
			return metadataName.equals("TIMESTAMP");
		} else {
			return timestampColumns.contains(metadataName);
		}
	}

	public boolean isShowErrorsOnly() {
		return showErrorsOnly;
	}

	@Override
	protected void buildMetadataQuery(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues,
			List<String> rangeSearchValues, StringBuilder query, List<Object> args, List<Integer> argTypes)
			throws StorageException {
		super.buildMetadataQuery(maxNumberOfRecords, metadataNames, searchValues, rangeSearchValues, query, args,
				argTypes);
		if (log.isDebugEnabled()) {
			log.debug("Get metadata original query: " + query.toString());
			// E.g.: select ID, TIMESTAMP, COMPONENT, ENDPOINT_NAME, CONVERSATION_ID, CORRELATION_ID, NR OF CHECKPOINTS, STATUS from MESSAGES order by ID desc limit 10
			// The new metadata query will be logged by the super class
			// E.g.: select min(m.ID), i.TIMESTAMP, COMPONENT, min(concat(CHECKPOINT_NR, '. ', ENDPOINT_NAME)), i.ORIGINAL_ID, i.CORRELATION_ID, count(CHECKPOINT_NR), min(m.STATUS) from MESSAGES m, IDS i where m.CORRELATION_ID=i.CORRELATION_ID group by COMPONENT, i.CORRELATION_ID order by i.TIMESTAMP desc, i.CORRELATION_ID limit 10
		}
		// Test by selecting Proof of migration (errors) view in the Ladybug test webapp
		replace(query, "ID", "min(m.ID)");
		replace(query, "TIMESTAMP", "i.TIMESTAMP");
		replace(query, "ENDPOINT NAME", "min(concat(m.CHECKPOINT_NR, '. ', m.ENDPOINT_NAME))");
		replace(query, "CONVERSATION ID", "i.ORIGINAL_ID");
		replace(query, "CORRELATION ID", "i.CORRELATION_ID");
		replace(query, "NR OF CHECKPOINTS", "count(m.CHECKPOINT_NR)");
		replace(query, "STATUS", "min(m.STATUS)");
		replace(query, "where", "and"); // Present when filter used by user
		replace(query, getTable(), getTable() + " m, IDS i where m.CORRELATION_ID=i.CORRELATION_ID");
		// Occurrences in where clause, test with filter
		replace(query, "TIMESTAMP", "i.TIMESTAMP"); // Prefix is added by replace so it will skip i.TIMESTAMP
		replace(query, "lower(ENDPOINT NAME)", "lower(m.ENDPOINT_NAME)");
		replace(query, "lower(CONVERSATION ID)", "lower(i.ORIGINAL_ID)");
		replace(query, "lower(CORRELATION ID)", "lower(i.CORRELATION_ID)");
		replace(query, "NR OF CHECKPOINTS", "m.CHECKPOINT_NR + 1000"); // Disable as it's not possible to filter on count(m.CHECKPOINT_NR) (when not disabled the result would be confusing)
		replace(query, "lower(STATUS)", "lower(m.STATUS)");
		// For performance it's important that the first selection can be made based on the IDS table, hence the
		// 'order by i.TIMESTAMP'. This will also make sure the record for the old component and the record for the new
		// component (that have the same correlation id) are displayed directly below each other (the extra order on
		// i.CORRELATION_ID will make this 100% waterproof)
		replace(query, "order by ID desc" , GROUP_BY + " order by i.TIMESTAMP desc, i.CORRELATION_ID");
		if (isShowErrorsOnly()) {
			replace(query, "group by" , "and m.STATUS != 'Success' group by");
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
		replace(query, "from", "from (select " + GROUP_BY.substring(9).replace("i.", "") + " from");
		query.append(" " + GROUP_BY.replace("i.", "") + ") as DUMMY");
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		String query =
				"select CORRELATION_ID, COMPONENT, CHECKPOINT_NR, ENDPOINT_NAME, ACTION, MESSAGE, TIMESTAMP from "
				+ getTable() + " where CORRELATION_ID = (select CORRELATION_ID from " + getTable() + " where "
				+ getStorageIdColumn() + " = ?) and COMPONENT = (select COMPONENT from " + getTable() + " where "
				+ getStorageIdColumn() + " = ?) order by CHECKPOINT_NR";
		List<Object> args = new ArrayList<Object>();
		args.add(storageId);
		args.add(storageId);
		log.debug("Get report query (with arguments: " + args + "): " + query);
		List<List<Object>> result = getJdbcTemplate().query(query, args.toArray(),
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
