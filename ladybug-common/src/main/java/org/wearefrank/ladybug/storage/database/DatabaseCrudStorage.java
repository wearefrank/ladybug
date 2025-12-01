/*
   Copyright 2024-2025 WeAreFrank!

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
package org.wearefrank.ladybug.storage.database;


import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;

import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.util.Export;

/**
 * Special use case database storage that can be used as debug storage IS DT ZO? MOET HIJ DAN NIET OOK LOG STOAGE IMPLEMENTEREN? 
 */
public class DatabaseCrudStorage extends DatabaseStorage implements CrudStorage {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public long getMaxStorageSize() {
		return -1L;
	}

	@Override
	public void update(Report report) throws StorageException {
		byte[] reportBytes = Export.getReportBytes(report);
		String reportXml = report.toXml();
		long storageSize = reportBytes.length;
		if (isStoreReportXml()) {
			storageSize = storageSize + reportXml.length();
		}
		report.setStorageSize(storageSize);

		StringBuilder query = new StringBuilder("update " + getTable() + " set ");
		boolean first = true;
		for (String column : getMetadataNames()) {
			if (!column.equals(getStorageIdColumn())) {
				if (!first) {
					query.append(", ");
				}
				query.append(column).append(" = ?");
				first = false;
			}
		}
		query.append(", report = ?");
		if (isStoreReportXml()) {
			query.append(", reportxml = ?");
		}
		query.append(" where " + getStorageIdColumn() + " = ?");

		log.debug("Update report query: " + query.toString());

		ladybugJdbcTemplate.update(query.toString(), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				int i = 1;
				for (String column : getMetadataNames()) {
					if (!column.equals(getStorageIdColumn())) {
						if (isInteger(column)) {
							ps.setInt(i, (Integer) metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT));
						} else if (isLong(column)) {
							ps.setLong(i, (Long) metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT));
						} else if (isTimestamp(column)) {
							ps.setTimestamp(i, new Timestamp((Long) metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT)));
						} else {
							ps.setString(i, (String) metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_STRING));
						}
						i++;
					}
				}
				ps.setBytes(i, reportBytes);
				i++;
				if (isStoreReportXml()) {
					ps.setClob(i, new StringReader(reportXml));
					i++;
				}
				ps.setInt(i, report.getStorageId());
			}
		});
	}

	@Override
	public void delete(Report report) throws StorageException {
		delete(report.getStorageId());
	}

	protected void delete(Integer storageId) throws StorageException {
		String query = "delete from " + getTable() + " where " + getStorageIdColumn() + " = ?";
		delete(query, storageId);
	}

}
