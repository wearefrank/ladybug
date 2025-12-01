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
package org.wearefrank.ladybug.echo2.test;

import java.util.Set;

import lombok.Setter;
import nextapp.echo2.app.ContentPane;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.echo2.BeanParent;
import org.wearefrank.ladybug.extensions.CustomReportAction;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.wearefrank.ladybug.echo2.Echo2Application;

/**
 * @author Jaco de Groot
 */
public class InfoPane extends ContentPane implements BeanParent {
	private static final long serialVersionUID = 1L;
	private @Setter TestTool testTool;
	private @Setter LogStorage debugStorage;
	private @Setter CrudStorage testStorage;
	private @Setter ReportXmlTransformer reportXmlTransformer;
	private @Setter CustomReportAction customReportAction;

	private TestComponent testComponent;
	private BeanParent beanParent;

	/**
	 * @see Echo2Application#initBean()
	 */
	public void initBean() {

		// Construct

		testComponent = new TestComponent();

		// Wire

		testComponent.setTestTool(testTool);
		testComponent.setDebugStorage(debugStorage);
		testComponent.setTestStorage(testStorage);
		testComponent.setReportXmlTransformer(reportXmlTransformer);
		testComponent.setCustomReportAction(customReportAction);

		// Init

		testComponent.initBean();
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		testComponent.initBean(this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}


	public void display(String path, Set<String> selectedStorageIds) {
		// TODO direct op testComponent doen?
		testComponent.display(path, selectedStorageIds);
		// TODO toch altijd hetzelfde component dus niet telkens verwijderen en toevoegen?
		removeAll();
		add(testComponent, 0);
	}
// TODO folder en report component samenvoegen of gemeenschappelijke basis geven zoals messageComponent dat is voor ...?
//	public void displayReport(Tree tree, TreePath treePath, DefaultMutableTreeNode node, Report report, Report reportCompare, boolean compare) {
//		reportComponent.displayReport(/*node, getPathAsString(treePath), report, reportCompare, compare*/);
//		removeAll();
//		add(reportComponent, 0);
//	}
}
