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
package nl.nn.testtool.echo2.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.RunPane;
import nl.nn.testtool.echo2.reports.ReportsTreeCellRenderer;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.TreeNode;
import echopointng.tree.TreePath;
import echopointng.tree.TreeSelectionEvent;
import echopointng.tree.TreeSelectionListener;
import echopointng.tree.TreeSelectionModel;

public class TreePane extends ContentPane implements BeanParent, TreeSelectionListener {
	private static final long serialVersionUID = 1L;
	private Logger log = LogUtil.getLogger(this);
//	private TestTool testTool;
	private InfoPane infoPane;
	private TreePane treePaneCounterpart;
	private ReportsTreeCellRenderer reportsTreeCellRenderer;
	private static final String ROOT_NODE_NAME = "Reports";
	private DefaultMutableTreeNode rootNode;
	private Tree tree;
	private BeanParent beanParent;

	private List reportsWithDirtyPaths = new ArrayList();

	private Storage storage;

	public TreePane() {
		super();
	}

	public void setTreePaneCounterpart(TreePane treePane) {
		treePaneCounterpart = treePane;
	}
	
	public void setReportsTreeCellRenderer(ReportsTreeCellRenderer reportsTreeCellRenderer) {
		this.reportsTreeCellRenderer = reportsTreeCellRenderer;
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		Column layoutColumn = new Column();
		add(layoutColumn);
		rootNode = new DefaultMutableTreeNode(ROOT_NODE_NAME);
		DefaultTreeModel defaultTreeModel = new DefaultTreeModel(rootNode);
		tree = new Tree(defaultTreeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(reportsTreeCellRenderer); 
		tree.addTreeSelectionListener(this);
		layoutColumn.add(tree);

		// TODO new stuff
//		int insertPosition = 0;
//		List children = storage.getTreeChildren("/");
//		if (children.size() == 0) {
//			try {
//				// TODO getStorageIds() met een param "/" doen
//				List list = storage.getStorageIds();
//				Iterator iterator = list.iterator();
//				while (iterator.hasNext()) {
//					Report report = storage.getReport((Integer)iterator.next());
//					DefaultMutableTreeNode reportNode = new DefaultMutableTreeNode(report);
//					rootNode.insert(reportNode, insertPosition);
//				}
//			} catch(StorageException e) {
//				// TODO iets anders bedenken
//				System.err.println(e.getMessage());
//				e.printStackTrace(System.err);
//			}
//		} else {
//			Iterator iterator = children.iterator();
//			while (iterator.hasNext()) {
//				String child = (String)iterator.next();
//				DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(child);
//				rootNode.insert(folderNode, insertPosition);
//			}
//		}
		
		
		
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		RunPane runPane = (RunPane)beanParent;
		infoPane = runPane.getInfoPane();
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	private DefaultMutableTreeNode addPaths(List paths,
			DefaultMutableTreeNode rootNode, String pathOfNodeToReturn) {
		DefaultMutableTreeNode returnNode = null;
		while (paths.size() > 0) {
			// Add first element of every path
			String path = (String)paths.get(0);
			String firstElement = path.substring(1, path.indexOf('/', 1));
			DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(firstElement);
			rootNode.add(subNode);
			String subPathOfNodeToReturn = null;
			if (pathOfNodeToReturn != null) {
				if (path.equals(pathOfNodeToReturn)) {
					returnNode = subNode;
				}
				if (pathOfNodeToReturn.startsWith(path)) {
					subPathOfNodeToReturn = pathOfNodeToReturn.substring(firstElement.length() + 1);
				}
			}
			// For all paths with the same first element call this method
			// recursively for the remaining elements of the path and remove the
			// path from this iteration.
			List<String> subPaths = new ArrayList<String>();
			int i = 0;
			while (i < paths.size()) {
				path = (String)paths.get(i);
				if (path.startsWith("/" + firstElement + "/")) {
					String subPath = (String)paths.remove(i);
					if (subPath.length() > firstElement.length() + 2) {
						subPath = subPath.substring(firstElement.length() + 1);
						subPaths.add(subPath);
					}
				} else {
					i++;
				}
			}
			DefaultMutableTreeNode returnedNode = addPaths(subPaths, subNode, subPathOfNodeToReturn);
			if (returnedNode != null) {
				returnNode = returnedNode;
			}
		}
		return returnNode;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void init() {
		super.init();
		selectNode(rootNode, null, false);
		
		
		redisplayReports(null);
		
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
		DefaultMutableTreeNode checkpointRootNode = (DefaultMutableTreeNode)reportNode.getChildAt(0);
		Enumeration enumeration = checkpointRootNode.depthFirstEnumeration();
		while (resultNode == null && enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			Checkpoint currentCheckpoint = (Checkpoint)currentNode.getUserObject();
			if (currentCheckpoint.getPath().equals(checkpoint.getPath())) {
				resultNode = currentNode;
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

	private void selectNode(DefaultMutableTreeNode node,
			DefaultMutableTreeNode nodeFromOtherTree, boolean compare) {
		if (node != null) {
			Object userObject = node.getUserObject();
			TreePath treePath = new TreePath(node.getPath());
			tree.setSelectionPath(treePath);
//			if (userObject instanceof Report) {
//				Report report = (Report)node.getUserObject();
//				Report reportFromOtherTree = null;
//				if (nodeFromOtherTree != null) {
//					reportFromOtherTree = (Report)nodeFromOtherTree.getUserObject();
//				}
//				log.debug("Display report: " + report.getName());
//				infoPane.displayReport(tree, treePath, node, report, reportFromOtherTree, compare);
//			} else if (node.getUserObject() instanceof Checkpoint) {
//				Report report = getReportParent(node);
//				Checkpoint checkpoint = (Checkpoint)userObject;
//				Checkpoint checkpointFromOtherTree = null;
//				if (nodeFromOtherTree != null) {
//					checkpointFromOtherTree = (Checkpoint)nodeFromOtherTree.getUserObject();
//				}
//				log.debug("Display checkpoint: " + checkpoint.getName());
//				infoPane.displayCheckpoint(tree, treePath, node, report, checkpoint, checkpointFromOtherTree, compare);
//			} else if (userObject instanceof String) {
//				String name = treePath.getPathComponent(treePath.getPathCount() - 1).toString();
//				if (treePath.getPathCount() == 1) {
//					log.debug("Display reports: " + name);
//					infoPane.displayReports();
//				} else {
//					log.debug("Display path: " + name);
//					infoPane.displayPath(tree, treePath, node, (String)userObject);
//				}
				String path = "";
				for (int i = 1; i < treePath.getPath().length; i++) {
					path = path + "/" + treePath.getPath()[i];
				}
				path = path + "/";
				//(String)userObject
				log.debug("Display: " + path);
				// TODO misschien beter een event als "node selected" o.i.d. doorgeven?
//				Echo2ApplicationEvent echo2ApplicationEvent = new Echo2ApplicationEvent(this);
//				echo2ApplicationEvent.setCommand("DisplayPath");
//				echo2ApplicationEvent.setCustomObject(path);
//				applicationContext.publishEvent(echo2ApplicationEvent);
				infoPane.display(path);
//			} else {
//				log.debug("Display nothing");
//				infoPane.displayNothing();
//			}
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
			int lastChildType = ((Checkpoint)((DefaultMutableTreeNode)parentNode.getLastChild()).getUserObject()).getType();
			if (isStartpoint(lastChildType)) {
				// Increase level
				parentNode = (DefaultMutableTreeNode)parentNode.getLastChild();
			} else if (isEndpoint(lastChildType)) {
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
		if (e.getNewLeadSelectionPath() == null) {
//			infoPane.displayNothing();
		} else {
			TreePath treePath = e.getNewLeadSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
//			Object userObject = node.getUserObject();
			selectNode(node);
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

// TODO misschien beter gewoon getReports() die een List terug geeft gebruiken? wordt mem storage dan nog gebruikt?
	public Storage getStorage() throws StorageException {
		CrudStorage storage = new nl.nn.testtool.storage.memory.Storage();
		for (int i = 0; i < rootNode.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			storage.store((Report)node.getUserObject());
		}
		return storage;
	}

	public List getReports() throws StorageException {
		List reports = new ArrayList();
		for (int i = 0; i < rootNode.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			reports.add((Report)node.getUserObject());
		}
		return reports;
	}

	public List getReportsWithDirtyPaths() {
		return reportsWithDirtyPaths;
	}
	
	// TODO anders noemen? is copy van andere treepane
	// TODO deze zware bewerkingen (is tie zwaar?) pas doen als pane of root node voor het eerst wordt aangeklikt?
	public void redisplayReports(String selectPath) {
		rootNode.removeAllChildren();
		reportsWithDirtyPaths.clear();
		List metadataNames = new ArrayList();
		metadataNames.add("storageId");
		metadataNames.add("path");
		List metadata = null;
		try {
			metadata = storage.getMetadata(-1, metadataNames, null, MetadataExtractor.VALUE_TYPE_OBJECT);
		} catch (StorageException e) {
			// TODO iets doen
			e.printStackTrace();
		}
		List cleanedPaths = new ArrayList();
		Iterator metadataIterator = metadata.iterator();
		while (metadataIterator.hasNext()) {
			List metadataRecord = (List)metadataIterator.next();
			Integer storageId = (Integer)metadataRecord.get(0);
			String path = (String)metadataRecord.get(1);
			if (path != null && path.startsWith("/") && path.endsWith("/") && path.length() > 1) {
				// TODO nog checken op dubbele slashes?
				cleanedPaths.add(path);
			} else {
				reportsWithDirtyPaths.add(storageId);
			}
		}
		Collections.sort(cleanedPaths);
		DefaultMutableTreeNode selectNode = addPaths(cleanedPaths, rootNode, selectPath);
		if (selectNode == null) {
			selectNode = rootNode;
		}
		selectNode(selectNode);
		tree.collapseAll();
		tree.expandAll();
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

	private boolean isStartpoint(int type) {
		if (type == Checkpoint.TYPE_STARTPOINT
				|| type == Checkpoint.TYPE_THREADSTARTPOINT) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isEndpoint(int type) {
		if (type == Checkpoint.TYPE_ENDPOINT
				|| type == Checkpoint.TYPE_THREADENDPOINT
				|| type == Checkpoint.TYPE_ABORTPOINT) {
			return true;
		} else {
			return false;
		}
	}

}

