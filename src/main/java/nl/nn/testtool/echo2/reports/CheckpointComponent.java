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
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.RadioButton;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.button.ButtonGroup;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.ReportPane;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.storage.CrudStorage;
import echopointng.tree.DefaultMutableTreeNode;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CheckpointComponent extends MessageComponent implements ActionListener {
	private static final long serialVersionUID = 1L;
	private TestTool testTool;
	private TreePane treePane;
	private DefaultMutableTreeNode node;
	private Checkpoint checkpoint;
	private Label nameLabel;
	private Label threadNameLabel;
	private Label sourceClassNameLabel;
	private Label pathLabel;
	private RadioButton radioButtonStubOptionFollowReportStrategy;
	private RadioButton radioButtonStubOptionYes;
	private RadioButton radioButtonStubOptionNo;
	private Label messageHasBeenStubbedLabel;
	private SelectField downloadSelectField;
	private Label estimatedMemoryUsageLabel;
	private Report report;
	private BeanParent beanParent;
	private Echo2Application echo2Application;

	public CheckpointComponent() {
		super();
	}
	
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}
	
	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}
	
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		super.initBeanPre();

		Row buttonRow = Echo2Application.getNewRow();
		add(buttonRow);

		Button rerunButton = new Button("Rerun");
		rerunButton.setActionCommand("Rerun");
		rerunButton.addActionListener(this);
		Echo2Application.decorateButton(rerunButton);
		buttonRow.add(rerunButton);

		editButton = new Button();
		editButton.setActionCommand("Edit");
		editButton.addActionListener(this);
		Echo2Application.decorateButton(editButton);
		buttonRow.add(editButton);

		lineNumbersButton = new Button();
		lineNumbersButton.setActionCommand("LineNumbers");
		lineNumbersButton.addActionListener(this);
		Echo2Application.decorateButton(lineNumbersButton);
		buttonRow.add(lineNumbersButton);

		saveButton = new Button("Save");
		saveButton.setActionCommand("Save");
		saveButton.addActionListener(this);
		Echo2Application.decorateButton(saveButton);
		buttonRow.add(saveButton);

		//TODO copy en delete hier ook toevoegen (ook copy-to select field) of alle report specifieke dingen (download) hier weghalen?
		
		Button downloadButton = new Button("Download");
		downloadButton.setActionCommand("Download");
		downloadButton.addActionListener(this);
		Echo2Application.decorateButton(downloadButton);
		buttonRow.add(downloadButton);

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

		Button closeButton = new Button("Close");
		closeButton.setActionCommand("Close");
		closeButton.addActionListener(this);
		Echo2Application.decorateButton(closeButton);
		buttonRow.add(closeButton);

		Row optionsRow = Echo2Application.getNewRow();
		optionsRow.setInsets(new Insets(0, 5, 0, 0));
		add(optionsRow);

		optionsRow.add(new Label("Stub:"));
		ButtonGroup stubButtonGroup = new ButtonGroup();
		radioButtonStubOptionYes = new RadioButton("Yes");
		Echo2Application.decorateRadioButton(radioButtonStubOptionYes);
		radioButtonStubOptionYes.setGroup(stubButtonGroup);
		radioButtonStubOptionYes.addActionListener(this);
		optionsRow.add(radioButtonStubOptionYes);
		radioButtonStubOptionNo = new RadioButton("No");
		Echo2Application.decorateRadioButton(radioButtonStubOptionNo);
		radioButtonStubOptionNo.setGroup(stubButtonGroup);
		radioButtonStubOptionNo.addActionListener(this);
		optionsRow.add(radioButtonStubOptionNo);
		radioButtonStubOptionFollowReportStrategy = new RadioButton("Follow report strategy");
		Echo2Application.decorateRadioButton(radioButtonStubOptionFollowReportStrategy);
		radioButtonStubOptionFollowReportStrategy.setGroup(stubButtonGroup);
		radioButtonStubOptionFollowReportStrategy.addActionListener(this);
		optionsRow.add(radioButtonStubOptionFollowReportStrategy);

		downloadSelectField = new SelectField(new String[]{"Both", "Report", "Message"});
		downloadSelectField.setSelectedIndex(0);
		optionsRow.add(new Label("Download:"));
		optionsRow.add(downloadSelectField);

		add(errorLabel);

		add(okayLabel);

		messageHasBeenStubbedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageHasBeenStubbedLabel.setVisible(false);
		messageHasBeenStubbedLabel.setText("Message has been stubbed (copied from original report)");
		add(messageHasBeenStubbedLabel);

		add(messageColumn);

		nameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(nameLabel);
		
		threadNameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(threadNameLabel);
		
		sourceClassNameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(sourceClassNameLabel);
		
		pathLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(pathLabel);

		estimatedMemoryUsageLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(estimatedMemoryUsageLabel);

		super.initBeanPost();
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	public void displayCheckpoint(DefaultMutableTreeNode node, String path, 
			Report report, Checkpoint checkpoint, Checkpoint checkpointCompare, boolean compare) {
		this.node = node;
		this.report = report;
		this.checkpoint = checkpoint;
		if (checkpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
			radioButtonStubOptionFollowReportStrategy.setSelected(true);
		} else if (checkpoint.getStub() == Checkpoint.STUB_NO) {
			radioButtonStubOptionNo.setSelected(true);
		} else if (checkpoint.getStub() == Checkpoint.STUB_YES) {
			radioButtonStubOptionYes.setSelected(true);
		} else {
			radioButtonStubOptionFollowReportStrategy.setSelected(false);
			radioButtonStubOptionYes.setSelected(false);
			radioButtonStubOptionNo.setSelected(false);
		}
		messageHasBeenStubbedLabel.setVisible(checkpoint.getMessageHasBeenStubbed());
		String message = null;
		if (checkpoint.getMessage() != null) {
			message = checkpoint.getMessage();
		}
		if (compare) {
			String messageCompare = null;
			if (checkpointCompare != null) {
				messageCompare = checkpointCompare.getMessage();
			}
			setMessage(message, messageCompare);
		} else {
			setMessage(message);
		}
		nameLabel.setText("Name: " + checkpoint.getName());
		threadNameLabel.setText("Thread name: " + checkpoint.getThreadName());
		sourceClassNameLabel.setText("Source class name: " + checkpoint.getSourceClassName());
		pathLabel.setText("Path: " + path);
		estimatedMemoryUsageLabel.setText("EstimatedMemoryUsage: " + checkpoint.getEstimatedMemoryUsage() + " bytes");
		errorLabel.setVisible(false);
		okayLabel.setVisible(false);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String errorMessage = null;
		String okayMessage = null;
		if (radioButtonStubOptionFollowReportStrategy == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_FOLLOW_REPORT_STRATEGY);
		} else if (radioButtonStubOptionNo == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_NO);
		} else if (radioButtonStubOptionYes == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_YES);
		} else if (e.getActionCommand().equals("ExpandAll")) {
			treePane.expandAll(node);
		} else if (e.getActionCommand().equals("CollapseAll")) {
			treePane.collapseAll(node);
		} else if (e.getActionCommand().equals("Close")) {
			if (getParent().getParent().getParent() instanceof ReportPane) {
				((Echo2Application)getApplicationInstance()).closeReport();
			} else {
				treePane.closeReport(report);
			}
		} else if (e.getActionCommand().equals("Download")) {
			if ("Both".equals(downloadSelectField.getSelectedItem())) {
				errorMessage = Download.download(report, checkpoint);
			} else if ("Report".equals(downloadSelectField.getSelectedItem())) {
				errorMessage = Download.download(report);
			} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
				errorMessage = Download.download(checkpoint);
			} else {
				errorMessage = "No download type selected";
			}
		} else if (e.getActionCommand().equals("LineNumbers")) {
			toggleShowLineNumbers();
		} else if (e.getActionCommand().equals("Edit")) {
			toggleEdit();
		} else if (e.getActionCommand().equals("Save")) {
			checkpoint.setMessage(save());
			if (report.getStorage() instanceof CrudStorage) {
				errorMessage = Echo2Application.update((CrudStorage)report.getStorage(), report);
			}
		} else if (e.getActionCommand().equals("Rerun")) {
			errorMessage = testTool.rerun(checkpoint.getReport(), echo2Application);
			if (errorMessage == null) {
				okayMessage = "Rerun succeeded";
			}
		}
		if (errorMessage != null) {
			errorLabel.setText(errorMessage);
			errorLabel.setVisible(true);
		} else {
			if (okayMessage != null) {
				okayLabel.setText(okayMessage);
				okayLabel.setVisible(true);
			}
		}
	}
}
