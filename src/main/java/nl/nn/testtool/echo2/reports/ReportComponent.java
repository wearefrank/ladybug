/*
   Copyright 2018-2019 Nationale-Nederlanden

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

import org.springframework.util.StringUtils;

import echopointng.tree.DefaultMutableTreeNode;
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
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.TransformationWindow;
import nl.nn.testtool.echo2.run.RunComponent;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.storage.CrudStorage;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportComponent extends MessageComponent {
	private static final long serialVersionUID = 1L;
	private CrudStorage runStorage;
	private Label nameLabel;
	private Label descriptionLabel;
	private Label pathLabel;
	private Label transformationLabel;
	private Label storageIdLabel;
	private Label storageLabel;
	private SelectField stubStrategySelectField;
	private SelectField copyToSelectField;
	private Label estimatedMemoryUsageLabel;
	private TextField nameTextField;
	protected Column descriptionColumn;
	private TextArea descriptionTextArea;
	private TextField pathTextField;
	protected Column transformationColumn;
	private TextArea transformationTextArea;
	private WindowPane deleteWarningWindow;
	private Label deleteIdLabel;
	private Label dynamicVariableLabel;
	private Column dynamicVariableColumn;
	private TextArea dynamicVariableTextArea;
	private Label dynamicVariableErrorMessageLabel;

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

		optionsRow.add(new Label("Download:"));
		optionsRow.add(downloadSelectField);

		add(errorLabel);

		add(okayLabel);

		add(messageColumn);
		add(messageTextArea);
		
		dynamicVariableErrorMessageLabel = Echo2Application.createErrorLabel();
		dynamicVariableErrorMessageLabel.setVisible(false);
		add(dynamicVariableErrorMessageLabel);
		
		buttonRow = Echo2Application.getNewRow();
		add(buttonRow);

		Row nameRow = Echo2Application.getNewRow();
		nameRow.setInsets(new Insets(0, 5, 0, 0));
		add(nameRow);

		nameLabel = Echo2Application.createInfoLabel();
		nameRow.add(nameLabel);

		nameTextField = new TextField();
		nameTextField.setWidth(new Extent(360));
		nameTextField.setVisible(false);
		nameRow.add(nameTextField);

		Row descriptionRow = Echo2Application.getNewRow();
		descriptionRow.setInsets(new Insets(0, 5, 0, 0));
		add(descriptionRow);

		descriptionLabel = Echo2Application.createInfoLabel();
		descriptionLabel.setText("Description:");
		descriptionRow.add(descriptionLabel);

		descriptionColumn = new Column();
		descriptionColumn.setInsets(new Insets(0, 5, 0, 0));
		add(descriptionColumn);
		descriptionTextArea = new TextArea();
		descriptionTextArea.setWidth(new Extent(50, Extent.PERCENT));
		descriptionTextArea.setHeight(new Extent(100));
		descriptionTextArea.setVisible(false);
		add(descriptionTextArea);

		Row pathRow = Echo2Application.getNewRow();
		pathRow.setInsets(new Insets(0, 5, 0, 0));
		add(pathRow);

		pathLabel = Echo2Application.createInfoLabel();
		pathRow.add(pathLabel);

		pathTextField = new TextField();
		pathTextField.setWidth(new Extent(360));
		pathTextField.setVisible(false);
		pathRow.add(pathTextField);

		Row transformationRow = Echo2Application.getNewRow();
		transformationRow.setInsets(new Insets(0, 5, 0, 0));
		add(transformationRow);

		transformationLabel = Echo2Application.createInfoLabel();
		transformationLabel.setText("Transformation:");
		transformationRow.add(transformationLabel);

		transformationColumn = new Column();
		transformationColumn.setInsets(new Insets(0, 5, 0, 0));
		transformationTextArea = new TextArea();
		transformationTextArea.setWidth(new Extent(50, Extent.PERCENT));
		transformationTextArea.setHeight(new Extent(TransformationWindow.TEXT_AREA_HEIGHT));
		transformationTextArea.setVisible(false);
		add(transformationTextArea);
		
		Row dynamicVariableRow = Echo2Application.getNewRow();
		dynamicVariableRow.setInsets(new Insets(0, 5, 0 ,0));
		add(dynamicVariableRow);
		
		dynamicVariableLabel = Echo2Application.createInfoLabel();
		dynamicVariableLabel.setText("Dynamic variables:");
		dynamicVariableLabel.setVisible(false);
		dynamicVariableLabel.setToolTipText("A map of variables to be written in CSV-format with delimiter ';'. "
				+ "These variables can be referred to in this report's input message by referring to their ${key}. "
				+ "Example:\n\n"
				+ "id;firstname;location\n"
				+ "0;waldo;null\n\n"
				+ "In this case, any occurences of ${firstname} in the report's input message "
				+ "will be replaced with \"waldo\" at runtime.");
		dynamicVariableRow.add(dynamicVariableLabel);
		
		dynamicVariableColumn = new Column();
		dynamicVariableColumn.setInsets(new Insets(0, 5, 0 ,0));
//		addLineNumbers(dynamicParamColumn);
		dynamicVariableTextArea = new TextArea();
		dynamicVariableTextArea.setWidth(new Extent(50, Extent.PERCENT));
		dynamicVariableTextArea.setHeight(new Extent(32));
		dynamicVariableTextArea.setVisible(false);
		add(dynamicVariableTextArea);

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
	@Override
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		echo2Application.getContentPane().add(deleteWarningWindow);
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
		storageIdLabel.setText("StorageId: " + report.getStorageId());
		storageLabel.setText("Storage: " + report.getStorage().getName());
		estimatedMemoryUsageLabel.setText("EstimatedMemoryUsage: " + report.getEstimatedMemoryUsage() + " bytes");
		hideMessages();
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (stubStrategySelectField == e.getSource()) {
			report.setStubStrategy((String)stubStrategySelectField.getSelectedItem());
		} else if (e.getActionCommand().equals("Download")) {
			if ("Both".equals(downloadSelectField.getSelectedItem())) {
				displayAndLogError(Download.download(report, true, true));
			} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
				displayAndLogError(Download.download(report, false, true));
			}
		} else if (e.getActionCommand().equals("Copy")) {
			displayAndLogError(Echo2Application.store(runStorage, report));
		} else if (e.getActionCommand().equals("Delete")) {
			if (report.getStorage() instanceof CrudStorage) {
				deleteIdLabel.setText(
						"Are you sure you want to delete report with storage id "
						+ report.getStorageId() + "?");
				deleteWarningWindow.setVisible(true);
			} else {
				displayError("Storage doesn't support delete method");
			}
		} else if (e.getActionCommand().equals("DeleteYes")
				|| e.getActionCommand().equals("DeleteNo")) {
			if (e.getActionCommand().equals("DeleteYes")) {
				displayAndLogError(Echo2Application.delete((CrudStorage)report.getStorage(), report));
			}
			deleteWarningWindow.setVisible(false);
			deleteIdLabel.setText("?");
		}
	}

	protected void toggleShowLineNumbers() {
		super.toggleShowLineNumbers();
		if (!infoPane.edit()) {
			if (infoPane.showLineNumbers()) {
				addLineNumbers(descriptionColumn);
			} else {
				removeLineNumbers(descriptionColumn);
			}
		}
	}

	@Override
	protected void toggleEdit() {
		super.toggleEdit();
		if (infoPane.edit()) {
			// Report xml should not be editable
			messageTextArea.setVisible(false);
		}
	}

	@Override
	protected void save() {
		report.setName(nameTextField.getText());
		report.setDescription(descriptionTextArea.getText());
		saveReportPathChanges();
		saveReportDynamicVariableChanges();
		report.setTransformation(transformationTextArea.getText());
		report.setDynamicVariables(dynamicVariableTextArea.getText());
		report.flushCachedXml();
		if (report.getStorage() instanceof CrudStorage) {
			displayAndLogError(Echo2Application.update((CrudStorage)report.getStorage(), report));
		}
		messageTextArea.setText(report.toXml());
		super.save();
	}

	private void saveReportPathChanges() {
		String input = pathTextField.getText();
		if(!StringUtils.isEmpty(input)) {
			input = RunComponent.normalizePath(input);
			pathTextField.setText(input);
		}
		report.setPath(input);
	}

	private void saveReportDynamicVariableChanges() {
		if(!dynamicVariableTextArea.getText().equals(report.getDynamicVariableCsv())) {
			String errorMessage = report.setDynamicVariables(dynamicVariableTextArea.getText());
			if(errorMessage == null) {
				dynamicVariableErrorMessageLabel.setVisible(false);
			} else {
				dynamicVariableErrorMessageLabel.setText("[Dynamic variables] "+errorMessage);
				dynamicVariableErrorMessageLabel.setVisible(true);
			}
		} else {
			dynamicVariableErrorMessageLabel.setVisible(false);
		}
	}

	@Override
	protected void updateMessageComponents() {
		super.updateMessageComponents();
		updateNameLabelAndNameTextField();
		updateDescriptionLabelAndDescriptionColumnAndTextArea();
		updatePathLabelAndPathTextField();
		updateTransformationLabelAndTransformationColumnAndTextArea();
		updateDynamicVariableLabelAndTextArea();
		if(!infoPane.edit()) {
			dynamicVariableErrorMessageLabel.setVisible(false);
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

	private void updateDescriptionLabelAndDescriptionColumnAndTextArea() {
		if (infoPane.edit()) {
			descriptionColumn.setVisible(false);
			descriptionTextArea.setVisible(true);
		} else {
			descriptionColumn.setVisible(true);
			descriptionTextArea.setVisible(false);
		}
		updateMessageColumn(report.getDescription(), descriptionColumn);
		if (infoPane.showLineNumbers()) {
			addLineNumbers(descriptionColumn);
		}
		descriptionTextArea.setText(report.getDescription());
	}

	private void updatePathLabelAndPathTextField() {
		if (infoPane.edit()) {
			pathLabel.setText("Path: ");
			pathTextField.setText(report.getPath());
			pathTextField.setVisible(true);
		} else {
			String text = "Path: ";
			if (report.getPath() != null) {
				text = text + report.getPath();
			}
			pathLabel.setText(text);
			pathTextField.setVisible(false);
		}
	}

	private void updateTransformationLabelAndTransformationColumnAndTextArea() {
		if (infoPane.edit()) {
			transformationColumn.setVisible(false);
			transformationTextArea.setVisible(true);
		} else {
			transformationColumn.setVisible(true);
			transformationTextArea.setVisible(false);
		}
		updateMessageColumn(report.getTransformation(), transformationColumn);
		if (infoPane.showLineNumbers()) {
			addLineNumbers(transformationColumn);
		}
		transformationTextArea.setText(report.getTransformation());
	}

	private void updateDynamicVariableLabelAndTextArea() {
		if (infoPane.edit()) {
			dynamicVariableColumn.setVisible(false);
			dynamicVariableLabel.setVisible(true);
			dynamicVariableTextArea.setVisible(true);
		} else {
			dynamicVariableColumn.setVisible(true);
			dynamicVariableLabel.setVisible(false);
			dynamicVariableTextArea.setVisible(false);
		}
		updateMessageColumn(report.getDynamicVariableCsv(), dynamicVariableColumn);
		if (infoPane.showLineNumbers()) {
			addLineNumbers(dynamicVariableColumn);
		}
		dynamicVariableTextArea.setText(report.getDynamicVariableCsv());
	}

	@Override
	protected boolean hasChanges() {
		if (super.hasChanges()) {
			return true;
		}
		if (infoPane.edit()) {
			if (hasChanges(report.getName(), nameTextField.getText())) {
				return true;
			}
			if (hasChanges(report.getDescription(), descriptionTextArea.getText())) {
				return true;
			}
			if (hasChanges(report.getPath(), pathTextField.getText())) {
				return true;
			}
			if (hasChanges(report.getTransformation(), transformationTextArea.getText())) {
				return true;
			}
		}
		return false;
	}

}
