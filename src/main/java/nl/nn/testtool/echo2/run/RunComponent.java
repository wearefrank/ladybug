package nl.nn.testtool.echo2.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;

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
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.RunPane;
import nl.nn.testtool.echo2.reports.ReportUploadListener;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.echo2.util.PopupWindow;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

import echopointng.tree.DefaultMutableTreeNode;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RunComponent extends Column implements BeanParent, ActionListener {
	private static final long serialVersionUID = 1L;
	private Logger log = LogUtil.getLogger(this);
	private TestTool testTool;
	private Storage debugStorage; // TODO juiste naam? overal consequent doen?
	private CrudStorage runStorage; // TODO juiste naam? overal consequent doen?
	private Echo2Application echo2Application;
	private TreePane treePane;
	private DefaultMutableTreeNode node;
//	private Report report;
//	private Label nameLabel;
//	private Label pathLabel;
	TextField pathTextField;
	private Label errorLabel;

	private ReportXmlTransformer reportXmlTransformer = null;

	private Map runResult = new HashMap();

	private WindowPane uploadWindow;

	private UploadSelect uploadSelect;
	
	private int numberOfComponentsToSkipForRowManipulation = 0;

	private String lastDisplayedPath;

	private BeanParent beanParent;

	public RunComponent() {
		super();
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setDebugStorage(Storage debugStorage) {
		this.debugStorage = debugStorage;
	}

	public void setRunStorage(CrudStorage runStorage) {
		this.runStorage = runStorage;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
//		super.initBeanPre();
//		
		setInsets(new Insets(10));
		
		// Construct

		errorLabel = Echo2Application.createErrorLabelWithColumnLayoutData();
		errorLabel.setVisible(false);

		// TODO code voor aanmaken upload window en ander zaken gaan delen met ReportsComponent
		Column uploadColumn = new Column();

		uploadWindow = new WindowPane();
		uploadWindow.setVisible(false);
		uploadWindow.setTitle("Upload");
		uploadWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
		uploadWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
		uploadWindow.setWidth(new Extent(350));
		uploadWindow.setHeight(new Extent(110));
		uploadWindow.setInsets(new Insets(10, 0, 10, 0));
		uploadWindow.add(uploadColumn);
		uploadWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		uploadWindow.init();

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

		Button deleteSelectedButton = new Button("Delete");
		deleteSelectedButton.setActionCommand("DeleteSelected");
		deleteSelectedButton.addActionListener(this);
		Echo2Application.decorateButton(deleteSelectedButton);
		buttonRow.add(deleteSelectedButton);

		Button deleteAllButton = new Button("Delete all");
		deleteAllButton.setActionCommand("DeleteAll");
		deleteAllButton.addActionListener(this);
		Echo2Application.decorateButton(deleteAllButton);
		buttonRow.add(deleteAllButton);

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

		Row uploadSelectRow = new Row();

		ReportUploadListener reportUploadListener = new ReportUploadListener();
		reportUploadListener.setRunComponent(this);
		reportUploadListener.setStorage(runStorage);

		uploadSelect = new UploadSelect();
		uploadSelect.setEnabledSendButtonText("Upload");
		uploadSelect.setDisabledSendButtonText("Upload");
		try {
			uploadSelect.addUploadListener(reportUploadListener);
		} catch (TooManyListenersException e) {
			String message = "TooManyListenersException: " + e.getMessage();
			log.error(message, e);
			displayError(message);
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

		add(buttonRow);
		numberOfComponentsToSkipForRowManipulation++;

		add(pathRow);
		numberOfComponentsToSkipForRowManipulation++;

		add(errorLabel);
		numberOfComponentsToSkipForRowManipulation++;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		echo2Application.getContentPane().add(uploadWindow);
		RunPane runPane = (RunPane)beanParent.getBeanParent();
		treePane = runPane.getTreePane();
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	public void display(String path) {
		while (getComponentCount() > numberOfComponentsToSkipForRowManipulation) {
			remove(numberOfComponentsToSkipForRowManipulation);
		}
//		int totalCounter = 0;
//		int directChildCounter = 0;
		List metadata = null;
		if (path.equals("/")) {
			metadata = new ArrayList();
			Iterator iterator = treePane.getReportsWithDirtyPaths().iterator();
			while (iterator.hasNext()) {
				Integer storageId = (Integer)iterator.next();
				// TODO name via metadata ophalen
				try {
					List metadataRecord = new ArrayList();
//					addReport(storageId.toString(), runStorage.getReport(storageId).getName());
					metadataRecord.add(storageId.toString());
					metadataRecord.add("/");
					Report report = runStorage.getReport(storageId);
					metadataRecord.add(report.getName());
					metadataRecord.add(report.getDescription());
					metadata.add(metadataRecord);
//					directChildCounter++;
				} catch (NumberFormatException e) {
					log.error(e);
					displayError(e.getMessage());
				} catch (StorageException e) {
					log.error(e);
					displayError(e.getMessage());
				}
			}
		} else {
			List metadataNames = new ArrayList();
			metadataNames.add("storageId");
			metadataNames.add("path");
			metadataNames.add("name");
			metadataNames.add("description");
			List searchValues = new ArrayList();
			searchValues.add(null);
			searchValues.add(path + "*");
			searchValues.add(null);
			searchValues.add(null);
			try {
				metadata = runStorage.getMetadata(-1, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
			} catch (StorageException e) {
				log.error(e);
				displayError(e.getMessage());
			}
		}
		Collections.sort(metadata, new MetadataNameComparator());
		Iterator metadataIterator = metadata.iterator();
		while (metadataIterator.hasNext()) {
			List metadataRecord = (List)metadataIterator.next();
			String metadataPath = (String)metadataRecord.get(1);
			if (path.equals(metadataPath)) {
				addReport((String)metadataRecord.get(0), (String)metadataRecord.get(2), (String)metadataRecord.get(3));
//				directChildCounter++;
			}
//			totalCounter++;
		}

//		progressBar.setMaximum(directChildCounter);
//		progressBar.setMinimum(0);
//		progressBar.setValue(0);

//		nameLabel.setText(/* "Name FOLDER: " +*/ path /*+ " direct: " + directChildCounter + " total: " + totalCounter*/);
		pathTextField.setText(path);
		
//		pathLabel.setText("Path: " + path);
//		estimatedMemoryUsageLabel.setText("EstimatedMemoryUsage: " + report.getEstimatedMemoryUsage() + " bytes");
//		errorLabel.setVisible(false);

		lastDisplayedPath = path;
	}

	private void addReport(String storageId, String name, String description) {
		Row row = Echo2Application.getNewRow();
		row.setId(storageId);
		row.setInsets(new Insets(0, 5, 0, 0));

		CheckBox checkBox = new CheckBox("");
		checkBox.setSelected(true);
		row.add(checkBox);

		Button button = new Button("Run");
		button.setActionCommand("Run");
		button.addActionListener(this);
		Echo2Application.decorateButton(button);
		row.add(button);

		button = new Button("Open");
		button.setActionCommand("Open");
		button.addActionListener(this);
		Echo2Application.decorateButton(button);
		row.add(button);

		button = new Button("Delete");
		button.setActionCommand("Delete");
		button.addActionListener(this);
		Echo2Application.decorateButton(button);
		row.add(button);

		button = new Button("Replace");
		button.setActionCommand("Replace");
		button.addActionListener(this);
		button.setVisible(false);
		Echo2Application.decorateButton(button);
		row.add(button);

		row.add(new Label(name));

		add(row);

		// TODO runStorage.getMetadata geeft blijkbaar "null" terug, fixen
		if (description != null && !"".equals(description) && !"null".equals(description)) {
//			row = Echo2Application.getNewRow();
//			row.setInsets(new Insets(1));
//			row.add(new Label(description));
			TextArea textArea = new TextArea();
			textArea.setWidth(new Extent(100, Extent.PERCENT));
			textArea.setHeight(new Extent(100));
			textArea.setEnabled(false);
			textArea.setText(description);
//			row.add(textArea);
//			add(row);
			add(textArea);
		}
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		hideErrorMessage();
		String errorMessage = null;
		if (e.getActionCommand().equals("Refresh")) {
			treePane.redisplayReports(lastDisplayedPath);
			runResult.clear();
		} else if (e.getActionCommand().equals("RunSelected")) {
			if (minimalOneSelected()) {
				for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
					Component component = getComponent(i);
					Row row = (Row)component;
					CheckBox checkbox = (CheckBox)row.getComponent(0);
					if (checkbox.isSelected()) {
						run(row);
					}
				}
			}
		} else if (e.getActionCommand().equals("DownloadAll")) {
			errorMessage = Download.download(runStorage);
		} else if (e.getActionCommand().equals("OpenUploadWindow")) {
			uploadWindow.setVisible(true);
		} else if (e.getActionCommand().equals("DeleteAll")) {
			List actionLabels = new ArrayList();
			List actionCommands = new ArrayList();
			List actionListeners = new ArrayList();
			actionLabels.add("Yes, delete all reports");
			actionCommands.add("DeleteAllOk");
			actionListeners.add(this);
			actionLabels.add("No, cancel this action");
			actionCommands.add("DeleteAllCancel");
			actionListeners.add(this);
			PopupWindow popupWindow = new PopupWindow("",
					"Are you sure you want to delete all reports in all folders?", 425, 100,
					actionLabels, actionCommands, actionListeners);
			echo2Application.getContentPane().add(popupWindow);
		} else if (e.getActionCommand().equals("DeleteAllOk")) {
			errorMessage = Echo2Application.deleteAll(runStorage);
			treePane.redisplayReports(null);
		} else if (e.getActionCommand().equals("DeleteSelected")) {
			if (minimalOneSelected()) {
				for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
					Component component = getComponent(i);
					Row row = (Row)component;
					CheckBox checkbox = (CheckBox)row.getComponent(0);
					if (checkbox.isSelected()) {
						Integer storageId = new Integer(row.getId());
						Report report = null;
						try {
							report = runStorage.getReport(storageId);
						} catch (StorageException exception) {
							log.error(exception);
							errorMessage = exception.getMessage();
						}
						if (errorMessage == null) {
							errorMessage = Echo2Application.delete(runStorage, report);
							if (errorMessage == null) {
								remove(row);
								i--;
							}
						}
					}
				}
			}
		} else if (e.getActionCommand().equals("MoveSelected")) {
			if (minimalOneSelected()) {
				String newPath = normalizePath(pathTextField.getText());
				for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
					Component component = getComponent(i);
					Row row = (Row)component;
					CheckBox checkbox = (CheckBox)row.getComponent(0);
					if (checkbox.isSelected()) {
						movePath(row, newPath);
					}
				}
				treePane.redisplayReports(newPath);
			}
		} else if (e.getActionCommand().equals("CopySelected")) {
			if (minimalOneSelected()) {
				String newPath = normalizePath(pathTextField.getText());
				if (newPath.equals(lastDisplayedPath)) {
					List actionLabels = new ArrayList();
					List actionCommands = new ArrayList();
					List actionListeners = new ArrayList();
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
					treePane.redisplayReports(newPath);
				}
			}
		} else if (e.getActionCommand().equals("CopyPathOk")) {
			if (minimalOneSelected()) {
				String newPath = normalizePath(pathTextField.getText());
				copyPath(newPath);
				treePane.redisplayReports(newPath);
			}
		} else if (e.getActionCommand().equals("Run")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			run(row);
		} else if (e.getActionCommand().equals("Open")) {

			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			Integer storageId = new Integer(row.getId());
			
			Report report = null;
			try {
				report = runStorage.getReport(storageId);
			} catch (StorageException exception) {
				log.error(exception);
				exception.printStackTrace();
			}

			// TODO tweede argument wel goed? openReport via pane of echo2 app laten gaan?
//			reportsComponent.openReport(report, false);
			
			
//			Echo2ApplicationEvent echo2ApplicationEvent = new Echo2ApplicationEvent(this);
//			echo2ApplicationEvent.setCommand("OpenReport");
//			echo2ApplicationEvent.setCustomObject(report);
//			applicationContext.publishEvent(echo2ApplicationEvent);

			Report runResultReport = getRunResult((String)runResult.get(storageId));
			report.setReportXmlTransformer(reportXmlTransformer);
			if (runResultReport == null) {
				echo2Application.openReport(report);
			} else {
				runResultReport.setReportXmlTransformer(reportXmlTransformer);
				echo2Application.openReportCompare(report, runResultReport);
			}
			
//			echo2Application.setActiveTabIndex(0);
//			echo2Application.setActiveTabIndex(1);
//			echo2Application.setActiveTabIndex(2);
//			echo2Application.setActiveTabIndex(3);
			
		} else if (e.getActionCommand().equals("Delete")
				|| e.getActionCommand().equals("Replace")) {
			Button button = (Button)e.getSource();
			Row row = (Row)button.getParent();
			Integer storageId = new Integer(row.getId());
 			Report report = null;
			try {
				report = runStorage.getReport(storageId);
			} catch (StorageException exception) {
				log.error(exception);
				errorMessage = exception.getMessage();
			}
			if (errorMessage == null) {
				if (e.getActionCommand().startsWith("Replace")) {
					Report runResultReport = getRunResult((String)runResult.get(storageId));
					runResultReport.setDescription(report.getDescription());
					runResultReport.setPath(report.getPath());
					errorMessage = Echo2Application.store(runStorage, runResultReport);
				}
				if (errorMessage == null) {
					errorMessage = Echo2Application.delete(runStorage, report);
					if (errorMessage == null
							&& e.getActionCommand().startsWith("Delete")) {
						remove(row);
					}
				}
			}
		}
		if (errorMessage != null) {
			displayError(errorMessage);
		}
	}

	void run(Row row) {
		String errorMessage = null;
		Integer storageId = new Integer(row.getId());
		Report report = null;
		try {
			report = runStorage.getReport(storageId);
		} catch (StorageException e) {
			log.error(e);
			displayError(e.getMessage());
		}
		if (report != null) {
			String correlationId = TestTool.getCorrelationId();
			errorMessage = testTool.rerun(correlationId, report, echo2Application);
			if (errorMessage != null) {
				displayError(errorMessage);
			}
			runResult.put(storageId, correlationId);
			Report runResultReport = getRunResult(correlationId);
			Label label = (Label)row.getComponent(5);
			if (runResultReport == null) {
				label.setText("Result report not found. Report generator not enabled?");
				label.setForeground(Echo2Application.getDifferenceFoundTextColor());
			} else {
				String stubInfo = "";
				if (!"Never".equals(report.getStubStrategy())) {
					stubInfo = " (" + report.getStubStrategy() + ")";
				}
				label.setText(report.getName() + " ("
						+ (report.getEndTime() - report.getStartTime())
						+ " >> "
						+ (runResultReport.getEndTime() - runResultReport.getStartTime())
						+ " ms)" + stubInfo);
				report.setReportXmlTransformer(reportXmlTransformer);
				runResultReport.setReportXmlTransformer(reportXmlTransformer);
				if (report.toXml().equals(runResultReport.toXml())) {
					label.setForeground(Echo2Application.getNoDifferenceFoundTextColor());
				} else {
					label.setForeground(Echo2Application.getDifferenceFoundTextColor());
				}
				Button replaceButton = (Button)row.getComponent(4);
				replaceButton.setVisible(true);
			}
		}
	}

	Report getRunResult(String runResultCorrelationId) {
		Report report = null;
		if (runResultCorrelationId != null) {
			List metadataNames = new ArrayList();
			metadataNames.add("storageId");
			metadataNames.add("correlationId");
			List searchValues = new ArrayList();
			searchValues.add(null);
			searchValues.add(runResultCorrelationId);
			List metadata = null;
			try {
				// TODO in Reader.getMetadata kun je ook i < numberOfRecords veranderen in result.size() < numberOfRecords zodat je hier 1 i.p.v. -1 mee kunt geven maar als je dan zoekt op iets dat niet te vinden is gaat hij alle records door. misschien debugStorage.getMetadata een extra paremter geven, numberOfRecordsToConsider en numberOfRecordsToReturn i.p.v. numberOfRecords? (let op: logica ook in mem storage aanpassen)
				metadata = debugStorage.getMetadata(-1, metadataNames, searchValues,
						MetadataExtractor.VALUE_TYPE_OBJECT);
				if (metadata != null && metadata.size() > 0) {
					Integer runResultStorageId = (Integer)((List)metadata.get(0)).get(0);
					report = debugStorage.getReport(runResultStorageId);
				}
			} catch(StorageException e) {
				log.error(e);
				displayError(e.getMessage());
			}
		}
		return report;
	}

	private boolean minimalOneSelected() {
		for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
			Component component = getComponent(i);
			Row row = (Row)component;
			CheckBox checkbox = (CheckBox)row.getComponent(0);
			if (checkbox.isSelected()) {
				return true;
			}
		}
		displayError("No reports selected");
		return false;
	}

	private String normalizePath(String path) {
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		return path;
	}

	private void movePath(Row row, String path) {
		Integer storageId = new Integer(row.getId());
		
		Report report = null;
		try {
			report = runStorage.getReport(storageId);
		} catch (StorageException e) {
			log.error(e);
			displayError(e.getMessage());
		}
		if (report != null) {
			report.setPath(path);
			try {
				runStorage.update(report);
			} catch (StorageException e) {
				log.error(e);
				displayError(e.getMessage());
			}
		}
	}

	private void copyPath(String newPath) {
		for (int i = numberOfComponentsToSkipForRowManipulation; i < getComponentCount(); i++) {
			Component component = getComponent(i);
			Row row = (Row)component;
			CheckBox checkbox = (CheckBox)row.getComponent(0);
			if (checkbox.isSelected()) {
				copyPath(row, newPath);
			}
		}
	}

	private void copyPath(Row row, String newPath) {
		Integer storageId = new Integer(row.getId());
		Report report = null;
		try {
			report = runStorage.getReport(storageId);
		} catch (StorageException e) {
			log.error(e);
			displayError(e.getMessage());
		}
		if (report != null) {
			log.debug("Copy report " + storageId + " from '" + report.getPath() + "' to '" + newPath + "'");
			Report clone;
			try {
				clone = (Report)report.clone();
				clone.setPath(newPath);
				try {
					runStorage.store(clone);
				} catch (StorageException e) {
					log.error(e);
					displayError(e.getMessage());
				}
			} catch (CloneNotSupportedException e) {
				log.error(e);
				displayError(e.getMessage());
			}
		}
	}

	public void displayError(String message) {
		log.error(message);
		if (errorLabel.isVisible()) {
			errorLabel.setText(errorLabel.getText() + " [" + message + "]");
		} else {
			errorLabel.setText("[" + message + "]");
			errorLabel.setVisible(true);
		}
	}

	public void hideErrorMessage() {
		errorLabel.setVisible(false);
	}

	public WindowPane getUploadOptionsWindow() {
		return uploadWindow;
	}

}

class MetadataNameComparator implements Comparator {

	public int compare(Object arg0, Object arg1) {
		List list0 = (List)arg0;
		List list1 = (List)arg1;
		String string0 = (String)list0.get(2);
		String string1 = (String)list1.get(2);
		return string0.compareTo(string1);
	}
	
}
