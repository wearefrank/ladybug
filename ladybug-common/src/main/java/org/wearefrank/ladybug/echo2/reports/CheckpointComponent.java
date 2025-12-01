/*
   Copyright 2020-2025 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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
package org.wearefrank.ladybug.echo2.reports;

import java.io.UnsupportedEncodingException;

import echopointng.tree.DefaultMutableTreeNode;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.RadioButton;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.button.ButtonGroup;
import nextapp.echo2.app.event.ActionEvent;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.CheckpointType;
import org.wearefrank.ladybug.MessageEncoderImpl;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.StubType;
import org.wearefrank.ladybug.echo2.Echo2Application;
import org.wearefrank.ladybug.echo2.util.Download;
import org.wearefrank.ladybug.storage.CrudStorage;

/**
 * @author Jaco de Groot
 */
public class CheckpointComponent extends MessageComponent {
	private static final long serialVersionUID = 1L;
	private Checkpoint checkpoint;
	private Label namePropertyLabel;
	private Label typePropertyLabel;
	private Label threadNamePropertyLabel;
	private Label sourceClassNamePropertyLabel;
	private Label messageClassNamePropertyLabel;
	private Label pathPropertyLabel;
	private Label checkpointUIDPropertyLabel;
	private Label levelLabel;
	private Label encodingPropertyLabel;
	private Label numberOfCharactersPropertyLabel;
	private Label estimatedMemoryUsagePropertyLabel;
	private RadioButton radioButtonStubOptionFollowReportStrategy;
	private RadioButton radioButtonStubOptionYes;
	private RadioButton radioButtonStubOptionNo;
	private Label messageIsStubbedLabel;
	private Label messageStubNotFoundLabel;
	private Label messageIsNullLabel;
	private Label messageIsEmptyStringLabel;
	private Label messageIsTruncatedLabel;
	private Label messageEncodingLabel;
	private Label messageStreamingLabel;
	private Label messageNoCloseReceivedForStream;
	protected Button toggleBase64Button;

	public CheckpointComponent() {
		super();
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	public void initBean() {
		super.initBeanPre();

		//TODO copy en delete hier ook toevoegen (ook copy-to select field) of alle report specifieke dingen (download) hier weghalen?

		toggleBase64Button = new Button("Base64");
		toggleBase64Button.setVisible(false);
		toggleBase64Button.setActionCommand("ToggleBase64");
		toggleBase64Button.addActionListener(this);
		Echo2Application.decorateButton(toggleBase64Button);
		buttonRow.add(toggleBase64Button);

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

		messageIsStubbedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsStubbedLabel.setVisible(false);

		messageIsStubbedLabel.setText("Message is stubbed");
		add(messageIsStubbedLabel);

		messageStubNotFoundLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageStubNotFoundLabel.setVisible(false);
		add(messageStubNotFoundLabel);

		messageIsNullLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsNullLabel.setVisible(false);
		messageIsNullLabel.setText("Message is null");
		add(messageIsNullLabel);

		messageIsEmptyStringLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsEmptyStringLabel.setVisible(false);
		messageIsEmptyStringLabel.setText("Message is an empty string");
		add(messageIsEmptyStringLabel);

		messageIsTruncatedLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageIsTruncatedLabel.setVisible(false);
		add(messageIsTruncatedLabel);

		messageEncodingLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageEncodingLabel.setVisible(false);
		add(messageEncodingLabel);

		messageStreamingLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageStreamingLabel.setVisible(false);
		add(messageStreamingLabel);

		messageNoCloseReceivedForStream = Echo2Application.createInfoLabelWithColumnLayoutData();
		messageNoCloseReceivedForStream.setVisible(false);
		messageNoCloseReceivedForStream.setText("No close received for stream");
		add(messageNoCloseReceivedForStream);

		add(messageColumn);
		add(messageTextArea);

		namePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(namePropertyLabel);

		typePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(typePropertyLabel);

		threadNamePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(threadNamePropertyLabel);

		sourceClassNamePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(sourceClassNamePropertyLabel);

		messageClassNamePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(messageClassNamePropertyLabel);

		pathPropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(pathPropertyLabel);

		checkpointUIDPropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		checkpointUIDPropertyLabel.setToolTipText("A unique identifier consisting of the report's storageId "
				+ "and this checkpoint's index. Use this value as part of a variable in another report's "
				+ "input message to use this checkpoint's message as input. Example: ${checkpoint(287#13)}.\n\n"
				+ "If this message is a valid XML message and you'd like to use a specific part of its data "
				+ "instead, extend your variable to, for example, ${checkpoint(287#13).xpath(results/result[1])}.");
		add(checkpointUIDPropertyLabel);

		levelLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(levelLabel);

		encodingPropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(encodingPropertyLabel);

		numberOfCharactersPropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(numberOfCharactersPropertyLabel);

		estimatedMemoryUsagePropertyLabel = Echo2Application.createInfoLabelWithColumnLayoutData();
		add(estimatedMemoryUsagePropertyLabel);

		super.initBeanPost();
	}

	public void displayCheckpoint(DefaultMutableTreeNode node, Report report, Checkpoint checkpoint,
			Checkpoint checkpointCompare, boolean compare) {
		this.node = node;
		this.report = report;
		this.checkpoint = checkpoint;
		if (checkpoint.getStub() == StubType.FOLLOW_REPORT_STRATEGY.toInt()) {
			radioButtonStubOptionFollowReportStrategy.setSelected(true);
		} else if (checkpoint.getStub() == StubType.NO.toInt()) {
			radioButtonStubOptionNo.setSelected(true);
		} else if (checkpoint.getStub() == StubType.YES.toInt()) {
			radioButtonStubOptionYes.setSelected(true);
		} else {
			radioButtonStubOptionFollowReportStrategy.setSelected(false);
			radioButtonStubOptionYes.setSelected(false);
			radioButtonStubOptionNo.setSelected(false);
		}
		messageIsStubbedLabel.setVisible(checkpoint.isStubbed());
		if (checkpoint.getStubNotFound() != null) {
			messageStubNotFoundLabel.setText(checkpoint.getStubNotFound());
			messageStubNotFoundLabel.setVisible(true);
		} else {
			messageStubNotFoundLabel.setVisible(false);
		}
		String message = null;
		if (checkpoint.getMessage() != null) {
			message = checkpoint.getMessage();
			messageIsNullLabel.setVisible(false);
			messageIsEmptyStringLabel.setVisible(message.equals(""));
		} else {
			messageIsNullLabel.setVisible(true);
			messageIsEmptyStringLabel.setVisible(false);
		}
		if (checkpoint.getPreTruncatedMessageLength() > 0) {
			messageIsTruncatedLabel.setText("Message is truncated ("
					+ (checkpoint.getPreTruncatedMessageLength() - checkpoint.getMessage().length()) + " characters removed)");
			messageIsTruncatedLabel.setVisible(true);
		} else {
			messageIsTruncatedLabel.setVisible(false);
		}
		if (checkpoint.getStreaming() != null) {
			String waiting = "Message is";
			if (checkpoint.isWaitingForStream()) {
				waiting = "Waiting for message to be";
			}
			messageStreamingLabel.setText(waiting  + " captured asynchronously from a "
					+ checkpoint.getStreaming().toLowerCase() + " stream");
			messageStreamingLabel.setVisible(true);
		} else {
			messageStreamingLabel.setVisible(false);
		}
		if (checkpoint.isNoCloseReceivedForStream()) {
			messageNoCloseReceivedForStream.setVisible(true);
		} else {
			messageNoCloseReceivedForStream.setVisible(false);
		}
		String messageCompare = null;
		if (compare) {
			if (checkpointCompare != null) {
				messageCompare = checkpointCompare.getMessage();
			}
		}
		if (checkpoint.getEncoding() != null) {
			messageEncodingLabel.setText("Message of type " + checkpoint.getMessageClassName()
					+ " is encoded to string using " + checkpoint.getEncoding());
			messageEncodingLabel.setVisible(true);
			if (checkpoint.getEncoding().equals(MessageEncoderImpl.BASE64_ENCODER)) {
				if (toggleBase64Button.getText().equals("Base64")) {
					message = decodeBase64AndUTF8(message);
					if (messageCompare != null) {
						messageCompare = decodeBase64AndUTF8(messageCompare);
					}
					messageEncodingLabel.setText(messageEncodingLabel.getText() + " and displayed using Base64 decoded to byte array and byte array decoded to string using UTF-8 (toggle with Base64 button)");
				}
				toggleBase64Button.setVisible(true);
			} else {
				toggleBase64Button.setVisible(false);
			}
		} else {
			messageEncodingLabel.setVisible(false);
			toggleBase64Button.setVisible(false);
		}
		if (compare) {
			setMessage(message, messageCompare);
		} else {
			setMessage(message);
		}
		namePropertyLabel.setText("Name: " + checkpoint.getName());
		typePropertyLabel.setText("Type: " + CheckpointType.toString(checkpoint.getType()));
		threadNamePropertyLabel.setText("Thread name: " + checkpoint.getThreadName());
		sourceClassNamePropertyLabel.setText("Source class name: " + checkpoint.getSourceClassName());
		String messageClassName = checkpoint.getMessageClassName();
		if (messageClassName == null) {
			messageClassName = "java.lang.String";
		}
		messageClassNamePropertyLabel.setText("Message class name: " + messageClassName);
		pathPropertyLabel.setText("Path: " + checkpoint.getPath());
		checkpointUIDPropertyLabel.setText("Checkpoint UID: "+checkpoint.getUid());
		levelLabel.setText("Level: " + checkpoint.getLevel());
		encodingPropertyLabel.setText("Encoding: "+checkpoint.getEncoding());
		numberOfCharactersPropertyLabel.setText("Number of characters: "+(checkpoint.getMessage() != null ? checkpoint.getMessage().length() : "0"));
		estimatedMemoryUsagePropertyLabel.setText("EstimatedMemoryUsage: " + checkpoint.getEstimatedMemoryUsage() + " bytes");
		hideMessages();
	}

	private String decodeBase64AndUTF8(String message) {
		byte[] bytes = java.util.Base64.getDecoder().decode(message);
		try {
			message = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			message = "Could not decode byte array (with length " + bytes.length + ") to string with UTF-8";
		}
		return message;
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (radioButtonStubOptionFollowReportStrategy == e.getSource()) {
			checkpoint.setStub(StubType.FOLLOW_REPORT_STRATEGY.toInt());
		} else if (radioButtonStubOptionNo == e.getSource()) {
			checkpoint.setStub(StubType.NO.toInt());
		} else if (radioButtonStubOptionYes == e.getSource()) {
			checkpoint.setStub(StubType.YES.toInt());
		} else if (e.getActionCommand().equals("ToggleBase64")) {
			if (toggleBase64Button.getText().equals("Base64")) {
				toggleBase64Button.setText("UTF-8");
			} else {
				toggleBase64Button.setText("Base64");
			}
			treePane.selectNode(node);
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
		} else {
			displayError("Storage with name " + report.getStorage().getName() + " is not a CrudStorage");
		}
	}

}
