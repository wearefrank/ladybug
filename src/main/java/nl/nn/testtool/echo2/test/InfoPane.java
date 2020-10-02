/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.Set;

import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.echo2.BeanParent;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class InfoPane extends ContentPane implements BeanParent {
	private static final long serialVersionUID = 1L;
	private TestComponent testComponent;
	private BeanParent beanParent;

	public void setTestComponent(TestComponent testComponent) {
		this.testComponent = testComponent;
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
