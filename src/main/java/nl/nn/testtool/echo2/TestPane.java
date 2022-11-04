/*
   Copyright 2020-2022 WeAreFrank!, 2018 Nationale-Nederlanden

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

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import nl.nn.testtool.echo2.test.InfoPane;
import nl.nn.testtool.echo2.test.TreePane;

@Dependent
public class TestPane extends Tab implements BeanParent {
	private static final long serialVersionUID = 1L;
	private String title = "Test";
	@Inject
	private TreePane treePane;
	@Inject
	private InfoPane infoPane;

	public TestPane() {
		super();
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
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

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@PostConstruct
	public void initBean() {

		// Construct

		TabPaneLayoutData tabPaneLayoutTestPane = new TabPaneLayoutData();
		tabPaneLayoutTestPane.setTitle(title);
		setLayoutData(tabPaneLayoutTestPane);

		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane1.setResizable(true);
		splitPane1.setSeparatorPosition(new Extent(280, Extent.PX));

		// Wire

		splitPane1.add(treePane);
		splitPane1.add(infoPane);
		add(splitPane1);

	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		treePane.initBean(this);
		infoPane.initBean(this);
	}

}