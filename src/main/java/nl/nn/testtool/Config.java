/*
   Copyright 2022-2024 WeAreFrank!

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
package nl.nn.testtool;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.TransactionManager;

import io.quarkus.arc.DefaultBean;
import nl.nn.testtool.echo2.ComparePane;
import nl.nn.testtool.echo2.DebugPane;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.Tabs;
import nl.nn.testtool.echo2.TestPane;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.database.DbmsSupport;
import nl.nn.testtool.storage.memory.Storage;
import nl.nn.testtool.storage.proofofmigration.ProofOfMigrationErrorsStorage;
import nl.nn.testtool.storage.proofofmigration.ProofOfMigrationErrorsView;
import nl.nn.testtool.storage.proofofmigration.ProofOfMigrationStorage;
import nl.nn.testtool.storage.proofofmigration.ProofOfMigrationView;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * <p>
 * Default configuration / wiring of beans to minimize the Spring (xml) / Quarkus configuration needed to integrate
 * Ladybug into an application.
 * </p>
 * 
 * <p>
 * For classes with <code>@Inject</code>, <code>@Autowired</code> and/or <code>@PostConstruct</code> annotations add
 * <code>@Bean</code> methods to this class for Spring with <code>@Scope("singleton")</code> or
 * <code>@Scope("prototype")</code> and for Quarkus add <code>@Produces</code> and optionally <code>@Singleton</code> to
 * this class or add <code>@Dependent</code> or <code>@Singleton</code> to the classes with <code>@Inject</code>,
 * <code>@Autowired</code> and/or <code>@PostConstruct</code> annotations.
 * </p>
 * 
 * <p>
 * For other classes add <code>@Bean</code> (Spring) and <code>@DefaultBean</code> (Quarkus) methods to this class which
 * can be overridden for Spring in xml and for Quarkus in Java.
 * </p>
 * 
 * Spring related:
 * 
 * <ul>
 *   <li>Enable component scanning, e.g.: &lt;context:component-scan base-package="nl.nn.testtool"/&gt;</li>
 *   <li>Use <code>@Autowired</code> also where <code>@Inject</code> is being used to get the same behavior whether or
 *   not Spring is able to find the Inject class on the classpath. See also the explanation of
 *   SpringBeanAutowiringInterceptor at
 *   https://stackoverflow.com/questions/37592743/configuring-spring-to-ignore-dependencies-annotated-with-inject</li>
 *   <li>The <code>@PostConstruct</code> annotation is part of Java SE 8 and supported by Spring, see also
 *   https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/beans.html#beans-postconstruct-and-predestroy-annotations</li>
 *   <li>Spring will wire and init beans returned by the <code>@Bean</code> methods (as far as those beans have the
 *   needed <code>@Inject</code>, <code>@Autowired</code> and <code>@PostConstruct</code> annotations</li>
 *   <li>Spring XML configuration can be used to override the defaults as specified by the annotations</li>
 * </ul>
 * 
 * Quarkus related:
 * 
 * <ul>
 *   <li>Quarkus doesn't wire and init beans returned by the methods in this class but will do it for beans created
 *   based on classes that contain <code>@Dependent</code> or <code>@Singleton</code></li>
 *   <li>Default wiring using <code>@DefaultBean</code> (https://quarkus.io/guides/cdi-reference#default_beans) can be
 *   overridden in an application using Ladybug as a library.</li>
 * </ul>
 * 
 * @author Jaco de Groot
 */

@Singleton
@Scope("singleton")
@Lazy // Lazy init singleton beans (prototype beans are already loaded on demand)
@Configuration
public class Config {

	@Bean
	@Scope("prototype") // Echo2Application needs to be unique per user (not per JVM)
	Echo2Application echo2Application() {
		return new Echo2Application();
	}

	@Produces
	@DefaultBean
	@Bean
	@Scope("prototype")
	Tabs tabs(DebugPane debugPane, TestPane testPane, ComparePane comparePane) {
		Tabs tabs = new Tabs();
		tabs.add(debugPane);
		tabs.add(testPane);
		tabs.add(comparePane);
		return tabs;
	}

	@Bean
	@Scope("prototype")
	DebugPane debugPane() {
		return new DebugPane();
	}

	@Bean
	@Scope("prototype")
	TestPane testPane() {
		return new TestPane();
	}

	@Bean
	@Scope("prototype")
	ComparePane comparePane() {
		return new ComparePane();
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	Views views(View view, LogStorage debugStorage) {
		view.setName("Default");
		List<View> list = new ArrayList<View>();
		list.add(view);
		Views views = new Views();
		views.setViews(list);
		return views;
	}

	@Bean
	@Scope("prototype")
	View view() {
		return new View();
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	// When CrudStorage is also implemented by the returned storage it's reports can be deleted in the debug tab
	LogStorage debugStorage(MetadataExtractor metadataExtractor) {
		Storage storage = new Storage(); // Memory storage which also implements CrudStorage
		storage.setName("Debug");
		storage.setMetadataExtractor(metadataExtractor);
		return storage;
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	CrudStorage testStorage(MetadataExtractor metadataExtractor) {
		Storage storage = new Storage();
		storage.setName("Test");
		storage.setMetadataExtractor(metadataExtractor);
		return storage;
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	List<String> metadataNames() {
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("endTime");
		metadataNames.add("duration");
		metadataNames.add("name");
		metadataNames.add("correlationId");
		metadataNames.add("status");
		metadataNames.add("numberOfCheckpoints");
		metadataNames.add("estimatedMemoryUsage");
		metadataNames.add("storageSize");
		return metadataNames;
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	MetadataExtractor metadataExtractor() {
		return new MetadataExtractor();
	}

	@Bean
	@Scope("singleton")
	ReportXmlTransformer reportXmlTransformer() {
		return new ReportXmlTransformer();
	}

	@Produces
	@Singleton
	@DefaultBean
	@Bean
	@Scope("singleton")
	String xsltResource() {
		return "ladybug/default.xslt";
	}

	@Bean
	@Scope("singleton")
	DataSource dataSource() {
		return new SimpleDriverDataSource();
	}

	@Bean
	@Scope("prototype")
	// Prefix with ladybug to prevent interference with other beans of the application that is using Ladybug. E.g. in
	// F!F matrix test several combination fail with 5 scenarios failed (all JMS related)
	TransactionManager ladybugTransactionManager(DataSource dataSource) {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource);
		return transactionManager;
	}

	@Bean
	@Scope("prototype")
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(dataSource);
		return jdbcTemplate;
	}

	@Bean
	@Scope("singleton")
	DbmsSupport dbmsSupport() {
		return new DbmsSupport();
	}

	@Bean
	@Scope("prototype")
	ProofOfMigrationStorage proofOfMigrationStorage() {
		return new ProofOfMigrationStorage();
	}

	@Bean
	@Scope("prototype")
	ProofOfMigrationView proofOfMigrationView() {
		return new ProofOfMigrationView();
	}

	@Bean
	@Scope("prototype")
	ProofOfMigrationErrorsStorage proofOfMigrationErrorsStorage() {
		return new ProofOfMigrationErrorsStorage();
	}

	@Bean
	@Scope("prototype")
	ProofOfMigrationErrorsView proofOfMigrationErrorsView() {
		return new ProofOfMigrationErrorsView();
	}

}
