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
package nl.nn.testtool;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import nl.nn.testtool.echo2.DebugPane;
import nl.nn.testtool.echo2.Tabs;
import nl.nn.testtool.echo2.TestPane;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.metadata.StatusMetadataFieldExtractor;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.memory.Storage;


/**
 * Default wiring using @DefaultBean (https://quarkus.io/guides/cdi-reference#default_beans) that can be overridden in
 * an application using Ladybug as a library.
 * 
 * @author Jaco de Groot
 */

@Singleton
public class Config {

	@Produces
	@Singleton
	@DefaultBean
	MetadataExtractor metadataExtractor() {
		MetadataExtractor metadataExtractor = new MetadataExtractor();
		List<MetadataFieldExtractor> extraMetadataFieldExtractors = new ArrayList<MetadataFieldExtractor>();
		extraMetadataFieldExtractors.add(new StatusMetadataFieldExtractor());
		metadataExtractor.setExtraMetadataFieldExtractors(extraMetadataFieldExtractors);
		return metadataExtractor;
	}

	@Produces
	@Singleton
	@DefaultBean
	LogStorage debugStorage(MetadataExtractor metadataExtractor) {
		Storage storage = new Storage();
		storage.setName("Debug");
		storage.setMetadataExtractor(metadataExtractor);
		return storage;
	}

	@Produces
	@Singleton
	@DefaultBean
	CrudStorage testStorage(MetadataExtractor metadataExtractor) {
		Storage storage = new Storage();
		storage.setName("Test");
		storage.setMetadataExtractor(metadataExtractor);
		return storage;
	}

	@Produces
	@DefaultBean
	Tabs tabs(DebugPane debugPane, TestPane testPane) {
		Tabs tabs = new Tabs();
		tabs.add(debugPane);
		tabs.add(testPane);
		return tabs;
	}

	@Produces
	@DefaultBean
	Views views(View view, LogStorage debugStorage) {
		view.setName("Default");
		view.setStorage(debugStorage);
		List<View> list = new ArrayList<View>();
		list.add(view);
		Views views = new Views();
		views.setViews(list);
		return views;
	}

	@Produces
	@DefaultBean
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
	String xsltResource() {
		return "ladybug/default.xslt";
	}

}
