/*
   Copyright 2022, 2024 WeAreFrank!

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
package nl.nn.testtool.storage.database;

import java.sql.SQLException;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import jakarta.annotation.PostConstruct;
import lombok.Setter;

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
	private @Setter @Inject @Autowired JdbcTemplate ladybugJdbcTemplate;
	private String commonDatabaseName;

	@PostConstruct
	public void init() throws SQLException, MetaDataAccessException {
		String databaseProductName = JdbcUtils.extractDatabaseMetaData(ladybugJdbcTemplate.getDataSource(), (dbmd) -> dbmd.getDatabaseProductName());
		commonDatabaseName = JdbcUtils.commonDatabaseName(databaseProductName);
	}

	public String provideFirstRowsHintAfterFirstKeyword(int rowCount) {
		String sql = "";
		if ("Oracle".equals(commonDatabaseName)) {
			sql += " /*+ first_rows( " + rowCount + " ) */";
		} else if ("Microsoft SQL Server".equals(commonDatabaseName)) {
			sql += " top( "+rowCount+" )";
		}
		return sql;
	}

	public String provideTrailingFirstRowsHint(int rowCount) {
		String sql = "";
		if (rowCount > -1) {
			if (!"Oracle".equals(commonDatabaseName) && !"Microsoft SQL Server".equals(commonDatabaseName)) {
				sql += " limit " + rowCount;
			}
		}
		return sql;
	}

	public String getRowNumber(String order, String sort) {
		if ("Oracle".equals(commonDatabaseName) || "Microsoft SQL Server".equals(commonDatabaseName)) {
			return "row_number() over (order by "+order+(sort==null?"":" "+sort)+") "+getRowNumberShortName();
		}
		return "";
	}

	public String getRowNumberShortName() {
		return "rn";
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

}
