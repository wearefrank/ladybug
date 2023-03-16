/*
   Copyright 2020-2023 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.testtool.util.CommonPropertiesComparator;
import nl.nn.testtool.util.ScenarioPropertiesComparator;
import nu.studer.java.util.OrderedProperties;
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
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.CsvUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;

import static nl.nn.testtool.util.XmlUtil.*;

/**
 * @author Jaco de Groot
 */
public class TestComponent extends BaseComponent implements BeanParent, ActionListener {
	private static final long serialVersionUID = 1L;
	private TestTool testTool;
	private LogStorage debugStorage;
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
	private final int INDEX_CONVER_TO_TEST_BUTTON = 5;
	private final int INDEX_STORAGEID_LABEL = 6;
	private final int INDEX_ERROR_LABEL = 7;
	private final int INDEX_RESULT_LABEL = 8;
	private final int INDEX_DYNAMIC_VAR_LABEL = 9;
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

	public void setDebugStorage(LogStorage debugStorage) {
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

		Button convertToTestButton = new Button("Convert to test");
		convertToTestButton.setActionCommand("ConvertToTest");
		Echo2Application.decorateButton(convertToTestButton);
		convertToTestButton.addActionListener(this);
		buttonRow.add(convertToTestButton);

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

		Button convertToTestButton = new Button("Convert to test");
		convertToTestButton.setActionCommand("ConvertRowToTest");
		convertToTestButton.addActionListener(this);
		Echo2Application.decorateButton(convertToTestButton);
		row.add(convertToTestButton, INDEX_CONVER_TO_TEST_BUTTON);

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
			treePane.getReportsTreeCellRenderer().setShowReportAndCheckpointIds(showCheckpointIds);
		} else if (e.getActionCommand().equals("RestoreDefaults")) {
			showReportStorageIds = false;
			showReportStorageIdsCheckbox.setSelected(false);
			showCheckpointIds = false;
			showCheckpointIdsCheckbox.setSelected(false);
			treePane.getReportsTreeCellRenderer().setShowReportAndCheckpointIds(showCheckpointIds);
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
		} else if (e.getActionCommand().equals("ConvertRowToTest")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			generateTestScenario(row);
		} else if (e.getActionCommand().equals("ConvertToTest")) {
			if (minimalOneSelected()) {
				for (Row row : getReportRows()) {
					CheckBox checkbox = (CheckBox)row.getComponent(INDEX_CHECKBOX);
					if (checkbox.isSelected()) {
						generateTestScenario(row);
					}
				}
			} else {
				List<String> actionLabels = new ArrayList<String>();
				List<String> actionCommands = new ArrayList<String>();
				List<ActionListener> actionListeners = new ArrayList<ActionListener>();
				actionLabels.add("Yes, convert all reports to test scenarios");
				actionCommands.add("ConvertAllToTestOk");
				actionListeners.add(this);
				actionLabels.add("No, cancel this action");
				actionCommands.add("ConvertAllToTestCancel");
				actionListeners.add(this);
				PopupWindow popupWindow = new PopupWindow("",
						"Are you sure you want to convert all reports to test scenarios?", 375, 130,
						actionLabels, actionCommands, actionListeners);
				echo2Application.getContentPane().add(popupWindow);
			}
		} else if (e.getActionCommand().equals("ConvertAllToTestOk")) {
			for (Row row : getReportRows()) {
				generateTestScenario(row);
			}
		}
		updateProgressBar();
	}

	private void generateTestScenario(Row row) {
		Report report = getReport(row);
		if (report == null) {
			displayError("Couldn't get corresponding report object for row.");
			return;
		}
		if (!report.getName().startsWith("Pipeline ")) {
			displayError("Report [" + report.getName() + "] is not a pipeline report. Test generation isn't implemented for this type of report.");
			return;
		}
		String reportName = report.getName();
		String adapterName = reportName.substring(9);
		//TODO: figure out how to consistently get a path to the testtool folder
		Path testDir = Paths.get("C:/workspace/temp", adapterName);
		try {
			Files.createDirectories(testDir);
		} catch (IOException e) {
			displayAndLogError("Error occurred when creating test directory [" + testDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
			return;
		}
		String scenarioSuffix = "01";
		try {
			OptionalInt maxSuffix = Files.list(testDir).filter(path -> path.getFileName().toString().matches("scenario\\d+.properties")).mapToInt(path -> {
				String fileName = path.getFileName().toString();
				return Integer.parseInt(fileName.substring(8,fileName.indexOf(".")));
			}).max();
			if (maxSuffix.isPresent()) {
				scenarioSuffix = String.format("%0" + 2 + "d", maxSuffix.getAsInt() + 1);
			}
		} catch (IOException e) {
			displayAndLogError("Error occurred counting existing scenarios in test directory [" + testDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
			return;
		}
		Path scenarioDir = testDir.resolve(scenarioSuffix);
		Path scenarioFile = testDir.resolve("scenario" + scenarioSuffix + ".properties");
		Path commonFile = testDir.resolve("common.properties");
		if (!Files.exists(scenarioDir)) {
			try {
				Files.createDirectory(scenarioDir);
			} catch (IOException e) {
				displayAndLogError("Error occurred when creating scenario directory [" + scenarioDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
				return;
			}
		} else {
			try {
				if (Files.list(scenarioDir).findAny().isPresent()) {
					displayError("Error: scenario directory [" + scenarioDir.toAbsolutePath() + "] already exists and is not empty. Not converting report [" + reportName + "]");
					return;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (!Files.exists(commonFile)) {
			try {
				Files.createFile(commonFile);
			} catch (IOException e) {
				displayAndLogError("Error occurred when creating common file [" + commonFile.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
				return;
			}
		}
		new Scenario(report, scenarioDir, scenarioFile, commonFile, adapterName);

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

	private static class Scenario {
		private static final HashSet<String> ignoredSenders = new HashSet(Arrays.asList(
				"nl.nn.adapterframework.jdbc.ResultSet2FileSender",
				"nl.nn.adapterframework.jdbc.DirectQuerySender",
				"nl.nn.adapterframework.jdbc.FixedQuerySender",
				"nl.nn.adapterframework.jdbc.XmlQuerySender",
				"nl.nn.adapterframework.senders.DelaySender",
				"nl.nn.adapterframework.senders.EchoSender",
				"nl.nn.adapterframework.senders.IbisLocalSender",
				"nl.nn.adapterframework.senders.LogSender",
				"nl.nn.adapterframework.senders.ParallelSenders",
				"nl.nn.adapterframework.senders.SenderSeries",
				"nl.nn.adapterframework.senders.SenderWrapper",
				"nl.nn.adapterframework.senders.XsltSender",
				"nl.nn.adapterframework.senders.CommandSender",
				"nl.nn.adapterframework.senders.FixedResultSender",
				"nl.nn.adapterframework.senders.JavascriptSender",
				"nl.nn.adapterframework.jdbc.MessageStoreSender",
				"nl.nn.adapterframework.senders.ReloadSender",
				"nl.nn.adapterframework.compression.ZipWriterSender",
				"nl.nn.adapterframework.senders.LocalFileSystemSender"
		));

		private static final HashSet<String> ignoredSessionKeys = new HashSet(Arrays.asList(
				"cid",
				"id",
				"key",
				"messageId",
				"originalMessage",
				"tcid",
				"tsReceived"
		));

		private File scenarioFile = null, commonFile = null;
		Path resultFolder, scenarioFolder = null;
		private String suffix = "01";
		FileReader scenarioReader, commonReader;
		OrderedProperties scenarioProperties, commonProperties;
		Map<String, String> existingStubs;

		public Scenario(Report report, Path scenarioDir, Path scenario, Path common, String adapterName) {
			scenarioFile = scenario.toFile();
			commonFile = common.toFile();
			FileWriter scenarioWriter, commonWriter;
			OrderedProperties.OrderedPropertiesBuilder commonBuilder = new OrderedProperties.OrderedPropertiesBuilder();
			commonBuilder.withOrdering(new CommonPropertiesComparator());
			commonBuilder.withSuppressDateInComment(true);
			commonProperties = commonBuilder.build();

			OrderedProperties.OrderedPropertiesBuilder scenarioBuilder = new OrderedProperties.OrderedPropertiesBuilder();
			scenarioBuilder.withOrdering(new ScenarioPropertiesComparator());
			scenarioBuilder.withSuppressDateInComment(true);
			scenarioProperties = scenarioBuilder.build();
			scenarioProperties.setProperty("scenario.description", "Test scenario for adapter " + adapterName + ", automatically generated based on a ladybug report");
			scenarioProperties.setProperty("include", "common.properties");
			String adapterProperty = "adapter." + adapterName;
			int paramI = 1;
			int current_step_nr = 0;
			List<Checkpoint> checkpoints = report.getCheckpoints();
			scenarioProperties.setProperty("step" + ++current_step_nr + "." + adapterProperty + ".write", stepPadding(current_step_nr) + "-" + adapterName + "-in.xml");
			createInputOutputFile(scenarioDir, current_step_nr, "adapter", adapterName, true, checkpoints.get(0).getMessage());
			commonProperties.setProperty(adapterProperty + ".className", "nl.nn.adapterframework.senders.IbisJavaSender");
			commonProperties.setProperty(adapterProperty + ".serviceName", "testtool-" + adapterName);
			commonProperties.setProperty(adapterProperty + ".convertExceptionToMessage", "true");

			boolean skipUntilEndOfSender = false;
			String skipUntilEndOfSenderName = "";
			int skipUntilEndOfSenderLevel = -1;

			for (Checkpoint checkpoint : checkpoints) {
				if (skipUntilEndOfSender) {
					//If we're currently stubbing a sender, and we haven't reached the end of it yet
					if (checkpoint.getLevel() == skipUntilEndOfSenderLevel && checkpoint.getType() == Checkpoint.TYPE_ENDPOINT && checkpoint.getName().equals(skipUntilEndOfSenderName)) {
						createInputOutputFile(scenarioDir, current_step_nr, "stub", checkpoint.getName().substring(7), false, checkpoint.getMessage());
						skipUntilEndOfSender = false;
					}
				} else if (checkpoint.getType() < 3 && checkpoint.getName().startsWith("Sender ")) {
					if (!ignoredSenders.contains(checkpoint.getSourceClassName())) {
						//If sender should be stubbed:
						String senderName = checkpoint.getName().substring(7);
						String senderProperty = "stub." + senderName;
						scenarioProperties.setProperty("step" + ++current_step_nr + "." + senderProperty + ".read", stepPadding(current_step_nr) + "-" + senderName + "-in.xml");
						createInputOutputFile(scenarioDir, current_step_nr, "stub", senderName, true, checkpoint.getMessage());
						scenarioProperties.setProperty("step" + ++current_step_nr + "." + senderProperty + ".write", stepPadding(current_step_nr) + "-" + senderName + "-out.xml");

						String serviceName = "testtool-Call" + senderName;
						String existingStubName = existingStubs.get(serviceName.toLowerCase());
						if (!senderProperty.equals(existingStubName)) {
							existingStubs.put(serviceName.toLowerCase(), senderProperty);
							commonProperties.setProperty(senderProperty + ".className", "nl.nn.adapterframework.receivers.JavaListener");
							commonProperties.setProperty(senderProperty + ".serviceName", serviceName);

							if (existingStubName != null) {
								commonProperties.removeProperty(existingStubName + ".className");
								commonProperties.removeProperty(existingStubName + ".serviceName");
								try {
									replaceStubName(resultFolder, existingStubName, senderProperty);
								} catch (IOException e) {
									System.out.println("Error occured when replacing old stub name [" + existingStubName + "] with new stub name [" + senderProperty + "]");
									throw new RuntimeException(e);
								}
							}
						}

						skipUntilEndOfSender = true;
						skipUntilEndOfSenderName = senderName;
						skipUntilEndOfSenderLevel = checkpoint.getLevel();
					}
				} else if (checkpoint.getLevel() == 1 && checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT) {
					//SessionKey for listener found
					String sessionKeyName = checkpoint.getName().substring(11);
					if (!ignoredSessionKeys.contains(sessionKeyName)) {
						scenarioProperties.setProperty(adapterProperty + ".param" + paramI + ".name", sessionKeyName);
						scenarioProperties.setProperty(adapterProperty + ".param" + paramI + ".value", checkpoint.getMessage());
						paramI++;
					}
				}
			}
			scenarioProperties.setProperty("step" + ++current_step_nr + "." + adapterProperty + ".read", stepPadding(current_step_nr) + "-" + adapterName + "-out.xml");
			createInputOutputFile(scenarioDir, current_step_nr, "adapter", adapterName, false, checkpoints.get(checkpoints.size() - 1).getMessage());

			System.out.println("Scenario file: " + scenario.toAbsolutePath());
			System.out.println("Common file: " + common.toAbsolutePath());
			System.out.println("Scenario dir: " + scenarioDir.toAbsolutePath());

			try {
				scenarioWriter = new FileWriter(scenarioFile);
				commonWriter = new FileWriter(commonFile);
				scenarioProperties.store(scenarioWriter, null);
				commonProperties.store(commonWriter, null);
			} catch (IOException e) {
				throw new RuntimeException("Failed to write properties to file", e);
			}
		}

		private static void replaceStubName(Path folder, String oldStubName, String newStubName) throws IOException {
			if (oldStubName.equals(newStubName)) {
				return;
			}
			File[] scenarios = folder.toFile().listFiles((dir, name) -> name.matches("scenario\\d*\\.properties"));
			for (File scenario : scenarios) {
				List<String> result = Files.readAllLines(scenario.toPath());
				boolean changed = false;
				for (int i = 0; i < result.size(); i++) {
					String line = result.get(i);
					if (line.contains(oldStubName + ".read") || line.contains(oldStubName + ".write")) {
						result.set(i, line.replace(oldStubName, newStubName));
						changed = true;
					}
				}
				if (changed) Files.write(scenario.toPath(), result);
			}
		}

		private static void createInputOutputFile(Path folder, int step, String type, String name, boolean startpoint, String message) {
			String filename = String.format("%s-%s-%s-%s.xml", stepPadding(step), type, name, startpoint ? "in" : "out");
			File messageFile = folder.resolve(filename).toFile();
			try {
				if (messageFile.createNewFile()) {
					if (isXml(message)) {
						Files.write(messageFile.toPath(), transform(new InputSource(new StringReader(message)), "/ladybug/testScenarioMessageFormatter.xsl").getBytes(StandardCharsets.UTF_8));
					} else {
						Files.write(messageFile.toPath(), message.getBytes(StandardCharsets.UTF_8));
					}
				}
			} catch (TransformerConfigurationException | ConfigurationException | SAXException | IOException e) {
				throw new RuntimeException("Failed to create file for message: ", e);
			}
		}

		private static String stepPadding(int i) {
			return String.format("%0" + 2 + "d", i);
		}

//		private void processStep(int step, Node checkpoint, String endpoint) {
//			int level = Integer.parseInt(checkpoint.getAttributes().getNamedItem("Level").getTextContent());
//			boolean startpoint = checkpoint.getAttributes().getNamedItem("Type").getTextContent().equals("Startpoint");
//			boolean write = (level & 1) != 1;
//			String message = null;
//			try {
//				message = XmlUtils.nodeContentsToString(checkpoint);
//			} catch (TransformerException e) {
//				throw new RuntimeException(e);
//			}
//			String proxyName = ProxyFactory.getProxy(false, "FF", endpoint).getName();
//			//create folders and scenario file when step = 1 because step 1 contains the adapter name
//			if (step == 1) {
//				inputAdapter = proxyName;
//				resultFolder = resultFolder.resolve(inputAdapter);
//				suffix = nextTestSuffix(resultFolder);
//				String scenarioName = "scenario" + suffix;
//				scenarioFile = resultFolder.resolve(scenarioName + ".properties").toFile();
//				commonFile = resultFolder.resolve("common.properties").toFile();
//				scenarioFolder = resultFolder.resolve(suffix);
//				scenarioFolder.toFile().mkdirs();
//				try {
//					System.out.println(scenarioFile.createNewFile());
//					System.out.println(commonFile.createNewFile());
//				} catch (IOException e) {
//					throw new RuntimeException("Failed to create new test scenario file: ", e);
//				}
//				try {
//					scenarioReader = new FileReader(scenarioFile);
//					commonReader = new FileReader(commonFile);
//					scenario.load(scenarioReader);
//					common.load(commonReader);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				existingStubs = Map.ofEntries(common.entrySet().stream().filter(property -> {
//					String key = property.getKey();
//					return key.startsWith("stub") && key.endsWith("serviceName");
//				}).map(property -> {
//					String key = property.getKey();
//					key = key.substring(0, key.lastIndexOf("."));
//					return Map.entry(property.getValue().toLowerCase(), key);
//				}).toArray(Map.Entry[]::new));
//				scenario.setProperty("scenario.description", "Automatically generated test scenario for " + inputAdapter);
//				scenario.setProperty("include", "common.properties");
//				scenario.setProperty("adapter.GetPartyDetails_1.param1.name", "conversationId");
//				scenario.setProperty("adapter.GetPartyDetails_1.param1.value", "TestId-GetPartyDetails_1");
//				common.setProperty("adapter." + proxyName + ".className", "nl.nn.adapterframework.senders.IbisJavaSender");
//				common.setProperty("adapter." + proxyName + ".serviceName", "testtool-" + proxyName);
//				common.setProperty("adapter." + proxyName + ".convertExceptionToMessage", "true");
//				List<Map.Entry<String, String>> existingIgnoreValuePairs = getExistingIgnoreValuePairs(common);
//				int ignoresCount = existingIgnoreValuePairs.size();
//				for (Map.Entry<String, String> entry : newIgnores) {
//					if (existingIgnoreValuePairs.contains(entry)) continue;
//					ignoresCount++;
//					common.setProperty("ignoreContentBetweenKeys." + ignoresCount + ".key1", entry.getKey());
//					common.setProperty("ignoreContentBetweenKeys." + ignoresCount + ".key2", entry.getValue());
//				}
//			}
//			String type = "adapter";
//			String propertyNamePrefix = "adapter.";
//			if (!proxyName.equals(inputAdapter)) {
//				type = "stub";
//				propertyNamePrefix = "stub.Call";
//				String serviceName = "testtool-Call" + proxyName;
//				String stubName = propertyNamePrefix + proxyName;
//				String existingStubName = existingStubs.get(serviceName.toLowerCase());
//				if (!stubName.equals(existingStubName)) {
//					existingStubs.put(serviceName.toLowerCase(), stubName);
//					common.setProperty(stubName + ".className", "nl.nn.adapterframework.receivers.JavaListener");
//					common.setProperty(stubName + ".serviceName", serviceName);
//
//					if (existingStubName != null) {
//						common.removeProperty(existingStubName + ".className");
//						common.removeProperty(existingStubName + ".serviceName");
//						try {
//							replaceStubName(resultFolder, existingStubName, stubName);
//						} catch (IOException e) {
//							System.out.println("Error occured when replacing old stub name [" + existingStubName + "] with new stub name [" + stubName + "]");
//							throw new RuntimeException(e);
//						}
//					}
//				}
//			}
//			String stepPadded = String.format("%0" + 2 + "d", step);
//			String filename = String.format("%s-%s-%s-%s.xml", stepPadded, type, proxyName, startpoint ? "in" : "out");
//			scenario.setProperty(
//					String.format("step%s.%s%s.%s", step, propertyNamePrefix, proxyName, (write ? "write" : "read")),
//					suffix + "/" + filename);
//			File messageFile = scenarioFolder.resolve(filename).toFile();
//			try {
//				if (messageFile.createNewFile()) {
//					if (XmlUtils.isWellFormed(message)) {
//						Files.write(messageFile.toPath(), XmlUtils.transform(new InputSource(new StringReader(message)), "/testScenarioMessageFormatter.xsl").getBytes(StandardCharsets.UTF_8));
//					} else {
//						Files.write(messageFile.toPath(), message.getBytes(StandardCharsets.UTF_8));
//					}
//				}
//			} catch (TransformerConfigurationException | ConfigurationException | SAXException | IOException e) {
//				throw new RuntimeException("Failed to create file for message: ", e);
//			}
//		}
	}
}

class MetadataComparator implements Comparator<List<Object>> {

	public int compare(List<Object> arg0, List<Object> arg1) {
		String string0 = (String)arg0.get(1) + (String)arg0.get(2);
		String string1 = (String)arg1.get(1) + (String)arg1.get(2);
		return string0.compareTo(string1);
	}
	
}
