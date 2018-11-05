/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2;

import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.reports.CheckpointComponent;
import nl.nn.testtool.echo2.reports.InfoPane;
import nl.nn.testtool.echo2.reports.PathComponent;
import nl.nn.testtool.echo2.reports.ReportComponent;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.echo2.reports.ReportsListPane;
import nl.nn.testtool.echo2.reports.ReportsTreeCellRenderer;
import nl.nn.testtool.echo2.reports.TreePane;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * @author Jaco de Groot
 */
public class DebugPane extends Tab implements BeanParent {
	private static final long serialVersionUID = 1L;
	private String title = "Debug";
	private TestTool testTool;
	private TreePane treePane;
	private InfoPane infoPane;
	private ReportsComponent reportsComponent;
	private ReportComponent reportComponent;
	private CheckpointComponent checkpointComponent;
	private ReportsTreeCellRenderer reportsTreeCellRenderer;
	private ReportXmlTransformer reportXmlTransformer;
	private ReportsListPane reportsListPane;
	private CrudStorage runStorage;

	public DebugPane() {
		super();
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setRunStorage(CrudStorage runStorage) {
		this.runStorage = runStorage;
	}

	public void setReportsComponent(ReportsComponent reportsComponent) {
		this.reportsComponent = reportsComponent;
	}

	public ReportsComponent getReportsComponent() {
		return reportsComponent;
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}

	public TreePane getTreePane() {
		return treePane;
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
	}

	public InfoPane getInfoPane() {
		return infoPane;
	}

	public void setReportsTreeCellRenderer(ReportsTreeCellRenderer reportsTreeCellRenderer) {
		this.reportsTreeCellRenderer = reportsTreeCellRenderer;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {

		// Construct

		TabPaneLayoutData tabPaneLayoutDebugPane = new TabPaneLayoutData();
		tabPaneLayoutDebugPane.setTitle(title);
		setLayoutData(tabPaneLayoutDebugPane);
		TreePane treePane = new TreePane();
		InfoPane infoPane = new InfoPane();
		reportsListPane = new ReportsListPane();
		reportComponent = new ReportComponent();
		PathComponent pathComponent = new PathComponent();
		checkpointComponent = new CheckpointComponent();

		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_VERTICAL);
		splitPane1.setResizable(true);
		splitPane1.setSeparatorPosition(new Extent(250, Extent.PX));
		SplitPane splitPane2 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane2.setResizable(true);
		splitPane2.setSeparatorPosition(new Extent(400, Extent.PX));

		// Wire

		setTreePane(treePane);
		setInfoPane(infoPane);
		
		treePane.setInfoPane(infoPane);
		treePane.setReportsTreeCellRenderer(reportsTreeCellRenderer);

		reportsListPane.setReportsComponent(reportsComponent);
		infoPane.setReportComponent(reportComponent);
		infoPane.setPathComponent(pathComponent);
		infoPane.setCheckpointComponent(checkpointComponent);

		reportsComponent.setTreePane(treePane);
		reportsComponent.setReportXmlTransformer(reportXmlTransformer);
		reportComponent.setTestTool(testTool);
		reportComponent.setRunStorage(runStorage);
		reportComponent.setTreePane(treePane);
		reportComponent.setInfoPane(infoPane);
		pathComponent.setTreePane(treePane);
		checkpointComponent.setTestTool(testTool);
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

		reportComponent.initBean();
		pathComponent.initBean();
		checkpointComponent.initBean();
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		Echo2Application echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		reportsComponent.setTransformationWindow(echo2Application.getTransformationWindow());
		reportsComponent.initBean(this);
		infoPane.initBean(this);
	}

}