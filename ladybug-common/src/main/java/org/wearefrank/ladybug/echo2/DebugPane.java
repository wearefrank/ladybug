/*
   Copyright 2020, 2022-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.echo2;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.echo2.reports.CheckpointComponent;
import org.wearefrank.ladybug.echo2.reports.InfoPane;
import org.wearefrank.ladybug.echo2.reports.PathComponent;
import org.wearefrank.ladybug.echo2.reports.ReportComponent;
import org.wearefrank.ladybug.echo2.reports.ReportsComponent;
import org.wearefrank.ladybug.echo2.reports.ReportsListPane;
import org.wearefrank.ladybug.echo2.reports.TreePane;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;

/**
 * @author Jaco de Groot
 */
@Dependent
public class DebugPane extends Tab implements BeanParent {
	private static final long serialVersionUID = 1L;
	private String title = "Debug";
	private @Inject @Autowired TestTool testTool;
	private @Inject @Autowired Views views;
	private @Inject @Autowired LogStorage debugStorage;
	private @Inject @Autowired CrudStorage testStorage;
	private @Inject @Autowired MetadataExtractor metadataExtractor;
	private @Inject @Autowired ReportXmlTransformer reportXmlTransformer;
	private @Inject @Resource(name = "dataAdminRoles") List<String> dataAdminRoles;
	private ReportsListPane reportsListPane;
	private ReportsComponent reportsComponent;
	private TreePane treePane;
	private InfoPane infoPane;
	private ReportComponent reportComponent;
	private CheckpointComponent checkpointComponent;
	private PathComponent pathComponent;

	public DebugPane() {
		super();
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	@PostConstruct
	public void initBean() {

		// Construct

		TabPaneLayoutData tabPaneLayoutDebugPane = new TabPaneLayoutData();
		tabPaneLayoutDebugPane.setTitle(title);
		setLayoutData(tabPaneLayoutDebugPane);
		reportsComponent = new ReportsComponent();
		treePane = new TreePane();
		infoPane = new InfoPane();
		reportsListPane = new ReportsListPane();
		reportComponent = new ReportComponent();
		checkpointComponent = new CheckpointComponent();
		pathComponent = new PathComponent();

		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_VERTICAL);
		splitPane1.setResizable(true);
		splitPane1.setSeparatorPosition(new Extent(250, Extent.PX));
		SplitPane splitPane2 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane2.setResizable(true);
		splitPane2.setSeparatorPosition(new Extent(400, Extent.PX));

		// Wire

		treePane.setInfoPane(infoPane);

		reportsListPane.setReportsComponent(reportsComponent);
		infoPane.setReportComponent(reportComponent);
		infoPane.setPathComponent(pathComponent);
		infoPane.setCheckpointComponent(checkpointComponent);

		reportsComponent.setTestTool(testTool);
		reportsComponent.setViews(views);
		reportsComponent.setMetadataExtractor(metadataExtractor);
		reportsComponent.setTreePane(treePane);
		reportsComponent.setReportXmlTransformer(reportXmlTransformer);
		reportsComponent.setDataAdminRoles(dataAdminRoles);
		reportComponent.setTestTool(testTool);
		reportComponent.setDebugStorage(debugStorage);
		reportComponent.setTestStorage(testStorage);
		reportComponent.setTreePane(treePane);
		reportComponent.setInfoPane(infoPane);
		pathComponent.setTreePane(treePane);
		checkpointComponent.setTestTool(testTool);
		checkpointComponent.setDebugStorage(debugStorage);
		checkpointComponent.setTreePane(treePane);
		checkpointComponent.setInfoPane(infoPane);

		splitPane2.add(treePane);
		splitPane2.add(infoPane);
		splitPane1.add(reportsListPane);
		splitPane1.add(splitPane2);
		add(splitPane1);

		// Init

		reportsListPane.initBean();
		treePane.initBean();
		infoPane.initBean();

		reportsComponent.initBean();
		reportComponent.initBean();
		pathComponent.initBean();
		checkpointComponent.initBean();
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		Echo2Application echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		reportsComponent.setTransformationWindow(echo2Application.getTransformationWindow());
		reportsComponent.initBean(this);
		infoPane.initBean(this);
	}

}
