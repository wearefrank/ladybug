/*
   Copyright 2018-2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import echopointng.tree.DefaultMutableTreeNode;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.RadioButton;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.button.ButtonGroup;
import nextapp.echo2.app.event.ActionEvent;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.storage.CrudStorage;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CheckpointComponent extends MessageComponent {
	private static final long serialVersionUID = 1L;
	private Checkpoint checkpoint;
	private Label nameLabel;
	private Label threadNameLabel;
	private Label sourceClassNameLabel;
	private Label pathLabel;
	private RadioButton radioButtonStubOptionFollowReportStrategy;
	private RadioButton radioButtonStubOptionYes;
	private RadioButton radioButtonStubOptionNo;
	private Label messageHasBeenStubbedLabel;
	private Label messageIsTruncatedLabel;
	private Label messageIsEncodedLabel;
	private Label checkpointUIDLabel;
	private Label numberOfCharactersLabel;
	private Label estimatedMemoryUsageLabel;

	public CheckpointComponent() {
		super();
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}
	
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		super.initBeanPre();

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

		optionsRow.add(new Label("Download:"));
		optionsRow.add(downloadSelectField);

		add(errorLabel);

		add(okayLabel);

		messageHasBeenStubbedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageHasBeenStubbedLabel.setVisible(false);
		messageHasBeenStubbedLabel.setText("Message has been stubbed (copied from original report)");
		add(messageHasBeenStubbedLabel);

		messageIsTruncatedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsTruncatedLabel.setVisible(false);
		add(messageIsTruncatedLabel);

		messageIsEncodedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsEncodedLabel.setVisible(false);
		add(messageIsEncodedLabel);

		add(messageColumn);
		add(messageTextArea);

		nameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(nameLabel);
		
		threadNameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(threadNameLabel);
		
		sourceClassNameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(sourceClassNameLabel);
		
		pathLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(pathLabel);
		
		checkpointUIDLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		checkpointUIDLabel.setToolTipText("A unique identifier consisting of the report's storageId "
				+ "and this checkpoint's index. Use this value as part of a variable in another report's "
				+ "input message to use this checkpoint's message as input. Example: ${checkpoint(287#13)}.\n\n"
				+ "If this message is a valid XML message and you'd like to use a specific part of its data "
				+ "instead, extend your variable to, for example, ${checkpoint(287#13).xpath(results/result[1])}.");
		add(checkpointUIDLabel);

		numberOfCharactersLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(numberOfCharactersLabel);
		
		estimatedMemoryUsageLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(estimatedMemoryUsageLabel);

		super.initBeanPost();
	}

	public void displayCheckpoint(DefaultMutableTreeNode node, Report report, Checkpoint checkpoint,
			Checkpoint checkpointCompare, boolean compare) {
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
		if (checkpoint.getPreTruncatedMessageLength() > 0) {
			messageIsTruncatedLabel.setText("Message is truncated ("
					+ (checkpoint.getPreTruncatedMessageLength() - testTool.getMaxMessageLength()) + " characters removed)");
			messageIsTruncatedLabel.setVisible(true);
		} else {
			messageIsTruncatedLabel.setVisible(false);
		}
		if (checkpoint.getEncoding() > 0) {
			messageIsEncodedLabel.setText("Message object is encoded to string (using " + checkpoint.getEncodingAsString() + ")");
			messageIsEncodedLabel.setVisible(true);
		} else {
			messageIsEncodedLabel.setVisible(false);
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
		pathLabel.setText("Path: " + checkpoint.getPath());
		checkpointUIDLabel.setText("Checkpoint UID: "+checkpoint.getUID());
		numberOfCharactersLabel.setText("Number of characters: "+(checkpoint.getMessage() != null ? checkpoint.getMessage().length() : "0"));
		estimatedMemoryUsageLabel.setText("EstimatedMemoryUsage: " + checkpoint.getEstimatedMemoryUsage() + " bytes");
		hideMessages();
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (radioButtonStubOptionFollowReportStrategy == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_FOLLOW_REPORT_STRATEGY);
		} else if (radioButtonStubOptionNo == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_NO);
		} else if (radioButtonStubOptionYes == e.getSource()) {
			checkpoint.setStub(Checkpoint.STUB_YES);
		} else if (e.getActionCommand().equals("Download")) {
			if ("Both".equals(downloadSelectField.getSelectedItem())) {
				displayAndLogError(Download.download(report, checkpoint));
			} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
				displayAndLogError(Download.download(checkpoint));
			}
		}
	}

	@Override
	protected void save() {
		super.save();
		checkpoint.setMessage(messageTextArea.getText());
		if (report.getStorage() instanceof CrudStorage) {
			displayAndLogError(Echo2Application.update((CrudStorage)report.getStorage(), report));
		}
	}

}
