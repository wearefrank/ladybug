/*
   Copyright 2018-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TooManyListenersException;

import org.apache.commons.lang.StringUtils;

import echopointng.ProgressBar;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.FillImageBorder;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.UploadSelect;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.BaseComponent;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.TestPane;
import nl.nn.testtool.echo2.reports.MessageComponent;
import nl.nn.testtool.echo2.reports.ReportUploadListener;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.echo2.util.PopupWindow;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.CsvUtil;

/**
 * @author Jaco de Groot
 */
public class TestComponent extends BaseComponent implements BeanParent, ActionListener {
	private static final long serialVersionUID = 1L;
	private TestTool testTool;
	private Storage debugStorage;
	private CrudStorage testStorage;
	private Echo2Application echo2Application;
	private TreePane treePane;
	private ProgressBar progressBar;
	private ReportRunner reportRunner;
	private TextField pathTextField;
	private ReportXmlTransformer reportXmlTransformer = null;
	private WindowPane uploadWindow;
	private WindowPane reportGenerationWindow;
	private WindowPane optionsWindow;
	private UploadSelect uploadSelect;
	private int numberOfComponentsToSkipForRowManipulation = 0;
	private String lastDisplayedPath;
	private BeanParent beanParent;

	private TextArea cloneGenerationTextArea;
	private Label reportGenerationWarningLabel;

	private final int INDEX_CHECKBOX = 0;
	private final int INDEX_RUN_BUTTON = 1;
	private final int INDEX_OPEN_BUTTON = 2;
	private final int INDEX_COMPARE_BUTTON = 3;
	private final int INDEX_REPLACE_BUTTON = 4;
	private final int INDEX_STORAGEID_LABEL = 5;
	private final int INDEX_ERROR_LABEL = 6;
	private final int INDEX_RESULT_LABEL = 7;
	private final int INDEX_DYNAMIC_VAR_LABEL = 8;
	private TextArea cloneGenerationReportInputTextArea;
	private Label cloneGenerationReportInputLabel;

	// options
	private CheckBox showReportStorageIdsCheckbox;
	private CheckBox showCheckpointIdsCheckbox;
	private boolean showReportStorageIds;
	private boolean showCheckpointIds;
	
	public TestComponent() {
		super();
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setDebugStorage(Storage debugStorage) {
		this.debugStorage = debugStorage;
	}

	public void setTestStorage(CrudStorage testStorage) {
		this.testStorage = testStorage;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		super.initBean();

		// Construct

		// TODO code voor aanmaken upload window en ander zaken gaan delen met ReportsComponent
		Column uploadColumn = new Column();
		Column cloneGenerationColumn = new Column();
		Column optionsColumn = new Column();

		uploadWindow = new WindowPane();
		uploadWindow.setVisible(false);
		uploadWindow.setTitle("Upload");
		uploadWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
		uploadWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
		uploadWindow.setWidth(new Extent(480));
		uploadWindow.setHeight(new Extent(360));
		uploadWindow.setInsets(new Insets(10, 0, 10, 0));
		uploadWindow.add(uploadColumn);
		uploadWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		uploadWindow.init();
		
		reportGenerationWindow = new WindowPane();
		reportGenerationWindow.setTitle("Generate report clones");
		reportGenerationWindow.setVisible(false);
		reportGenerationWindow.setWidth(new Extent(464));
		reportGenerationWindow.setHeight(new Extent(610));
		reportGenerationWindow.setInsets(new Insets(5, 5, 5, 5));
		reportGenerationWindow.add(cloneGenerationColumn);
		reportGenerationWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		reportGenerationWindow.init();
		
		optionsWindow = new WindowPane();
		optionsWindow.setTitle("Options");
		optionsWindow.setVisible(false);
		optionsWindow.setWidth(new Extent(280));
		optionsWindow.setHeight(new Extent(120));
		optionsWindow.setInsets(new Insets(5, 5, 5, 5));
		optionsWindow.add(optionsColumn);
		optionsWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		optionsWindow.init();

		Row buttonRow = Echo2Application.getNewRow();

		Button refreshButton = new Button("Refresh");
		refreshButton.setActionCommand("Refresh");
		refreshButton.addActionListener(this);
		Echo2Application.decorateButton(refreshButton);
		buttonRow.add(refreshButton);

		Button runSelectedButton = new Button("Run");
		runSelectedButton.setActionCommand("RunSelected");
		runSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(runSelectedButton);
		buttonRow.add(runSelectedButton);

		Button resetSelectedButton = new Button("Reset");
		resetSelectedButton.setActionCommand("Reset");
		resetSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(resetSelectedButton);
		buttonRow.add(resetSelectedButton);

		Button prepareOptionsButton = new Button("Options...");
		prepareOptionsButton.setActionCommand("OpenOptionsWindow");
		Echo2Application.decorateButton(prepareOptionsButton);
		prepareOptionsButton.addActionListener(this);
		buttonRow.add(prepareOptionsButton);

		Button selectAllButton = new Button("Select all");
		selectAllButton.setActionCommand("SelectAll");
		selectAllButton.addActionListener(this);
		Echo2Application.decorateButton(selectAllButton);
		buttonRow.add(selectAllButton);

		Button deselectAllButton = new Button("Deselect all");
		deselectAllButton.setActionCommand("DeselectAll");
		deselectAllButton.addActionListener(this);
		Echo2Application.decorateButton(deselectAllButton);
		buttonRow.add(deselectAllButton);

		Button moveSelectedButton = new Button("Move");
		moveSelectedButton.setActionCommand("MoveSelected");
		moveSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(moveSelectedButton);
		buttonRow.add(moveSelectedButton);

		Button copySelectedButton = new Button("Copy");
		copySelectedButton.setActionCommand("CopySelected");
		copySelectedButton.addActionListener(this);
		Echo2Application.decorateButton(copySelectedButton);
		buttonRow.add(copySelectedButton);

		Button cloneSelectedButton = new Button("Clone");
		cloneSelectedButton.setToolTipText(
				"Generates clone reports based on a custom CSV-formatted table of parameters and their values. "
				+ "Example:\n\nid;firstname;lastname\n0;jaco;de groot\n1;daniel;meyer\n\nOne clone report is made "
				+ "for every row of values - two in this case. The parameters can be referred to in each report's "
				+ "input message by writing, for example, ${firstname}, which would be parsed to the corresponding "
				+ "value for that parameter at runtime.");
		cloneSelectedButton.setActionCommand("CloneSelected");
		cloneSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(cloneSelectedButton);
		buttonRow.add(cloneSelectedButton);

		Button deleteSelectedButton = new Button("Delete");
		deleteSelectedButton.setActionCommand("DeleteSelected");
		deleteSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(deleteSelectedButton);
		buttonRow.add(deleteSelectedButton);

		Button downloadAllButton = new Button("Download all");
		downloadAllButton.setActionCommand("DownloadAll");
		downloadAllButton.addActionListener(this);
		Echo2Application.decorateButton(downloadAllButton);
		buttonRow.add(downloadAllButton);

		Button prepareUploadButton = new Button("Upload...");
		prepareUploadButton.setActionCommand("OpenUploadWindow");
		Echo2Application.decorateButton(prepareUploadButton);
		prepareUploadButton.addActionListener(this);
		buttonRow.add(prepareUploadButton);

		progressBar = new ProgressBar();
		buttonRow.add(progressBar);
		reportRunner = new ReportRunner();
		reportRunner.setTestTool(testTool);
		reportRunner.setDebugStorage(debugStorage);

		Row uploadSelectRow = new Row();

		ReportUploadListener reportUploadListener = new ReportUploadListener();
		reportUploadListener.setTestComponent(this);
		reportUploadListener.setStorage(testStorage);

		uploadSelect = new UploadSelect();
		uploadSelect.setEnabledSendButtonText("Upload");
		uploadSelect.setDisabledSendButtonText("Upload");
		try {
			uploadSelect.addUploadListener(reportUploadListener);
		} catch (TooManyListenersException e) {
			displayAndLogError(e);
		}

		Row pathRow = Echo2Application.getNewRow();
		pathRow.setInsets(new Insets(0, 5, 0, 5));

		pathRow.add(new Label("Move/Copy to:"));

		pathTextField = new TextField();
		pathTextField.setWidth(new Extent(400));
		pathRow.add(pathTextField);

		// Wire

		uploadSelectRow.add(new Label("Upload"));
		uploadSelectRow.add(uploadSelect);
		uploadColumn.add(uploadSelectRow);

		// Report generation window
		reportGenerationWarningLabel = Echo2Application.createErrorLabel();
		reportGenerationWarningLabel.setVisible(false);
		Row row = Echo2Application.getNewRow();
		row.add(reportGenerationWarningLabel);
		cloneGenerationColumn.add(row);

		Label cloneGenerationCsvLabel = Echo2Application.createInfoLabel();
		cloneGenerationCsvLabel.setText("Variable CSV:");
		row = Echo2Application.getNewRow();
		row.add(cloneGenerationCsvLabel);
		cloneGenerationColumn.add(row);
		
		cloneGenerationTextArea = new TextArea();
		cloneGenerationTextArea.setWidth(new Extent(440));
		cloneGenerationTextArea.setHeight(new Extent(240));
		row = Echo2Application.getNewRow();
		row.add(cloneGenerationTextArea);
		cloneGenerationColumn.add(row);
		
		cloneGenerationReportInputLabel = Echo2Application.createInfoLabel();
		cloneGenerationReportInputLabel.setText("Report input message to clone:");
		row = Echo2Application.getNewRow();
		row.add(cloneGenerationReportInputLabel);
		cloneGenerationColumn.add(row);
		
		cloneGenerationReportInputTextArea = new TextArea();
		cloneGenerationReportInputTextArea.setWidth(new Extent(440));
		cloneGenerationReportInputTextArea.setHeight(new Extent(240));
		row = Echo2Application.getNewRow();
		row.add(cloneGenerationReportInputTextArea);
		cloneGenerationColumn.add(row);
		
		Button generateClonesButton = new Button("Generate");
		generateClonesButton.setActionCommand("GenerateClonesFromCsv");
		generateClonesButton.addActionListener(this);
		Echo2Application.decorateButton(generateClonesButton);
		row = Echo2Application.getNewRow();
		row.add(generateClonesButton);
		cloneGenerationColumn.add(row);
		//
		
		// Options window
		showReportStorageIdsCheckbox = new CheckBox("Show report storage IDs");
		showReportStorageIdsCheckbox.setActionCommand("ToggleReportStorageIds");
		showReportStorageIdsCheckbox.addActionListener(this);
		showReportStorageIdsCheckbox.setSelected(showReportStorageIds);

		showCheckpointIdsCheckbox = new CheckBox("Show checkpoint IDs");
		showCheckpointIdsCheckbox.setActionCommand("ToggleCheckpointIds");
		showCheckpointIdsCheckbox.addActionListener(this);
		showCheckpointIdsCheckbox.setSelected(showCheckpointIds);
		
		Button restoreDefaultsButton = new Button("Restore defaults");
		restoreDefaultsButton.setActionCommand("RestoreDefaults");
		restoreDefaultsButton.addActionListener(this);
		Echo2Application.decorateButton(restoreDefaultsButton);

		row = Echo2Application.getNewRow();
		row.add(showReportStorageIdsCheckbox);
		optionsColumn.add(row);
		row = Echo2Application.getNewRow();
		row.add(showCheckpointIdsCheckbox);
		optionsColumn.add(row);
		row = Echo2Application.getNewRow();
		row.add(restoreDefaultsButton);
		optionsColumn.add(row);
		//

		add(buttonRow);
		numberOfComponentsToSkipForRowManipulation++;

		add(pathRow);
		numberOfComponentsToSkipForRowManipulation++;

		add(errorLabel);
		numberOfComponentsToSkipForRowManipulation++;

		add(okayLabel);
		numberOfComponentsToSkipForRowManipulation++;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		echo2Application.getContentPane().add(uploadWindow);
		echo2Application.getContentPane().add(reportGenerationWindow);
		echo2Application.getContentPane().add(optionsWindow);
		TestPane testPane = (TestPane)beanParent.getBeanParent();
		treePane = testPane.getTreePane();
		reportRunner.setSecurityContext(echo2Application);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	public void display(String path, Set<String> selectedStorageIds) {
		while (getComponentCount() > numberOfComponentsToSkipForRowManipulation) {
			remove(numberOfComponentsToSkipForRowManipulation);
		}
		List<List<Object>> metadata = new ArrayList<List<Object>>();
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("path");
		metadataNames.add("name");
		metadataNames.add("description");
		List<String> searchValues = new ArrayList<String>();
		searchValues.add(null);
		searchValues.add("[" + path + "*]");
		searchValues.add(null);
		searchValues.add(null);
		try {
			metadata = testStorage.getMetadata(-1, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
		} catch (StorageException e) {
			displayAndLogError(e);
		}
		if (path.equals("/")) {
			Iterator<Integer> iterator = treePane.getReportsWithDirtyPaths().iterator();
			while (iterator.hasNext()) {
				Integer storageId = (Integer)iterator.next();
				try {
					for (int i = 0; i < metadata.size(); i++) {
						List<Object> metadataRecord = metadata.get(i);
						if (metadataRecord.get(0).equals(storageId.toString())) {
							metadata.remove(i);
							i--;
						}
					}
					List<Object> metadataRecord = new ArrayList<Object>();
					metadataRecord.add(storageId.toString());
					metadataRecord.add("/");
					Report report = testStorage.getReport(storageId);
					metadataRecord.add(report.getName());
					metadataRecord.add(report.getDescription());
					metadata.add(metadataRecord);
				} catch (NumberFormatException e) {
					displayAndLogError(e);
				} catch (StorageException e) {
					displayAndLogError(e);
				}
			}
		}
		boolean directChildReportsPresent = false;
		Collections.sort(metadata, new MetadataComparator());
		Iterator<List<Object>> metadataIterator;
		// First direct child's (path.equals(metadataPath))
		metadataIterator = metadata.iterator();
		while (metadataIterator.hasNext()) {
			List<Object> metadataRecord = (List<Object>)metadataIterator.next();
			String metadataPath = (String)metadataRecord.get(1);
			if (path.equals(metadataPath)) {
				boolean selected;
				if (selectedStorageIds != null) {
					selected = selectedStorageIds.contains(metadataRecord.get(0));
				} else {
					selected = true;
				}
				displayReport(metadataRecord, selected);
				directChildReportsPresent = true;
			}
		}
		// Then remaining child's (!path.equals(metadataPath))
		metadataIterator = metadata.iterator();
		while (metadataIterator.hasNext()) {
			List<Object> metadataRecord = (List<Object>)metadataIterator.next();
			String metadataPath = (String)metadataRecord.get(1);
			if (!path.equals(metadataPath)) {
				boolean selected;
				if (selectedStorageIds != null) {
					selected = selectedStorageIds.contains(metadataRecord.get(0));
				} else {
					selected = !directChildReportsPresent;
				}
				displayReport(metadataRecord, selected);
			}
		}
		pathTextField.setText(path);
		lastDisplayedPath = path;
		// Update progress bar also when node in tree is selected
		updateProgressBar();
	}

	private void displayReport(List<Object> metadataRecord, boolean selected) {
		String storageId = (String)metadataRecord.get(0);
		String path = (String)metadataRecord.get(1);
		String name = (String)metadataRecord.get(2);
		String description = (String)metadataRecord.get(3);
		displayReport(storageId, path, name, description, selected);
	}

	private void displayReport(String storageId, String path, String name, String description, boolean selected) {
		Row row = Echo2Application.getNewRow();
		row.setId(storageId);
		row.setInsets(new Insets(0, 5, 0, 0));

		CheckBox checkBox = new CheckBox("");
		checkBox.setSelected(selected);
		row.add(checkBox, INDEX_CHECKBOX);

		Button runButton = new Button("Run");
		runButton.setActionCommand("Run");
		runButton.addActionListener(this);
		Echo2Application.decorateButton(runButton);
		row.add(runButton, INDEX_RUN_BUTTON);

		Button openButton = new Button("Open");
		openButton.setActionCommand("Open");
		openButton.addActionListener(this);
		Echo2Application.decorateButton(openButton);
		row.add(openButton, INDEX_OPEN_BUTTON);

		Button button = new Button("Compare");
		button.setActionCommand("Compare");
		button.addActionListener(this);
		button.setVisible(false);
		Echo2Application.decorateButton(button);
		row.add(button, INDEX_COMPARE_BUTTON);

		button = new Button("Replace");
		button.setActionCommand("Replace");
		button.addActionListener(this);
		button.setVisible(false);
		Echo2Application.decorateButton(button);
		row.add(button, INDEX_REPLACE_BUTTON);

		Report report = null;
		try {
			report = testStorage.getReport(Integer.parseInt(storageId));
		} catch (StorageException e) {
			displayAndLogError(e);
		}

		Label storageIdLabel = new Label(String.valueOf(report.getStorageId()));
		storageIdLabel.setForeground(Echo2Application.getButtonBackgroundColor());
		storageIdLabel.setVisible(showReportStorageIds);
		row.add(storageIdLabel, INDEX_STORAGEID_LABEL);

		Label fullPathLabel = new Label(report.getFullPath());
		row.add(fullPathLabel, INDEX_STORAGEID_LABEL);

		Label errorLabel = Echo2Application.createErrorLabel();
		errorLabel.setVisible(false);
		row.add(errorLabel, INDEX_ERROR_LABEL);

		Label resultLabel = new Label();
		RunResult runResult = reportRunner.getResults().get(Integer.parseInt(storageId));
		if (runResult != null) {
			if (runResult.errorMessage != null) {
				errorLabel.setText(runResult.errorMessage);
				errorLabel.setVisible(true);
			} else {
				Report runResultReport = getRunResultReport(runResult.correlationId);
				if (runResultReport == null) {
					errorLabel.setText("Result report not found. Report generator not enabled?");
					errorLabel.setVisible(true);
				} else {
					if(report != null) {
						int stubbed = 0;
						// Don't count first checkpoint which is always used as input (stub property on this checkpoint
						// doesn't influence behavior).
						boolean first = true;
						for (Checkpoint checkpoint : runResultReport.getCheckpoints()) {
							if (first) {
								first = false;
							} else if (checkpoint.isStubbed()) {
								stubbed++;
							}
						}
						int total = runResultReport.getCheckpoints().size() - 1;
						String stubInfo = " (" + stubbed + "/" + total + " stubbed)";
						resultLabel.setText("(" + (report.getEndTime() - report.getStartTime()) + " >> "
								+ (runResultReport.getEndTime() - runResultReport.getStartTime()) + " ms)" + stubInfo);
						report.setGlobalReportXmlTransformer(reportXmlTransformer);
						runResultReport.setGlobalReportXmlTransformer(reportXmlTransformer);
						runResultReport.setTransformation(report.getTransformation());
						runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
						if (report.toXml(reportRunner).equals(runResultReport.toXml(reportRunner))) {
							resultLabel.setForeground(Echo2Application.getNoDifferenceFoundTextColor());
						} else {
							resultLabel.setForeground(Echo2Application.getDifferenceFoundTextColor());
						}
						((Button)row.getComponent(INDEX_COMPARE_BUTTON)).setVisible(true);
						((Button)row.getComponent(INDEX_REPLACE_BUTTON)).setVisible(true);
					}
				}
			}
		}
		row.add(resultLabel, INDEX_RESULT_LABEL);

		Label dynamicVarLabel = newDynamicVariableLabel(report);
		row.add(dynamicVarLabel, INDEX_DYNAMIC_VAR_LABEL);

		add(row);

		// TODO testStorage.getMetadata geeft blijkbaar "null" terug, fixen
		if (description != null && !"".equals(description) && !"null".equals(description)) {
			Column descriptionColumn = new Column();
			descriptionColumn.setInsets(new Insets(0, 5, 0, 0));
			MessageComponent.updateMessageColumn(description, descriptionColumn);
			add(descriptionColumn);
		}
	}

	private Label newDynamicVariableLabel(Report report) {
		Label label = new Label();
		label.setForeground(Echo2Application.getButtonRolloverBackgroundColor());
		if(report.getVariableCsv() != null) {
			String labelText = "[";
			boolean tooManyChars = false;
			for(Entry<String, String> entry : report.getVariablesAsMap().entrySet()) {
				if(labelText.length() + entry.getValue().length() < 50) {
					labelText += entry.getKey()+"="+entry.getValue()+", ";
				} else {
					tooManyChars = true;
					break;
				}
			}
			labelText = labelText.substring(0, labelText.length()-2) + (tooManyChars? "...]" : "]");
			label.setText(labelText);
		} else {
			label.setVisible(false);
		}
		return label;
	}
	
	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		hideMessages();
		if (e.getActionCommand().equals("Refresh")) {
			refresh();
		} else if (e.getActionCommand().equals("Reset")) {
			displayError(reportRunner.reset());
			refresh();
		} else if (e.getActionCommand().equals("SelectAll") || e.getActionCommand().equals("DeselectAll")) {
			for (Row row : getReportRows()) {
				CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
				if (e.getActionCommand().equals("SelectAll")) {
					checkbox.setSelected(true);
				} else {
					checkbox.setSelected(false);
				}
			}
		} else if (e.getActionCommand().equals("RunSelected")) {
			if (minimalOneSelected()) {
				List<Row> rows = new ArrayList<Row>();
				for (Row row : getReportRows()) {
					CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
					if (checkbox.isSelected()) {
						rows.add(row);
					}
				}
				List<Report> reports = new ArrayList<Report>();
				for (Row row : rows) {
					Report report = getReport(row);
					if (report != null) {
						reports.add(report);
					}
				}
				String errorMessage = reportRunner.run(reports, true, false);
				if (errorMessage == null) {
					displayOkay("Report runner started, use Refresh to see results");
				} else {
					displayError(errorMessage);
				}
			}
		} else if (e.getActionCommand().equals("DownloadAll")) {
			displayAndLogError(Download.download(testStorage));
		} else if (e.getActionCommand().equals("OpenUploadWindow")) {
			uploadWindow.setVisible(true);
		} else if (e.getActionCommand().equals("OpenOptionsWindow")) {
			optionsWindow.setVisible(true);
		} else if (e.getActionCommand().equals("ToggleReportStorageIds")) {
			showReportStorageIds = showReportStorageIdsCheckbox.isSelected();
			refresh();
		} else if (e.getActionCommand().equals("ToggleCheckpointIds")) {
			showCheckpointIds = showCheckpointIdsCheckbox.isSelected();
			echo2Application.getReportsTreeCellRenderer().setShowReportAndCheckpointIds(showCheckpointIds);
		} else if (e.getActionCommand().equals("RestoreDefaults")) {
			showReportStorageIds = false;
			showReportStorageIdsCheckbox.setSelected(false);
			showCheckpointIds = false;
			showCheckpointIdsCheckbox.setSelected(false);
			echo2Application.getReportsTreeCellRenderer().setShowReportAndCheckpointIds(showCheckpointIds);
			refresh();
		} else if (e.getActionCommand().equals("DeleteSelected")) {
			if (minimalOneSelected()) {
				List<String> actionLabels = new ArrayList<String>();
				List<String> actionCommands = new ArrayList<String>();
				List<ActionListener> actionListeners = new ArrayList<ActionListener>();
				
				int reportsSelected = getSelectedReportCount();
				String popupMessage;
				String confirmActionLabelText;
				if(reportsSelected > 1) {
					popupMessage = "Are you sure you want to delete the "+reportsSelected+" selected reports?";
					confirmActionLabelText = "Yes, delete "+reportsSelected+" selected reports";
				} else {
					popupMessage = "Are you sure you want to delete the selected report?";
					confirmActionLabelText = "Yes, delete 1 selected report";
				}
				
				actionLabels.add(confirmActionLabelText);
				actionCommands.add("DeleteOk");
				actionListeners.add(this);
				actionLabels.add("No, cancel this action");
				actionCommands.add("DeleteCancel");
				actionListeners.add(this);
				PopupWindow popupWindow = new PopupWindow("", popupMessage, 450, 100,
						actionLabels, actionCommands, actionListeners);
				echo2Application.getContentPane().add(popupWindow);
			}
		} else if (e.getActionCommand().equals("DeleteOk")) {
			for (Row row : getReportRows()) {
				CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
				if (checkbox.isSelected()) {
					Report report = getReport(row);
					if (report != null) {
						String errorMessage = Echo2Application.delete(testStorage, report);
						if (errorMessage == null) {
							remove(row);
							treePane.getReportsWithDirtyPaths().remove(report.getStorageId());
						} else {
							displayAndLogError(errorMessage);
						}
					}
				}
			}
			refresh();
		} else if (e.getActionCommand().equals("MoveSelected")) {
			if (minimalOneSelected()) {
				String newPath = normalizePath(pathTextField.getText());
				for (Row row : getReportRows()) {
					CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
					if (checkbox.isSelected()) {
						movePath(row, newPath);
					}
				}
				treePane.redisplayReports(newPath, null);
			}
		} else if (e.getActionCommand().equals("CopySelected") || e.getActionCommand().equals("CopyPathOk")) {
			if (minimalOneSelected()) {
				String newPath = normalizePath(pathTextField.getText());
				if (newPath.equals(lastDisplayedPath) && !e.getActionCommand().equals("CopyPathOk")) {
					List<String> actionLabels = new ArrayList<String>();
					List<String> actionCommands = new ArrayList<String>();
					List<ActionListener> actionListeners = new ArrayList<ActionListener>();
					actionLabels.add("Yes, duplicate reports");
					actionCommands.add("CopyPathOk");
					actionListeners.add(this);
					actionLabels.add("No, cancel this action");
					actionCommands.add("CopyPathCancel");
					actionListeners.add(this);
					PopupWindow popupWindow = new PopupWindow("",
							"Are you sure you want to copy to the same folder?", 375, 100,
							actionLabels, actionCommands, actionListeners);
					echo2Application.getContentPane().add(popupWindow);
				} else {
					copyPath(newPath);
					treePane.redisplayReports(newPath, null);
				}
			}
		} else if (e.getActionCommand().equals("CloneSelected")) {
			if(getSelectedReportCount() == 1) {
				reportGenerationWindow.setVisible(true);
				for(Row r : getReportRows()) {
					if(((CheckBox)r.getComponent(INDEX_CHECKBOX)).isSelected()) {
						cloneGenerationReportInputTextArea.setText(getReport(r).getCheckpoints().get(0).getMessage());
					}
				}
			} else if(getSelectedReportCount() > 1) {
				displayError("Please clone reports one at a time");
			} else {
				displayError("No report selected");
			}
		} else if (e.getActionCommand().equals("GenerateClonesFromCsv")) {
			if (getSelectedReportCount() == 1) {
				String errorMessage = CsvUtil.validateCsv(cloneGenerationTextArea.getText(), ";");
				if(errorMessage == null) {
					Report reportToClone = null;
					for (Row r : getReportRows()) {
						if (((CheckBox)r.getComponent(INDEX_CHECKBOX)).isSelected()) {
							reportToClone = getReport(r);
						}
					}
					if(StringUtils.isNotEmpty(cloneGenerationReportInputTextArea.getText())) {
						reportToClone.getInputCheckpoint().setMessage(cloneGenerationReportInputTextArea.getText());
					}
					if(!reportToClone.getInputCheckpoint().containsVariables()
							&& (reportGenerationWarningLabel.getText() == null
							|| !reportGenerationWarningLabel.getText().endsWith("press again to confirm"))) {
						reportGenerationWarningLabel.setText("No variables found in input message; press again to confirm");
						reportGenerationWarningLabel.setVisible(true);
					} else {
						generateReportClonesFromCsv(reportToClone);
						reportGenerationWindow.setVisible(false);
						reportGenerationWarningLabel.setText(null);
						reportGenerationWarningLabel.setVisible(false);
						refresh();
					}
				} else {
					reportGenerationWarningLabel.setText(errorMessage);
					reportGenerationWarningLabel.setVisible(true);
				}
			} else if(getSelectedReportCount() > 1) {
				displayError("Please clone reports one at a time");
			} else {
				displayError("No report selected");
			}
		} else if (e.getActionCommand().equals("Run")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			Report report = getReport(row);
			if (report != null) {
				List<Report> reports = new ArrayList<Report>();
				reports.add(report);
				displayError(reportRunner.run(reports, false, true));
				refresh();
			}
		} else if (e.getActionCommand().equals("Open")
				|| e.getActionCommand().equals("Compare")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			Report report = getReport(row);
			report.setGlobalReportXmlTransformer(reportXmlTransformer);
			Integer storageId = new Integer(row.getId());
			RunResult runResult = reportRunner.getResults().get(storageId);
			if (e.getActionCommand().equals("Open")) {
				echo2Application.openReport(report, ReportsComponent.OPEN_REPORT_ALLOWED);
			} else {
				Report runResultReport = getRunResultReport(runResult.correlationId);
				if (runResultReport != null) {
					runResultReport.setGlobalReportXmlTransformer(reportXmlTransformer);
					runResultReport.setTransformation(report.getTransformation());
					runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
					echo2Application.openReportCompare(report, runResultReport, reportRunner);
				}
			}
		} else if (e.getActionCommand().equals("Delete")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
 			Report report = getReport(row);
			if (report != null) {
				String errorMessage = Echo2Application.delete(testStorage, report);
				if (errorMessage == null) {
					remove(row);
				} else {
					displayAndLogError(errorMessage);
				}
			}
		} else if (e.getActionCommand().equals("Replace")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			Report report = getReport(row);
			if (report != null) {
				Integer storageId = new Integer(row.getId());
				boolean isSelected = ((CheckBox)row.getComponent(INDEX_CHECKBOX)).isSelected();
				Report runResultReport = getRunResultReport(reportRunner.getResults().get(storageId).correlationId);
				runResultReport.setTestTool(report.getTestTool());
				runResultReport.setName(report.getName());
				runResultReport.setDescription(report.getDescription());
				if(report.getCheckpoints().get(0).containsVariables()) {
					runResultReport.getCheckpoints().get(0).setMessage(report.getCheckpoints().get(0).getMessage());
				}
				runResultReport.setPath(report.getPath());
				runResultReport.setTransformation(report.getTransformation());
				runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
				runResultReport.setVariableCsvWithoutException(report.getVariableCsv());
				runResultReport.setStorageId(report.getStorageId());
				String errorMessage = Echo2Application.update(testStorage, runResultReport);
				if (errorMessage == null) {
					reportRunner.getResults().remove(storageId);
					if (treePane.getReportsWithDirtyPaths().remove(report.getStorageId())) {
						treePane.getReportsWithDirtyPaths().add(runResultReport.getStorageId());
					}
					refresh();
					for (Row reportRow : getReportRows()) {
						CheckBox checkbox = (CheckBox)reportRow.getComponent(INDEX_CHECKBOX);
						if (new Integer(reportRow.getId()).equals(runResultReport.getStorageId())) {
							checkbox.setSelected(isSelected);
						}
					}
				} else {
					displayAndLogError(errorMessage);
				}
			}
		}
		updateProgressBar();
	}

	private void generateReportClonesFromCsv(Report reportToClone) {
		Scanner scanner = new Scanner(cloneGenerationTextArea.getText());
		List<String> lines = new ArrayList<String>();
		while(scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if(StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#")) {
				lines.add(nextLine);
			}
		}
		scanner.close();
		
		try {
			reportToClone.setVariableCsvWithoutException(lines.get(0)+"\n"+lines.get(1));
			displayAndLogError(Echo2Application.update(testStorage, reportToClone));
			if(lines.size() > 2) {
				for(int i = 2; i < lines.size(); i++) {
					Report cloneReport = reportToClone.clone();
					cloneReport.setVariableCsvWithoutException(lines.get(0)+"\n"+lines.get(i));
					displayAndLogError(Echo2Application.store(testStorage, cloneReport));
				}
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	private int getSelectedReportCount() {
		int count = 0;
		for(Row row : getReportRows()) {
			if(((CheckBox)row.getComponent(INDEX_CHECKBOX)).isSelected()) {
				count++;
			}
		}
		return count;
	}

	private void updateProgressBar() {
		progressBar.setMaximum(reportRunner.getMaximum());
		progressBar.setValue(reportRunner.getProgressValue());
		progressBar.setToolTipText(reportRunner.getProgressValue() + " / " + reportRunner.getMaximum());
	}

	public void refresh() {
		treePane.redisplayReports(lastDisplayedPath, getSelectedStorageIds());
	}

	private List<Row> getReportRows() {
		List<Row> result = new ArrayList<Row>();
		for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
			Component component = getComponent(i);
			// Ignore TextArea's for Reports with a description
			if (component instanceof Row) {
				result.add((Row)component);
			}
		}
		return result;
	}

	private Set<String> getSelectedStorageIds() {
		Set<String> selectedStorageIds = new HashSet<String>();
		for (Row row : getReportRows()) {
			CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
			if (checkbox.isSelected()) {
				selectedStorageIds.add(row.getId());
			}
		}
		return selectedStorageIds;
	}

	private boolean minimalOneSelected() {
		if (getSelectedStorageIds().size() > 0) {
			return true;
		} else {
			displayError("No reports selected");
			return false;
		}
	}

	private Report getReport(Row row) {
		Integer storageId = new Integer(row.getId());
		return echo2Application.getReport(testStorage, storageId, this);
	}

	private Report getRunResultReport(String runResultCorrelationId) {
		Report report = null;
		try {
			report = reportRunner.getRunResultReport(runResultCorrelationId);
		} catch(StorageException storageException) {
			displayAndLogError(storageException);
		}
		return report;
	}

	public static String normalizePath(String path) {
		for (int i = 0; i < path.length(); i++) {
			// Be on the safe side for now
			if (!Character.isLetterOrDigit(path.charAt(i)) && "/ -_.()".indexOf(path.charAt(i)) == -1) {
				if (path.length() > i + 1) {
					path = path.substring(0, i) + path.substring(i + 1);
					i--;
				} else {
					path = path.substring(0, i);
				}
			}
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		while (path.indexOf("//") != -1) {
			path = path.substring(0, path.indexOf("//")) + path.substring(path.indexOf("//") +1);
		}
		return path;
	}

	private void movePath(Row row, String path) {
		Report report = getReport(row);
		if (report != null) {
			report.setPath(path);
			try {
				testStorage.update(report);
			} catch (StorageException e) {
				displayAndLogError(e);
			}
		}
	}

	private void copyPath(String newPath) {
		for (Row row : getReportRows()) {
			CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
			if (checkbox.isSelected()) {
				copyPath(row, newPath);
			}
		}
	}

	private void copyPath(Row row, String newPath) {
		Report report = getReport(row);
		if (report != null) {
			Integer storageId = new Integer(row.getId());
			log.debug("Copy report " + storageId + " from '" + report.getPath() + "' to '" + newPath + "'");
			Report clone;
			try {
				clone = report.clone();
				clone.setPath(newPath);
				try {
					testStorage.store(clone);
				} catch (StorageException e) {
					displayAndLogError(e);
				}
			} catch (CloneNotSupportedException e) {
				displayAndLogError(e);
			}
		}
	}

	public WindowPane getUploadOptionsWindow() {
		return uploadWindow;
	}

}

class MetadataComparator implements Comparator<List<Object>> {

	public int compare(List<Object> arg0, List<Object> arg1) {
		String string0 = (String)arg0.get(1) + (String)arg0.get(2);
		String string1 = (String)arg1.get(1) + (String)arg1.get(2);
		return string0.compareTo(string1);
	}
	
}
