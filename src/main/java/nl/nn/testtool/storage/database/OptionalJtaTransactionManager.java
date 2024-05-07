/*
   Copyright 2024 WeAreFrank!

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

import java.lang.invoke.MethodHandles;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import lombok.Setter;

/**
 * <p>
 * Use {@link org.springframework.transaction.jta.JtaTransactionManager} when possible (e.g. on JBoss),
 * fallback to {@link org.springframework.jdbc.datasource.DataSourceTransactionManager} otherwise (e.g. on Tomcat).
 * </p>
 * 
 * <p>
 * This will help applications, that for example need to run on both JBoss and Tomcat, to automatically use the JBoss
 * provided transaction manager on JBoss and create a DataSourceTransactionManager when running on Tomcat (using a
 * DataSourceTransactionManager on JBoss can give: IJ031019: You cannot commit during a managed transaction).
 * </p>
 * 
 * <p>
 * For applications that require XA / distributed transactions a DataSourceTransactionManager will not but sufficient.
 * But for example when using JdbcTemplate and only in need of a transaction manager to disable auto-commit (like in
 * {@link DatabaseStorage} using OptionalJtaTransactionManager can be convenient.
 * </p>
 * 
 * <p>
 * Other solutions like defining both beans and only use one of them depending on the detected application server can be
 * tricky because Spring will detect both transaction managers and give the following error:
 *   No qualifying bean of type 'org.springframework.transaction.TransactionManager' available: expected single matching
 *   bean but found 2
 * </p>
 * 
 * <p>
 * Using &lt;tx:jta-transaction-manager/> will require Tomcat to be configured to provide a transaction manager through
 * JNDI. See https://stackoverflow.com/a/5080761/17193564
 * </p>
 * 
 * <p>
 * Please note that when DataSourceTransactionManager is being used you might want to check whether a connection pool is
 * provided. Java EE servers that provide a transaction manager will automatically link the data source to it and
 * provide a connection pool. In case of Tomcat it depends on how the database connection is configured in the
 * context.xml. DBCP or HikariCP can be used to add connection pooling at application level. This should not be done
 * when Tomcat already provides a connection pool. Class PoolingDataSourceFactory of the Frank!Framework can be used to
 * detect whether a connection pool is provided and automatically create a DBCP 2 connection pool when needed.
 * </p>
 * 
 * @author Jaco de Groot
 */
public class OptionalJtaTransactionManager implements PlatformTransactionManager, InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private JtaTransactionManager jtaTransactionManager;
	private DataSourceTransactionManager dataSourceTransactionManager;
	/**
	 * Will be used when a fallback to {@link org.springframework.jdbc.datasource.DataSourceTransactionManager} is
	 * needed (Java EE servers that provide a transaction manager will automatically link the data source to it as the
	 * data source is also managed by the Java EE server).
	 */
	private @Setter DataSource dataSource;

	public OptionalJtaTransactionManager() {
		jtaTransactionManager = new JtaTransactionManager();
		// Copied from Frank!Framework, seems like a reasonable default
		jtaTransactionManager.setTransactionSynchronization(
				AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		try {
			jtaTransactionManager.afterPropertiesSet();
			log.debug("JtaTransactionManager will be used");
		} catch(IllegalStateException e) {
			log.debug("DataSourceTransactionManager will be used");
			dataSourceTransactionManager = new DataSourceTransactionManager();
			dataSourceTransactionManager.setDataSource(dataSource);
			dataSourceTransactionManager.afterPropertiesSet();
		}
	}

	@Override
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			return dataSourceTransactionManager.getTransaction(definition);
		} else {
			return jtaTransactionManager.getTransaction(definition);
		}
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			dataSourceTransactionManager.commit(status);
		} else {
			jtaTransactionManager.commit(status);
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			dataSourceTransactionManager.rollback(status);
		} else {
			jtaTransactionManager.rollback(status);
		}
	}
}
