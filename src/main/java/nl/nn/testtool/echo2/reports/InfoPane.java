/*
 * Created on 06-Nov-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BeanParent;
import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.TreePath;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class InfoPane extends ContentPane implements BeanParent {
	private static final long serialVersionUID = 1L;
	private ReportsComponent reportsComponent;
	private ReportComponent reportComponent;
	private PathComponent pathComponent;
	private CheckpointComponent checkpointComponent;
	private ErrorMessageComponent errorMessageComponent;
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

	public void setErrorMessageComponent(ErrorMessageComponent errorMessageComponent) {
		this.errorMessageComponent = errorMessageComponent;
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
		checkpointComponent.displayCheckpoint(node, getPathAsString(treePath), 
				report, checkpoint, checkpointCompare, compare);
		removeAll();
		add(checkpointComponent, 0);
	}

	public void displayPath(Tree tree, TreePath treePath, DefaultMutableTreeNode node, String path) {
		pathComponent.displayPath(node, getPathAsString(treePath));
		removeAll();
		add(pathComponent, 0);
	}

	public void displayErrorMessage(String errorMessage) {
		errorMessageComponent.displayErrorMessage(errorMessage);
		removeAll();
		add(errorMessageComponent, 0);
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
}
