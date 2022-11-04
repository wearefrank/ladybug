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
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nl.nn.testtool.echo2.Echo2Application;

import javax.annotation.PostConstruct;

import echopointng.tree.DefaultMutableTreeNode;

/**
 * @author Jaco de Groot
 */
public class PathComponent extends Column implements ActionListener {
	private TreePane treePane;
	private DefaultMutableTreeNode node;
	private Label nameLabel;
	private Label stringLabel;

	public PathComponent() {
		super();
	}
	
	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}
	
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@PostConstruct
	public void initBean() {
		setInsets(new Insets(10));
		Row buttonRow = Echo2Application.getNewRow();
		add(buttonRow);

		Button expandAll  = new Button("Expand all");
		expandAll.setActionCommand("ExpandAll");
		Echo2Application.decorateButton(expandAll);
		expandAll.addActionListener(this);
		buttonRow.add(expandAll);

		Button collapseAll  = new Button("Collapse all");
		collapseAll.setActionCommand("CollapseAll");
		Echo2Application.decorateButton(collapseAll);
		collapseAll.addActionListener(this);
		buttonRow.add(collapseAll);

		nameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(nameLabel);
	}

	public void displayPath(DefaultMutableTreeNode node, String path) {
		this.node = node;
		nameLabel.setText("Path: " + path);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("ExpandAll")) {
			treePane.expandAll(node);
		} else if (e.getActionCommand().equals("CollapseAll")) {
			treePane.collapseAll(node);
		}
	}
}
