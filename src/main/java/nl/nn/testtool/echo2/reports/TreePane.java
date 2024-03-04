/*
   Copyright 2020-2024 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.lang.invoke.MethodHandles;
import java.util.Enumeration;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.TreeNode;
import echopointng.tree.TreePath;
import echopointng.tree.TreeSelectionEvent;
import echopointng.tree.TreeSelectionListener;
import echopointng.tree.TreeSelectionModel;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;

/**
 * Jaco de Groot
 */
public class TreePane extends ContentPane implements TreeSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected InfoPane infoPane;
	private TreePane treePaneCounterpart;
	private ReportsTreeCellRenderer reportsTreeCellRenderer;
	private static final String ROOT_NODE_NAME = "Reports";
	protected DefaultMutableTreeNode rootNode;
	protected Tree tree;

	public TreePane() {
		super();
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
	}

	public void setTreePaneCounterpart(TreePane treePane) {
		treePaneCounterpart = treePane;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		Column layoutColumn = new Column();
		add(layoutColumn);
		reportsTreeCellRenderer = new ReportsTreeCellRenderer();
		rootNode = new DefaultMutableTreeNode(ROOT_NODE_NAME);
		DefaultTreeModel defaultTreeModel = new DefaultTreeModel(rootNode);
		tree = new Tree(defaultTreeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(reportsTreeCellRenderer); 
		tree.addTreeSelectionListener(this);
		layoutColumn.add(tree);
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void init() {
		super.init();
		selectNode(rootNode, null, false);
	}

	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultMutableTreeNode getReportNode(int reportIndex) {
		DefaultMutableTreeNode reportNode = null;
		if (reportIndex < rootNode.getChildCount()) {
			reportNode = (DefaultMutableTreeNode)rootNode.getChildAt(reportIndex);
		}
		return reportNode;
	}

	public Checkpoint getSelectedCheckpoint() {
		Checkpoint selectedCheckpoint = null;
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			Object[] path = selectionPath.getPath();
			if (path.length > 2) {
				selectedCheckpoint = (Checkpoint)((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).getUserObject();
			}
		}
		return selectedCheckpoint;
	}

	public DefaultMutableTreeNode findNodeWithEqualCheckpointPath(
			DefaultMutableTreeNode reportNode, Checkpoint checkpoint) {
		DefaultMutableTreeNode resultNode = null;
		if (reportNode.getChildCount() > 0) {
			DefaultMutableTreeNode checkpointRootNode = (DefaultMutableTreeNode)reportNode.getChildAt(0);
			Enumeration enumeration = checkpointRootNode.depthFirstEnumeration();
			while (resultNode == null && enumeration.hasMoreElements()) {
				DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
				Checkpoint currentCheckpoint = (Checkpoint)currentNode.getUserObject();
				if (currentCheckpoint.getPath().equals(checkpoint.getPath())
						&& currentCheckpoint.getType() == checkpoint.getType()) {
					resultNode = currentNode;
				}
			}
		}
		return resultNode;
	}

	public void selectNode(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode counterpartNode = null;
		boolean compare = false;
		if (treePaneCounterpart != null && node.getPath().length > 0) {
			if (node.getPath().length == 1) {
				treePaneCounterpart.selectNode(treePaneCounterpart.getRootNode(), null, false);
			} else {
				compare = true;
				int reportIndex = node.getRoot().getIndex(node.getPath()[1]);
				DefaultMutableTreeNode counterpartReportNode = treePaneCounterpart.getReportNode(reportIndex);
				if (counterpartReportNode != null) {
					if (node.getPath().length == 2) {
						counterpartNode = counterpartReportNode;
					} else {
						Checkpoint checkpoint = (Checkpoint)node.getUserObject();
						counterpartNode = treePaneCounterpart.findNodeWithEqualCheckpointPath(counterpartReportNode, checkpoint);
					}
				}
				if (counterpartNode != null) {
					treePaneCounterpart.selectNode(counterpartNode, node, compare);
				} else {
					treePaneCounterpart.unselect();
				}
			}
		}
		selectNode(node, counterpartNode, compare);
	}

	protected void selectNode(DefaultMutableTreeNode node,
			DefaultMutableTreeNode nodeFromOtherTree, boolean compare) {
		if (node != null) {
			Object userObject = node.getUserObject();
			TreePath treePath = new TreePath(node.getPath());
			tree.setSelectionPath(treePath);
			if (userObject instanceof Report) {
				Report report = (Report)node.getUserObject();
				Report reportFromOtherTree = null;
				if (nodeFromOtherTree != null) {
					reportFromOtherTree = (Report)nodeFromOtherTree.getUserObject();
				}
				log.debug("Display report: " + report.getName());
				infoPane.displayReport(tree, treePath, node, report, reportFromOtherTree, compare);
			} else if (node.getUserObject() instanceof Checkpoint) {
				Report report = getReportParent(node);
				Checkpoint checkpoint = (Checkpoint)userObject;
				Checkpoint checkpointFromOtherTree = null;
				if (nodeFromOtherTree != null) {
					checkpointFromOtherTree = (Checkpoint)nodeFromOtherTree.getUserObject();
				}
				log.debug("Display checkpoint: " + checkpoint.getName());
				infoPane.displayCheckpoint(tree, treePath, node, report, checkpoint, checkpointFromOtherTree, compare);
			} else if (userObject instanceof String) {
				String name = treePath.getPathComponent(treePath.getPathCount() - 1).toString();
				if (treePath.getPathCount() == 1) {
					log.debug("Display reports: " + name);
					infoPane.displayReports();
				} else {
					log.debug("Display path: " + name);
					infoPane.displayPath(tree, treePath, node, (String)userObject);
				}
			} else {
				log.debug("Display nothing");
				infoPane.displayNothing();
			}
		}
	}

	public void unselect() {
		tree.removeSelectionPaths(tree.getSelectionPaths());
	}

	public void expandDirectChilds() {
		tree.expandPath(new TreePath(rootNode));
	}

	public void expandAll() {
		tree.expandAll();
	}

	public void expandAll(DefaultMutableTreeNode node) {
		tree.expandPath(new TreePath(node.getPath()));
		Enumeration enumeration = node.depthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			node = (DefaultMutableTreeNode)enumeration.nextElement();
			tree.expandPath(new TreePath(node.getPath()));
		}
	}

	public void collapseAll() {
		tree.collapseAll();
	}

	public void collapseAll(DefaultMutableTreeNode node) {
		tree.collapsePath(new TreePath(node.getPath()));
		Enumeration enumeration = node.depthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			node = (DefaultMutableTreeNode)enumeration.nextElement();
			tree.collapsePath(new TreePath(node.getPath()));
		}
	}

	// Made synchronized because it is called from ReportUploadListener
	synchronized public DefaultMutableTreeNode addReport(Report report, View view, boolean sortReports) {
		int insertPosition = 0;
		if (sortReports) {
			insertPosition = -1;
			for (int i = rootNode.getChildCount() - 1; i > -1 && insertPosition == -1; i--) {
				DefaultMutableTreeNode currentReportNode = (DefaultMutableTreeNode)rootNode.getChildAt(i);
				Report currentReport = (Report)currentReportNode.getUserObject();
				if (report.toXml().compareTo(currentReport.toXml()) > 0) {
					insertPosition = i + 1;
				}
			}
			if (insertPosition == -1) {
				insertPosition = 0;
			}
		}
		DefaultMutableTreeNode reportNode = new DefaultMutableTreeNode(report);
		rootNode.insert(reportNode, insertPosition);
		addCheckpoints(reportNode, view);
		DefaultMutableTreeNode nodeToSelect = expandOnlyChilds(reportNode);
		selectNode(nodeToSelect);
		return reportNode;
	}

	public DefaultMutableTreeNode expandOnlyChilds(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode nodeToExpandTo = node;
		while (nodeToExpandTo.getChildCount() == 1
					&& isStartpoint(((Checkpoint)((DefaultMutableTreeNode)nodeToExpandTo.getFirstChild()).getUserObject()).getType())) {
			tree.expandPath(new TreePath(nodeToExpandTo.getPath()));
			nodeToExpandTo = (DefaultMutableTreeNode)nodeToExpandTo.getFirstChild();
		}
		tree.expandPath(new TreePath(nodeToExpandTo.getPath()));
		return nodeToExpandTo;
	}

	public void addCheckpoints(DefaultMutableTreeNode reportNode, View view) {
		Report report = (Report)reportNode.getUserObject();
		Iterator checkpointsIterator = report.getCheckpoints().iterator();
		while (checkpointsIterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint)checkpointsIterator.next();
			if (view.showCheckpoint(report, checkpoint)) {
				reportNode = addCheckpoint(reportNode, checkpoint);
			}
		}
	}

	public DefaultMutableTreeNode addCheckpoint(DefaultMutableTreeNode parentNode, Checkpoint checkpoint) {
		if (parentNode.getChildCount() > 0) {
			int lastChildLevel = ((Checkpoint)((DefaultMutableTreeNode)parentNode.getLastChild()).getUserObject()).getLevel();
			if (checkpoint.getLevel() < 0) {
				// Ignore. See also INVALID LEVEL in Checkpoint.getPath(boolean checkpointInProgress)
			} else if (checkpoint.getLevel() > lastChildLevel) {
				// Increase level
				parentNode = (DefaultMutableTreeNode)parentNode.getLastChild();
			} else if (checkpoint.getLevel() < lastChildLevel) {
				// Decrease level
				parentNode = (DefaultMutableTreeNode)parentNode.getParent();
			}
		}
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(checkpoint);
		parentNode.add(node);
		return parentNode;
	}

	public void closeReport(Report report) {
		int childToSelect = -1;
		for (int i = 0; i < rootNode.getChildCount(); i++) {
			DefaultMutableTreeNode reportNode = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			// TODO is dit wel een goede check?
			if (report.toString().equals(reportNode.getUserObject().toString())) {
				rootNode.remove(i);
				if (i < rootNode.getChildCount()) {
					childToSelect = i;
				} else {
					childToSelect = i - 1;
				}
			}
		}
		if (childToSelect != -1) {
			tree.setSelectionPath(new TreePath(new TreePath(rootNode), rootNode.getChildAt(childToSelect)));
		} else {
			tree.setSelectionPath(new TreePath(rootNode));
		}
	}

	public void closeAllReports() {
		rootNode.removeAllChildren();
		selectNode(rootNode);
		// When rootNode was already selected before calling this method the
		// tree isn't updated, calling setVisible will fix this.
		tree.setVisible(false);
		tree.setVisible(true);
	}

	public void valueChanged(TreeSelectionEvent e) {
		if (e.getNewLeadSelectionPath() != null) {
			if (infoPane != null && infoPane.hasChanges()) {
				infoPane.displayError("First Save or change to Read-only");
				unselect();
			} else {
				TreePath treePath = e.getNewLeadSelectionPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
				selectNode(node);
			}
		}
	}

	private Report getReportParent(DefaultMutableTreeNode node) {
        Object o = node.getUserObject();
        TreeNode parent = node.getParent();
        if (o instanceof Report) {
            return (Report) o;
        } else if (parent == null || !(parent instanceof DefaultMutableTreeNode)) {
            return null;
        } else {
            return getReportParent((DefaultMutableTreeNode) parent);
        }
    }

	public Storage getStorage() throws StorageException {
		CrudStorage storage = new nl.nn.testtool.storage.memory.Storage();
		for (int i = 0; i < rootNode.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			storage.store((Report)node.getUserObject());
		}
		return storage;
	}

	public void redisplayReports(View view) {
		if (rootNode.getChildCount() > 0) {
			DefaultMutableTreeNode selectedReportNode = getReportNodeFromSelectionPath();
			Checkpoint selectedCheckpoint = getSelectedCheckpoint();
			for (int i = 0; i < rootNode.getChildCount(); i++) {
				DefaultMutableTreeNode reportNode = (DefaultMutableTreeNode)rootNode.getChildAt(i);
				reportNode.removeAllChildren();
				addCheckpoints(reportNode, view);
				expandOnlyChilds(reportNode);
			}
			if (selectedReportNode != null) {
				DefaultMutableTreeNode nodeToSelect = null;
				if (selectedCheckpoint != null) {
					nodeToSelect = findNodeWithEqualCheckpointPath(selectedReportNode, selectedCheckpoint);
					if (nodeToSelect != null) {
						expandOnlyChilds(nodeToSelect);
					}
				}
				if (nodeToSelect == null) {
					nodeToSelect = expandOnlyChilds(selectedReportNode);
				}
				selectNode(nodeToSelect);
			}
		}
	}

	public DefaultMutableTreeNode getReportNodeFromSelectionPath() {
		DefaultMutableTreeNode reportNodeFromSelectionPath = null;
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			Object[] path = selectionPath.getPath();
			if (path.length > 1) {
				reportNodeFromSelectionPath = (DefaultMutableTreeNode)path[1];
			}
		}
		return reportNodeFromSelectionPath;
	}

	public ReportsTreeCellRenderer getReportsTreeCellRenderer() {
		return reportsTreeCellRenderer;
	}

	private boolean isStartpoint(int type) {
		if (type == Checkpoint.TYPE_STARTPOINT
				|| type == Checkpoint.TYPE_THREADSTARTPOINT) {
			return true;
		} else {
			return false;
		}
	}

}

