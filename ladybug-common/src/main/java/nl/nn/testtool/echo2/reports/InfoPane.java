/*
   Copyright 2020, 2022-2023, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2.reports;

import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.TreePath;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BaseComponent;
import nl.nn.testtool.echo2.BeanParent;

/**
 * @author Jaco de Groot
 */
public class InfoPane extends ContentPane implements BeanParent {
	private static final long serialVersionUID = 1L;
	private ReportsComponent reportsComponent;
	private ReportComponent reportComponent;
	private PathComponent pathComponent;
	private CheckpointComponent checkpointComponent;
	private boolean showLineNumbers = false;
	private boolean edit = false;
	private BeanParent beanParent;

	public void setReportsComponent(ReportsComponent reportsComponent) {
		this.reportsComponent = reportsComponent;
	}

	public void setReportComponent(ReportComponent reportComponent) {
		this.reportComponent = reportComponent;
	}

	public void setPathComponent(PathComponent pathComponent) {
		this.pathComponent = pathComponent;
	}

	public void setCheckpointComponent(CheckpointComponent checkpointComponent) {
		this.checkpointComponent = checkpointComponent;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		reportComponent.initBean(this);
		checkpointComponent.initBean(this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	protected boolean showLineNumbers() {
		return showLineNumbers;
	}

	protected void showLineNumbers(boolean showLineNumbers) {
		this.showLineNumbers = showLineNumbers;
	}

	protected boolean edit() {
		return edit;
	}

	protected void edit(boolean edit) {
		this.edit = edit;
	}

	public void displayReports() {
		removeAll();
		if (reportsComponent == null) {
			// When selecting Reports in tree of Reports tab
			return;
		} else {
			// When selecting Reports in trees in Compare tab
			add(reportsComponent, 0);
		}
	}

	public void displayReport(Tree tree, TreePath treePath, DefaultMutableTreeNode node, Report report, Report reportCompare, boolean compare) {
		reportComponent.displayReport(node, getPathAsString(treePath), report, reportCompare, compare);
		removeAll();
		add(reportComponent, 0);
	}

	public void displayCheckpoint(Tree tree, TreePath treePath, DefaultMutableTreeNode node, 
			Report report, Checkpoint checkpoint, Checkpoint checkpointCompare, boolean compare) {
		checkpointComponent.displayCheckpoint(node, report, checkpoint, checkpointCompare, compare);
		removeAll();
		add(checkpointComponent, 0);
	}

	public void displayPath(Tree tree, TreePath treePath, DefaultMutableTreeNode node, String path) {
		pathComponent.displayPath(node, getPathAsString(treePath));
		removeAll();
		add(pathComponent, 0);
	}


	public void displayNothing() {
		removeAll();
	}

	private String getPathAsString(TreePath treePath) {
		String path = "";
		for (int i = 0; i < treePath.getPathCount(); i++) {
			path = path + treePath.getPathComponent(i);
			if (i < treePath.getPathCount() - 1) {
				path = path + ", ";
			}
		}
		return path;
	}

	public boolean hasChanges() {
		if (getComponentCount() > 0) {
			Component component = getComponent(0);
			if (component instanceof MessageComponent) {
				return ((MessageComponent)component).hasChanges();
			}
		}
		return false;
	}

	public void displayError(String message) {
		if (getComponentCount() > 0) {
			Component component = getComponent(0);
			((BaseComponent)component).displayError(message);
		}
	}

}
