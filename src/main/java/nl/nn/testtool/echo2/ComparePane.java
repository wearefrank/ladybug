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

import java.util.ArrayList;
import java.util.List;

import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.reports.CheckpointComponent;
import nl.nn.testtool.echo2.reports.InfoPane;
import nl.nn.testtool.echo2.reports.PathComponent;
import nl.nn.testtool.echo2.reports.ReportComponent;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.echo2.reports.ReportsTreeCellRenderer;
import nl.nn.testtool.echo2.reports.TreePane;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ComparePane extends Tab implements BeanParent {
	private static final long serialVersionUID = 1L;
	private String title = "Compare";
	private TestTool testTool;
	private TreePane treePane1;
	private TreePane treePane2;
	private InfoPane infoPane1;
	private InfoPane infoPane2;
	private ReportsComponent reportsComponent1;
	private ReportComponent reportComponent1;
	private CheckpointComponent checkpointComponent1;
	private ReportsComponent reportsComponent2;
	private ReportComponent reportComponent2;
	private CheckpointComponent checkpointComponent2;
	private ReportsTreeCellRenderer reportsTreeCellRenderer;
	private ReportXmlTransformer reportXmlTransformer;
	private CrudStorage runStorage;

	public ComparePane() {
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

	public void setReportsComponent1(ReportsComponent reportsComponent1) {
		this.reportsComponent1 = reportsComponent1;
	}

	public ReportsComponent getReportsComponent1() {
		return reportsComponent1;
	}

	public ReportsComponent getReportsComponent2() {
		return reportsComponent2;
	}

	public void setReportsComponent2(ReportsComponent reportsComponent2) {
		this.reportsComponent2 = reportsComponent2;
	}

	public void setTreePane1(TreePane treePane) {
		treePane1 = treePane;
	}

	public TreePane getTreePane1() {
		return treePane1;
	}

	public void setTreePane2(TreePane treePane) {
		treePane2 = treePane;
	}

	public TreePane getTreePane2() {
		return treePane2;
	}

	public void setInfoPane1(InfoPane infoPane) {
		infoPane1 = infoPane;
	}

	public void setInfoPane2(InfoPane infoPane) {
		infoPane2 = infoPane;
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

		TabPaneLayoutData tabPaneLayoutComparePane = new TabPaneLayoutData();
		tabPaneLayoutComparePane.setTitle(title);
		setLayoutData(tabPaneLayoutComparePane);
		TreePane treePane1 = new TreePane();
		TreePane treePane2 = new TreePane();
		InfoPane infoPane1 = new InfoPane();
		InfoPane infoPane2 = new InfoPane();
		reportComponent1 = new ReportComponent();
		PathComponent pathComponent1 = new PathComponent();
		checkpointComponent1 = new CheckpointComponent();
		reportComponent2 = new ReportComponent();
		PathComponent pathComponent2 = new PathComponent();
		checkpointComponent2 = new CheckpointComponent();
		
		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane1.setResizable(true);
		// TODO: Change separator position to 50% of browser's screen width for compatibility
		// with values other than the most common full-sized browser window width of 1920px;
		// using "new Extent(50, Extent.PERCENT)" is 'unsupported for this context'.
		splitPane1.setSeparatorPosition(new Extent(960, Extent.PX));

		SplitPane splitPane2 = new SplitPane(SplitPane.ORIENTATION_VERTICAL);
		splitPane2.setResizable(true);
		splitPane2.setSeparatorPosition(new Extent(250, Extent.PX));


		SplitPane splitPane3 = new SplitPane(SplitPane.ORIENTATION_VERTICAL);
		splitPane3.setResizable(true);
		splitPane3.setSeparatorPosition(new Extent(250, Extent.PX));

		// Wire

		setTreePane1(treePane1);
		setTreePane2(treePane2);
		setInfoPane1(infoPane1);
		setInfoPane2(infoPane2);

		treePane1.setInfoPane(infoPane1);
		treePane1.setTreePaneCounterpart(treePane2);
		treePane1.setReportsTreeCellRenderer(reportsTreeCellRenderer);

		treePane2.setInfoPane(infoPane2);
		treePane2.setTreePaneCounterpart(treePane1);
		treePane2.setReportsTreeCellRenderer(reportsTreeCellRenderer);

		infoPane1.setReportsComponent(reportsComponent1);
		infoPane1.setReportComponent(reportComponent1);
		infoPane1.setPathComponent(pathComponent1);
		infoPane1.setCheckpointComponent(checkpointComponent1);

		infoPane2.setReportsComponent(reportsComponent2);
		infoPane2.setReportComponent(reportComponent2);
		infoPane2.setPathComponent(pathComponent2);
		infoPane2.setCheckpointComponent(checkpointComponent2);

		reportsComponent1.setTreePane(treePane1);
		reportsComponent1.setReportXmlTransformer(reportXmlTransformer);
		reportsComponent1.setComparePane(this);
		reportComponent1.setTestTool(testTool);
		reportComponent1.setRunStorage(runStorage);
		reportComponent1.setTreePane(treePane1);
		reportComponent1.setInfoPane(infoPane1);
		pathComponent1.setTreePane(treePane1);
		checkpointComponent1.setTestTool(testTool);
		checkpointComponent1.setTreePane(treePane1);
		checkpointComponent1.setInfoPane(infoPane1);

		reportsComponent2.setTreePane(treePane2);
		reportsComponent2.setReportXmlTransformer(reportXmlTransformer);
		reportsComponent2.setComparePane(this);
		reportComponent2.setTestTool(testTool);
		reportComponent2.setRunStorage(runStorage);
		reportComponent2.setTreePane(treePane2);
		reportComponent2.setInfoPane(infoPane2);
		pathComponent2.setTreePane(treePane2);
		checkpointComponent2.setTestTool(testTool);
		checkpointComponent2.setTreePane(treePane2);
		checkpointComponent2.setInfoPane(infoPane2);

		splitPane1.add(splitPane2);
		splitPane1.add(splitPane3);

		splitPane2.add(treePane1);
		splitPane2.add(infoPane1);

		splitPane3.add(treePane2);
		splitPane3.add(infoPane2);

		add(splitPane1);

		// Init

		reportComponent1.initBean();
		pathComponent1.initBean();
		checkpointComponent1.initBean();

		reportComponent2.initBean();
		pathComponent2.initBean();
		checkpointComponent2.initBean();

		treePane1.initBean();
		treePane2.initBean();

		infoPane1.initBean();
		infoPane2.initBean();
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		Echo2Application echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		reportsComponent1.setTransformationWindow(echo2Application.getTransformationWindow());
		reportsComponent2.setTransformationWindow(echo2Application.getTransformationWindow()); 
		reportsComponent1.initBean(this);
		reportsComponent2.initBean(this);
		infoPane1.initBean(this);
		infoPane2.initBean(this);
	}

	public void compare(Report report1, Report report2) {
		if (report1.toXml().equals(report2.toXml())) {
			report1.setDifferenceFound(false);
			report2.setDifferenceFound(false);
		} else {
			report1.setDifferenceFound(true);
			report2.setDifferenceFound(true);
		}
		report1.setDifferenceChecked(true);
		report2.setDifferenceChecked(true);
	}

	public void compare() {
		treePane1.collapseAll();
		treePane1.expandDirectChilds();
		treePane2.collapseAll();
		treePane2.expandDirectChilds();
		try {
			Storage storage1 = treePane1.getStorage();
			Storage storage2 = treePane2.getStorage();
			List ids1 = storage1.getStorageIds();
			if (ids1 == null) {
				ids1 = new ArrayList();
			}
			List ids2 = storage2.getStorageIds();
			if (ids2 == null) {
				ids2 = new ArrayList();
			}
			int size = ids1.size();
			if (ids2.size() > size) {
				size = ids2.size();
			}
			for (int i = 0; i < size; i++) {
				if (i < ids1.size()) {
					Integer id1 = (Integer)ids1.get(i);
					Report report1 = storage1.getReport(id1);
					report1.setDifferenceFound(true);
					if (i < ids2.size()) {
						Integer id2 = (Integer)ids2.get(i);
						Report report2 = storage2.getReport(id2);
						if (report1.toXml().equals(report2.toXml())) {
							report1.setDifferenceFound(false);
							report2.setDifferenceFound(false);
						} else {
							report2.setDifferenceFound(true);
						}
						report2.setDifferenceChecked(true);
					}
					report1.setDifferenceChecked(true);
				} else {
					Integer id2 = (Integer)ids2.get(i);
					Report report2 = storage2.getReport(id2);
					report2.setDifferenceFound(true);
					report2.setDifferenceChecked(true);
				}
			}
		} catch(StorageException e) {
			// TODO display error
		}
	}

}
