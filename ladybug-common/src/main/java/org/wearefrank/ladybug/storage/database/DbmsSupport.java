/*
   Copyright 2022, 2024-2026 WeAreFrank!

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

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/*

Copied some code from https://github.com/ibissource/iaf/tree/master/core/src/main/java/nl/nn/adapterframework/jdbc/dbms
into this class to prevent a dependency on F!F

For concrete databaseProductName values see:
  https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/core/metadata/CallMetaDataProviderFactory.java
See that most databaseProductName values are returned as-is as commonDatabaseName:
  https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/support/JdbcUtils.java

*/
// @Singleton disabled for Quarkus for now because of the use of JdbcTemplate
public class DbmsSupport {
	private @Inject @Autowired JdbcTemplate ladybugJdbcTemplate;
	private String commonDatabaseName;

	public enum SortOrder {
		ASC("ASC"),
		DESC("DESC");

		private final String value;

		SortOrder(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@PostConstruct
	public void init() throws SQLException, MetaDataAccessException {
		String databaseProductName = JdbcUtils.extractDatabaseMetaData(ladybugJdbcTemplate.getDataSource(), (dbmd) -> dbmd.getDatabaseProductName());
		commonDatabaseName = JdbcUtils.commonDatabaseName(databaseProductName);
	}

	// ── no-offset variants (delegate to offset-aware overloads) ─────────────────

	public String provideLimitAfterFirstKeyword(int limit, List<Object> args, List<Integer> argTypes) {
		return provideLimitAfterFirstKeyword(limit, 0, args, argTypes);
	}

	public String provideFirstRowsHintAfterFirstKeyword(int limit) {
		return provideFirstRowsHintAfterFirstKeyword(limit, 0);
	}

	public String provideLimitWithRowNumber(int limit, List<Object> args, List<Integer> argTypes) {
		return provideLimitWithRowNumber(limit, 0, args, argTypes);
	}

	public String provideLimit(int limit, List<Object> args, List<Integer> argTypes) {
		return provideLimit(limit, 0, args, argTypes);
	}

	// ── offset-aware overloads ───────────────────────────────────────────────────

	/**
	 * SQL Server: emit TOP(N) only when offset is 0. When offset > 0 the caller
	 * must use OFFSET/FETCH (see {@link #provideLimit(int, int, List, List)}).
	 */
	public String provideLimitAfterFirstKeyword(int limit, int offset, List<Object> args, List<Integer> argTypes) {
		if (limit > -1 && "Microsoft SQL Server".equals(commonDatabaseName) && offset <= 0) {
			args.add(limit);
			argTypes.add(Types.INTEGER);
			return " top(" + limit + ")";
		}
		return "";
	}

	/**
	 * Oracle: adjust the first_rows hint to cover offset + limit rows so the
	 * optimizer knows the total number of rows the outer WHERE needs to inspect.
	 */
	public String provideFirstRowsHintAfterFirstKeyword(int limit, int offset) {
		if (limit > -1 && "Oracle".equals(commonDatabaseName)) {
			int hintRows = offset > 0 ? limit + offset : limit;
			return " /*+ first_rows(" + hintRows + ") */ * from (select";
		}
		return "";
	}

	/**
	 * Oracle: close the row-number subquery.
	 * Without offset: {@code WHERE rn <= limit}
	 * With offset:    {@code WHERE rn > offset AND rn <= offset + limit}
	 */
	public String provideLimitWithRowNumber(int limit, int offset, List<Object> args, List<Integer> argTypes) {
		if (limit > -1 && "Oracle".equals(commonDatabaseName)) {
			if (offset > 0) {
				args.add(offset);
				argTypes.add(Types.INTEGER);
				args.add(limit + offset);
				argTypes.add(Types.INTEGER);
				return ") where rn > ? and rn <= ?";
			}
			args.add(limit);
			argTypes.add(Types.INTEGER);
			return ") where rn <= ?";
		}
		return "";
	}

	/**
	 * PostgreSQL / MySQL / H2: LIMIT N
	 * SQL Server with offset: OFFSET ? ROWS FETCH NEXT ? ROWS ONLY (replaces TOP N)
	 */
	public String provideLimit(int limit, int offset, List<Object> args, List<Integer> argTypes) {
		if (limit > -1 && "Microsoft SQL Server".equals(commonDatabaseName) && offset > 0) {
			args.add(offset);
			argTypes.add(Types.INTEGER);
			args.add(limit);
			argTypes.add(Types.INTEGER);
			return " offset ? rows fetch next ? rows only";
		}
		if (limit > -1 && !"Oracle".equals(commonDatabaseName)
				&& !"Microsoft SQL Server".equals(commonDatabaseName)) {
			args.add(limit);
			argTypes.add(Types.INTEGER);
			return " limit ?";
		}
		return "";
	}

	/**
	 * PostgreSQL / MySQL / H2: OFFSET N (appended after LIMIT).
	 * Oracle and SQL Server handle offset inside their own limit clauses.
	 */
	public String provideOffset(int offset, List<Object> args, List<Integer> argTypes) {
		if (offset > 0 && !"Oracle".equals(commonDatabaseName)
				&& !"Microsoft SQL Server".equals(commonDatabaseName)) {
			args.add(offset);
			argTypes.add(Types.INTEGER);
			return " offset ?";
		}
		return "";
	}

	// ── unchanged methods ────────────────────────────────────────────────────────

	public String provideOrderWithRowNumber(int limit, String orderByColumn, SortOrder sortOrder) {
		if (limit > -1 && "Oracle".equals(commonDatabaseName)) {
			return " row_number() over (order by "+ orderByColumn + " " + sortOrder + ") as rn";
		}
		return "";
	}

	public String provideOrder(int limit, String orderByColumn, SortOrder sortOrder) {
		if (!"Oracle".equals(commonDatabaseName) || limit < 0) {
			return " order by " + orderByColumn + " " + sortOrder;
		}
		return "";
	}

	public boolean autoIncrementKeyMustBeInserted() {
		if ("Oracle".equals(commonDatabaseName)) {
			return true;
		}
		return false;
	}

	public String autoIncrementInsertValue(String sequenceName) {
		if ("Oracle".equals(commonDatabaseName)) {
			return sequenceName + ".NEXTVAL";
		}
		return null;
	}

	public String escapeLikePattern(String pattern) {
		pattern = pattern.replaceAll("\\%", "\\\\%");
		if (!"Oracle".equals(commonDatabaseName) && !"Microsoft SQL Server".equals(commonDatabaseName)) {
			pattern = pattern.replaceAll("\\_", "\\\\_");
		}
		return pattern;
	}

}
