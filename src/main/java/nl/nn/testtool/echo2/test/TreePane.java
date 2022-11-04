/*
   Copyright 2020, 2022 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.TreePath;
import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.TestPane;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;

@Dependent @Named("testTreePane")
public class TreePane extends nl.nn.testtool.echo2.reports.TreePane implements BeanParent {
	private static final long serialVersionUID = 1L;
	private InfoPane infoPane;
	private BeanParent beanParent;
	@Setter @Inject
	private CrudStorage storage;
	private List<Integer> reportsWithDirtyPaths = new ArrayList<Integer>();

	@Override
	public void setInfoPane(nl.nn.testtool.echo2.reports.InfoPane infoPane) {
		throw new RuntimeException("Not implemented");
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		TestPane testPane = (TestPane)beanParent;
		infoPane = testPane.getInfoPane();
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@Override
	public void init() {
		super.init();
		redisplayReports((String)null, null);
	}

	@Override
	public void selectNode(DefaultMutableTreeNode node) {
		selectNode(node, null);
	}

	@Override
	protected void selectNode(DefaultMutableTreeNode node, DefaultMutableTreeNode nodeFromOtherTree, boolean compare) {
		selectNode(node, null);
	}

	protected void selectNode(DefaultMutableTreeNode node, Set<String> selectedStorageIds) {
		if (node != null) {
			TreePath treePath = new TreePath(node.getPath());
			tree.setSelectionPath(treePath);
			String path = "";
			for (int i = 1; i < treePath.getPath().length; i++) {
				path = path + "/" + treePath.getPath()[i];
			}
			path = path + "/";
			log.debug("Display: " + path);
			infoPane.display(path, selectedStorageIds);
		}
	}

	@Override
	synchronized public DefaultMutableTreeNode addReport(Report report, View view, boolean sortReports) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Storage getStorage() throws StorageException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void redisplayReports(View view) {
		throw new RuntimeException("Not implemented");
	}

	public void redisplayReports(String selectPath, Set<String> selectedStorageIds) {
		rootNode.removeAllChildren();
		reportsWithDirtyPaths.clear();
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("path");
		List<List<Object>> metadata = null;
		try {
			metadata = storage.getMetadata(-1, metadataNames, null, MetadataExtractor.VALUE_TYPE_OBJECT);
		} catch (StorageException e) {
			// TODO iets doen
			e.printStackTrace();
		}
		List<String> pathsToAdd = new ArrayList<String>();
		Iterator<List<Object>> metadataIterator = metadata.iterator();
		while (metadataIterator.hasNext()) {
			List<Object> metadataRecord = metadataIterator.next();
			Integer storageId = (Integer)metadataRecord.get(0);
			String path = (String)metadataRecord.get(1);
			if (path == null || !path.equals(TestComponent.normalizePath(path))) {
				reportsWithDirtyPaths.add(storageId);
			} else if (path.length() > 1) {
				pathsToAdd.add(path);
			}
		}
		Collections.sort(pathsToAdd);
		DefaultMutableTreeNode selectNode = addPaths(pathsToAdd, rootNode, selectPath);
		if (selectNode == null) {
			selectNode = rootNode;
		}
		selectNode(selectNode, selectedStorageIds);
		tree.collapseAll();
		tree.expandAll();
	}

	private DefaultMutableTreeNode addPaths(List<String> paths,
			DefaultMutableTreeNode rootNode, String pathOfNodeToReturn) {
		DefaultMutableTreeNode returnNode = null;
		while (paths.size() > 0) {
			// Add first element of every path
			String path = (String)paths.get(0);
			String firstElement = path.substring(1, path.indexOf('/', 1));
			DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(firstElement);
			rootNode.add(subNode);
			if (pathOfNodeToReturn != null) {
				if (pathOfNodeToReturn.equals("/")) {
					returnNode = rootNode;
				} else {
					if (path.startsWith(pathOfNodeToReturn)) {
						returnNode = subNode;
					}
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
			if (subPaths.size() > 0) {
				String subPathOfNodeToReturn = null;
				if (pathOfNodeToReturn != null && pathOfNodeToReturn.startsWith("/" + firstElement + "/")) {
					subPathOfNodeToReturn = pathOfNodeToReturn.substring(firstElement.length() + 1);
				}
				DefaultMutableTreeNode returnedNode = addPaths(subPaths, subNode, subPathOfNodeToReturn);
				if (returnedNode != null) {
					returnNode = returnedNode;
				}
			}
		}
		return returnNode;
	}

	public List<Integer> getReportsWithDirtyPaths() {
		return reportsWithDirtyPaths;
	}

}