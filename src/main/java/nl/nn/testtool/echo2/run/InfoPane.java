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
	private RunComponent runComponent;
	private BeanParent beanParent;

	public void setRunComponent(RunComponent runComponent) {
		this.runComponent = runComponent;
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
		runComponent.initBean(this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}


	public void display(String path) {
		// TODO direct op runComponent doen?
		runComponent.display(path);
		// TODO toch altijd hetzelfde component dus niet telkens verwijderen en toevoegen?
		removeAll();
		add(runComponent, 0);
	}
// TODO folder en report component samenvoegen of gemeenschappelijke basis geven zoals messageComponent dat is voor ...?
//	public void displayReport(Tree tree, TreePath treePath, DefaultMutableTreeNode node, Report report, Report reportCompare, boolean compare) {
//		reportComponent.displayReport(/*node, getPathAsString(treePath), report, reportCompare, compare*/);
//		removeAll();
//		add(reportComponent, 0);
//	}
}
