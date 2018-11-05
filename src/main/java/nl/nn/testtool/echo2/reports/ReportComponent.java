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
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.FillImageBorder;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
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
public class ReportComponent extends MessageComponent implements ActionListener {
	private static final long serialVersionUID = 1L;
	private TestTool testTool;
	private CrudStorage runStorage;
	private TreePane treePane;
	private DefaultMutableTreeNode node;
	private Report report;
	private Label nameLabel;
	private Label descriptionLabel;
	private Label pathLabel;
	private Label storageIdLabel;
	private Label storageLabel;
	private SelectField stubStrategySelectField;
	private SelectField copyToSelectField;
	private SelectField downloadSelectField;
	private Label estimatedMemoryUsageLabel;
	private TextField nameTextField;
	private TextArea descriptionTextArea;
	private TextField pathTextField;
	private BeanParent beanParent;
	private Echo2Application echo2Application;
	private WindowPane deleteWarningWindow;
	private Label deleteIdLabel;

	public ReportComponent() {
		super();
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setRunStorage(CrudStorage runStorage) {
		this.runStorage = runStorage;
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

		Button copyButton = new Button("Copy");
		copyButton.setActionCommand("Copy");
		copyButton.addActionListener(this);
		Echo2Application.decorateButton(copyButton);
		buttonRow.add(copyButton);

		Button deleteButton = new Button("Delete");
		deleteButton.setActionCommand("Delete");
		deleteButton.addActionListener(this);
		Echo2Application.decorateButton(deleteButton);
		buttonRow.add(deleteButton);

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

		stubStrategySelectField = new SelectField(testTool.getStubStrategies().toArray());
		stubStrategySelectField.addActionListener(this);
		optionsRow.add(new Label("Stub strategy:"));
		optionsRow.add(stubStrategySelectField);

		// TODO werkend maken
		copyToSelectField = new SelectField(new String[]{runStorage.getName()});
		copyToSelectField.setSelectedIndex(0);
		optionsRow.add(new Label("Copy to:"));
		optionsRow.add(copyToSelectField);

		downloadSelectField = new SelectField(new String[]{"Both", "Report", "Message"});
		downloadSelectField.setSelectedIndex(0);
		optionsRow.add(new Label("Download:"));
		optionsRow.add(downloadSelectField);

		add(errorLabel);

		add(okayLabel);

		add(messageColumn);

		buttonRow = Echo2Application.getNewRow();
		add(buttonRow);

		Row nameRow = Echo2Application.getNewRow();
		nameRow.setInsets(new Insets(0, 5, 0, 0));
		add(nameRow);

		nameLabel = Echo2Application.createInfoLabel();
		nameRow.add(nameLabel);

		nameTextField = new TextField();
		nameTextField.setVisible(false);
		nameRow.add(nameTextField);

//		Row descriptionRow = Echo2Application.getNewRow();
//		descriptionRow.setInsets(new Insets(0, 5, 0, 0));
//		add(descriptionRow);

		descriptionLabel = Echo2Application.createInfoLabel();
//		descriptionRow.add(descriptionLabel);
		add(descriptionLabel);

		descriptionTextArea = new TextArea();
//		descriptionTextArea.setVisible(false);
		descriptionTextArea.setWidth(new Extent(100, Extent.PERCENT));
		descriptionTextArea.setHeight(new Extent(100));
		descriptionTextArea.setEnabled(false);
//		descriptionRow.add(descriptionTextArea);
		add(descriptionTextArea);

		Row pathRow = Echo2Application.getNewRow();
		pathRow.setInsets(new Insets(0, 5, 0, 0));
		add(pathRow);

		pathLabel = Echo2Application.createInfoLabel();
		pathRow.add(pathLabel);

		pathTextField = new TextField();
		pathTextField.setVisible(false);
		pathRow.add(pathTextField);

		storageIdLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(storageIdLabel);

		storageLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(storageLabel);

		estimatedMemoryUsageLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(estimatedMemoryUsageLabel);


		// "Are you sure?" window for delete button
		{
			Column deleteWarningColumn = new Column();

			// TODO een OptionsWindow class maken en in Echo2Application instantieren zoals TransformationWindow?
			deleteWarningWindow = new WindowPane();
			deleteWarningWindow.setVisible(false);
			deleteWarningWindow.setTitle("Warning");
			deleteWarningWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
			deleteWarningWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
			deleteWarningWindow.setWidth(new Extent(500));
			deleteWarningWindow.setHeight(new Extent(90));
			deleteWarningWindow.setInsets(new Insets(10, 5, 0, 0));
			deleteWarningWindow.add(deleteWarningColumn);
			deleteWarningWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
			deleteWarningWindow.init();

			Button yesButton  = new Button("Yes");
			yesButton.setActionCommand("DeleteYes");
			Echo2Application.decorateButton(yesButton);
			yesButton.addActionListener(this);

			Button noButton  = new Button("No");
			noButton.setActionCommand("DeleteNo");
			Echo2Application.decorateButton(noButton);
			noButton.addActionListener(this);

			deleteIdLabel = new Label("?");

			Row deleteWarningButtonRow = Echo2Application.getNewRow();
			deleteWarningButtonRow.setInsets(new Insets(0, 5, 0, 0));
			deleteWarningButtonRow.add(yesButton);
			deleteWarningButtonRow.add(noButton);

			deleteWarningColumn.add(deleteIdLabel);
			deleteWarningColumn.add(deleteWarningButtonRow);
		}

		super.initBeanPost();
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		echo2Application.getContentPane().add(deleteWarningWindow);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	public void displayReport(DefaultMutableTreeNode node, String path, Report report, Report reportCompare, boolean compare) {
		this.node = node;
		this.report = report;
		stubStrategySelectField.setSelectedItem(report.getStubStrategy());
		String reportXml = report.toXml();
		if (compare) {
			String reportCompareXml = null;
			if (reportCompare != null) {
				reportCompareXml = reportCompare.toXml();
			}
			setMessage(reportXml, reportCompareXml);
		} else {
			setMessage(reportXml);
		}
		updateNameLabelAndNameTextField();
		updateDescriptionLabelAndDescriptionTextArea();
		updatePathLabelAndPathTextField();
		storageIdLabel.setText("StorageId: " + report.getStorageId());
		storageLabel.setText("Storage: " + report.getStorage().getName());
		estimatedMemoryUsageLabel.setText("EstimatedMemoryUsage: " + report.getEstimatedMemoryUsage() + " bytes");
		errorLabel.setVisible(false);
		okayLabel.setVisible(false);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String errorMessage = null;
		String okayMessage = null;
		if (stubStrategySelectField == e.getSource()) {
			report.setStubStrategy((String)stubStrategySelectField.getSelectedItem());
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
				errorMessage = Download.download(report, true, true);
			} else if ("Report".equals(downloadSelectField.getSelectedItem())) {
				errorMessage = Download.download(report);
			} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
				errorMessage = Download.download(report, false, true);
			} else {
				errorMessage = "No download type selected";
			}
		} else if (e.getActionCommand().equals("LineNumbers")) {
			toggleShowLineNumbers();
		} else if (e.getActionCommand().equals("Edit")) {
			toggleEdit();
			updateNameLabelAndNameTextField();
			updateDescriptionLabelAndDescriptionTextArea();
			updatePathLabelAndPathTextField();
		} else if (e.getActionCommand().equals("Save")) {
			save();
			report.setName(nameTextField.getText());
			report.setDescription(descriptionTextArea.getText());
			report.setPath(pathTextField.getText());
			if (report.getStorage() instanceof CrudStorage) {
				Echo2Application.update((CrudStorage)report.getStorage(), report);
			}
		} else if (e.getActionCommand().equals("Rerun")) {
			errorMessage = testTool.rerun(report, echo2Application);
			if (errorMessage == null) {
				okayMessage = "Rerun succeeded";
			}
		} else if (e.getActionCommand().equals("Copy")) {
			//TODO voorkomen dat * en evt. andere chars er niet in komen?
			//misschien ook dwingen dat het met een / moet beginnen?
			//als je via reports tab ook deze repository kunt bekijken, kun je
			//altijd nog een reparatie uitvoeren
//			report.setPath("/test/");
//			report.setPath("/test/bla/");
//			report.setPath("/test/bla");
			errorMessage = Echo2Application.store(runStorage, report);
//		} else if (e.getActionCommand().equals("Update")) {
//			echo2Application.update(report);
		} else if (e.getActionCommand().equals("Delete")) {
			if (report.getStorage() instanceof CrudStorage) {
				deleteIdLabel.setText(
						"Are you sure you want to delete report with storage id "
						+ report.getStorageId() + "?");
				deleteWarningWindow.setVisible(true);
			} else {
				errorMessage = "Storage doesn't support delete method";
			}
		} else if (e.getActionCommand().equals("DeleteYes")
				|| e.getActionCommand().equals("DeleteNo")) {
			if (e.getActionCommand().equals("DeleteYes")) {
				errorMessage = Echo2Application.delete((CrudStorage)report.getStorage(), report);
			}
			deleteWarningWindow.setVisible(false);
			deleteIdLabel.setText("?");
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

	private void updateNameLabelAndNameTextField() {
		if (infoPane.edit()) {
			nameLabel.setText("Name: ");
			nameTextField.setText(report.getName());
			nameTextField.setVisible(true);
		} else {
			nameLabel.setText("Name: " + report.getName());
			nameTextField.setVisible(false);
		}
	}

	private void updateDescriptionLabelAndDescriptionTextArea() {
		if (infoPane.edit()) {
//			descriptionLabel.setText("Description: ");
//			descriptionTextArea.setText(report.getDescription());
			descriptionTextArea.setEnabled(true);
		} else {
//			descriptionLabel.setText("Description: " + report.getDescription());
			descriptionTextArea.setEnabled(false);
		}
		// TODO gewoon alleen eenmalig bij initBean o.i.d. doen?
		descriptionLabel.setText("Description:");
		descriptionTextArea.setText(report.getDescription());
	}

	private void updatePathLabelAndPathTextField() {
		if (infoPane.edit()) {
			pathLabel.setText("Path: ");
			pathTextField.setText(report.getPath());
			pathTextField.setVisible(true);
		} else {
			pathLabel.setText("Path: " + report.getPath());
			pathTextField.setVisible(false);
		}
	}

}
