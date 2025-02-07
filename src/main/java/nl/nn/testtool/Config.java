/*
   Copyright 2022-2025 WeAreFrank!

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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.TransactionManager;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
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
import nl.nn.testtool.storage.database.OptionalJtaTransactionManager;
import nl.nn.testtool.storage.memory.MemoryCrudStorage;
import nl.nn.testtool.storage.memory.MemoryLogStorage;
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
 * For classes with <code>@Inject</code>, <code>@Autowired</code>, <code>@Resource</code> and/or
 * <code>@PostConstruct</code> annotations:
 * <ul>
 *   <li>
 *     For Spring: Add <code>@Bean</code> methods to this class for with <code>@Scope("singleton")</code> or
 *     <code>@Scope("prototype")</code>
 *   </li>
 *   <li>
 *     For Quarkus: Add <code>@Produces</code> and optionally <code>@Singleton</code> to this class. Or add
 *     <code>@Dependent</code> or <code>@Singleton</code> to the classes with <code>@Inject</code>,
 *     <code>@Autowired</code>, <code>@Resource</code> and/or <code>@PostConstruct</code> annotations.
 *   </li>
 * </ul>
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
 *   <li>
 *     Enable component scanning, e.g.: &lt;context:component-scan base-package="nl.nn.testtool"/&gt;
 *   </li>
 *   <li>
 *     Use <code>@Autowired</code> also where <code>@Inject</code> is being used to get the same behavior whether or not
 *     Spring is able to find the Inject class on the classpath. See also the explanation of
 *     SpringBeanAutowiringInterceptor at
 *     https://stackoverflow.com/questions/37592743/configuring-spring-to-ignore-dependencies-annotated-with-inject
 *   </li>
 *   <li>
 *     Use <code>@Resource(name="beanName")</code> instead of <code>@Autowired</code> for lists of strings otherwise all
 *     String beans (e.g. the beans in Spring xml with class="java.lang.String") are added to a list and wired instead
 *     (see also https://stackoverflow.com/a/1363435/17193564). This doesn't happen when autowire="byName" is used in
 *     the Spring xml but the idea is to keep the Spring xml as small and simple as possible (and not use
 *     autowire="byName").
 *   </li>
 *   <li>
 *     The <code>@PostConstruct</code> annotation is part of Java SE 8 and supported by Spring, see also
 *     https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/beans.html#beans-postconstruct-and-predestroy-annotations
 *   </li>
 *   <li>
 *     Spring will wire and init beans returned by the <code>@Bean</code> methods (as far as those beans have the
 *     needed <code>@Inject</code>, <code>@Autowired</code>, <code>@Resource</code> and <code>@PostConstruct</code>
 *     annotations)
 *   </li>
 *   <li>
 *     Spring XML configuration can be used to override the defaults as specified by the annotations
 *   </li>
 *   <li>
 *     Spring XML configuration needs a setter method / <code>@Setter</code> annotation to set a property
 *     (<code>@Inject</code> and <code>@Autowired</code> don't need / call the setter method, see
 *     https://stackoverflow.com/questions/74061160/do-we-have-to-write-setter-method-to-use-inject-annotation)
 *   </li>
 * </ul>
 * 
 * Quarkus related:
 * 
 * <ul>
 *   <li>
 *     Quarkus doesn't wire and init beans returned by the methods in this class but will do it for beans created
 *     based on classes that contain <code>@Dependent</code> or <code>@Singleton</code>
 *   </li>
 *   <li>
 *     Default wiring using <code>@DefaultBean</code> (https://quarkus.io/guides/cdi-reference#default_beans) can be
 *     overridden in an application using Ladybug as a library.
 *   </li>
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
	Views views(@Qualifier("view") View view, @Qualifier("debugStorage") LogStorage debugStorage) {
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
	LogStorage debugStorage(MetadataExtractor metadataExtractor) {
		MemoryLogStorage storage = new MemoryLogStorage();
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
		MemoryCrudStorage storage = new MemoryCrudStorage();
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
	// Prefix with ladybug to prevent interference with other beans of the application that is using Ladybug. E.g. in
	// F!F when ladybug.jdbc.datasource is empty this bean should not be initialized but when it's name is dataSource
	// it will be wired to the scheduler bean and initialized (giving error with ladybug.jdbc.datasource is empty)
	DataSource ladybugDataSource() {
		return new SimpleDriverDataSource();
	}

	@Bean
	@Scope("prototype")
	// Prefix with ladybug to prevent interference with other beans of the application that is using Ladybug. E.g. in
	// F!F matrix test several combination fail with 5 scenarios failed (all JMS related)
	TransactionManager ladybugTransactionManager(DataSource ladybugDataSource) {
		OptionalJtaTransactionManager optionalJtaTransactionManager = new OptionalJtaTransactionManager();
		optionalJtaTransactionManager.setDataSource(ladybugDataSource);
		return optionalJtaTransactionManager;
	}

	@Bean
	@Scope("prototype")
	// Prefix with ladybug to prevent interference with other beans of the application that is using Ladybug as this is
	// a commonly used class. Other classes (except the above two that are also prefixed) are Ladybug specific 
	JdbcTemplate ladybugJdbcTemplate(DataSource ladybugDataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(ladybugDataSource);
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
