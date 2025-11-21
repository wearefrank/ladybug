/*
   Copyright 2020, 2022-2025 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import echopointng.tree.DefaultMutableTreeNode;
import lombok.Setter;
import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.echo2.BaseComponent;
import org.wearefrank.ladybug.echo2.BeanParent;
import org.wearefrank.ladybug.echo2.Echo2Application;
import org.wearefrank.ladybug.echo2.ReportPane;
import org.wearefrank.ladybug.echo2.util.Download;
import org.wearefrank.ladybug.echo2.util.PopupWindow;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.storage.LogStorage;
import org.wearefrank.ladybug.storage.StorageException;

/**
 * @author Jaco de Groot
 */
public class MessageComponent extends BaseComponent implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final char REPLACE_NON_XML_CHAR = 0x00BF; // Inverted question mark.
	protected @Setter TestTool testTool;
	protected @Setter LogStorage debugStorage;
	protected @Setter TreePane treePane;
	protected @Setter InfoPane infoPane;
	private BeanParent beanParent;
	protected Echo2Application echo2Application;
	protected DefaultMutableTreeNode node;
	protected Report report;
	protected Row buttonRow;
	protected Button editButton;
	protected Button saveButton;
	protected SelectField downloadSelectField;
	private String message;
	private String messageCompare;
	private boolean compare;
	protected Column messageColumn;
	protected Button lineNumbersButton;
	protected TextArea messageTextArea;

	/**
	 * @see Echo2Application#initBean()
	 */
	protected void initBeanPre() {
		super.initBean();

		buttonRow = Echo2Application.getNewRow();
		add(buttonRow);

		Button rerunButton = new Button("Rerun");
		rerunButton.setActionCommand("Rerun");
		rerunButton.addActionListener(this);
		Echo2Application.decorateButton(rerunButton);
		buttonRow.add(rerunButton);

		editButton = new Button();
		editButton.setActionCommand("ToggleEdit");
		editButton.addActionListener(this);
		Echo2Application.decorateButton(editButton);
		buttonRow.add(editButton);

		lineNumbersButton = new Button();
		lineNumbersButton.setActionCommand("ToggleShowLineNumbers");
		lineNumbersButton.addActionListener(this);
		Echo2Application.decorateButton(lineNumbersButton);
		buttonRow.add(lineNumbersButton);

		saveButton = new Button("Save");
		saveButton.setActionCommand("Save");
		saveButton.addActionListener(this);
		Echo2Application.decorateButton(saveButton);
		buttonRow.add(saveButton);

		downloadSelectField = new SelectField(new String[]{"Both", "Report", "Message"});
		downloadSelectField.setSelectedIndex(0);

		messageColumn = new Column();
		messageColumn.setInsets(new Insets(0, 5, 0, 0));
		messageTextArea = new TextArea();
		messageTextArea.setWidth(new Extent(100, Extent.PERCENT));
		messageTextArea.setHeight(new Extent(300));
		messageTextArea.setVisible(false);
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	protected void initBeanPost() {
		updateLineNumbersButton();
		updateEditButton();
		updateSaveButton();
	}

	/**
	 * @see Echo2Application#initBean()
	 * @param beanParent ...
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	protected void setMessage(String message) {
		this.message = message;
		this.messageCompare = null;
		compare = false;
		updateMessageComponents();
	}

	protected void setMessage(String message, String messageCompare) {
		this.message = message;
		this.messageCompare = messageCompare;
		compare = true;
		updateMessageComponents();
	}

	protected String getMessage() {
		return message;
	}
	
	protected void toggleShowLineNumbers() {
		if (!infoPane.edit()) {
			if (infoPane.showLineNumbers()) {
				infoPane.showLineNumbers(false);
				removeLineNumbers(messageColumn);
			} else {
				infoPane.showLineNumbers(true);
				addLineNumbers(messageColumn);
			}
			updateLineNumbersButton();
		}
	}

	protected void toggleEdit() {
		if (infoPane.edit()) {
			infoPane.edit(false);
		} else {
			infoPane.edit(true);
		}
		updateMessageComponents();
	}

	protected void save() {
		message = messageTextArea.getText();
	}

	protected void updateMessageComponents() {
		updateLineNumbersButton();
		updateEditButton();
		updateSaveButton();
		if (infoPane.edit()) {
			messageColumn.setVisible(false);
			messageTextArea.setVisible(true);
		} else {
			messageColumn.setVisible(true);
			messageTextArea.setVisible(false);
		}
		updateMessageColumn(message, messageColumn, compare, messageCompare);
		if (infoPane.showLineNumbers()) {
			addLineNumbers(messageColumn);
		}
		if (message == null) {
			messageTextArea.setText(message);
		} else {
			messageTextArea.setText(replaceNonValidXmlCharacters(message, null, false));
		}
	}

	public static void updateMessageColumn(String message, Column messageColumn) {
		updateMessageColumn(message, messageColumn, false, null);
	}

	public static void updateMessageColumn(String message, Column messageColumn, boolean compare,
			String messageCompare) {
		messageColumn.removeAll();
		if (message == null || message.equals("")) {
			messageColumn.setVisible(false);
		} else {
			LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(message));
			String line = readLineIgnoringExceptions(lineNumberReader);
			LineNumberReader lineNumberReaderCompare = null;
			String lineCompare = null;
			if (messageCompare != null) {
				lineNumberReaderCompare = new LineNumberReader(new StringReader(messageCompare));
				lineCompare = readLineIgnoringExceptions(lineNumberReaderCompare);
			}
			while (line != null) {
				addLine(line, messageColumn, compare, lineCompare);
				line = readLineIgnoringExceptions(lineNumberReader);
				if (messageCompare != null) {
					lineCompare = readLineIgnoringExceptions(lineNumberReaderCompare);
				}
			}
		}
	}

	private static String readLineIgnoringExceptions(LineNumberReader lineNumberReader) {
		String line = null;
		try {
			line = lineNumberReader.readLine();
		} catch (IOException e) {
		}
		return line;
	}

	private static void addLine(String line, Column messageColumn, boolean compare, String lineCompare) {
		Row row = new Row();
		messageColumn.add(row);
		boolean differenceFound = compare && !line.equals(lineCompare);
		replaceNonValidXmlCharacters(line, row, differenceFound);
	}

	private static Label createLabel(String text, boolean applyIndent, boolean differenceFound) {
		RowLayoutData rowLayouData = new RowLayoutData();
		rowLayouData.setAlignment(Alignment.ALIGN_TOP);
		Label label = new Label();
		label.setLayoutData(rowLayouData);
		label.setFont(Echo2Application.getMessageFont());
		if (differenceFound) {
			label.setForeground(Echo2Application.getDifferenceFoundTextColor());
		}
		if (applyIndent) {
			setIndentAndText(label, text);
		} else {
			label.setText(text);
		}
		return label;
	}

	private static void setIndentAndText(Label label, String line) {
		int preSpace = 0;
		int preIndex = 0;
		while (line.length() > preIndex && (line.charAt(preIndex) == ' ' || line.charAt(preIndex) == '\t')) {
			if (line.charAt(preIndex) == ' ') {
				preSpace = preSpace + 1;
			} else {
				preSpace = preSpace + 4;
			}
			preIndex++;
		}
		boolean whiteSpaceOnly = preIndex == line.length();
		int postSpace = 0;
		if (!whiteSpaceOnly) {
			int postIndex = line.length() - 1;
			while (postIndex >= 0 && (line.charAt(postIndex) == ' ' || line.charAt(postIndex) == '\t')) {
				if (line.charAt(postIndex) == ' ') {
					postSpace = postSpace + 1;
				} else {
					postSpace = postSpace + 4;
				}
				postIndex--;
			}
		}
		setIndent(label, preSpace, postSpace, whiteSpaceOnly);
		label.setText(line.substring(preIndex));
	}

	private static void setIndent(Label label, int preNumberOfChars, int postNumberOfChars, boolean whiteSpaceOnly) {
		RowLayoutData rowLayoutData = (RowLayoutData)label.getLayoutData();
		int topPx = 0;
		if (whiteSpaceOnly) {
			topPx = 15;
		}
		rowLayoutData.setInsets(new Insets(preNumberOfChars * 7, topPx, postNumberOfChars * 7, 0));
	}

	private void updateLineNumbersButton() {
		if (infoPane.showLineNumbers()) {
			lineNumbersButton.setText("Hide line numbers");
		} else {
			lineNumbersButton.setText("Show line numbers");
		}
		if (infoPane.edit()) {
			lineNumbersButton.setVisible(false);
		} else {
			lineNumbersButton.setVisible(true);
		}
	}

	private void updateEditButton() {
		if (infoPane.edit()) {
			editButton.setText("Read-only");
		} else {
			editButton.setText("Edit");
		}
	}

	private void updateSaveButton() {
		if (saveButton != null) {
			if (infoPane.edit()) {
				saveButton.setVisible(true);
			} else {
				saveButton.setVisible(false);
			}
		}
	}
	
	public static void addLineNumbers(Column messageColumn) {
		int maxNumberLength = ("" + messageColumn.getComponentCount()).length();
		for (int i = 0; i < messageColumn.getComponentCount(); i++) {
			Row row = (Row)messageColumn.getComponent(i);
			int lineNumber = i + 1;
			Label label = createLabel(lineNumber + ":", false, false);
			label.setForeground(Echo2Application.getLineNumberTextColor());
			setIndent(label, maxNumberLength - ("" + lineNumber).length(), 0, false);
			row.add(label, 0);
		}
	}

	public static void removeLineNumbers(Column messageColumn) {
		for (int i = 0; i < messageColumn.getComponentCount(); i++) {
			Row row = (Row)messageColumn.getComponent(i);
			row.remove(0);
		}
	}

	private static String replaceNonValidXmlCharacters(String string, Row row, boolean differenceFound) {
		StringBuilder builder = new StringBuilder();
		int c;
		for (int i = 0; i < string.length(); i += Character.charCount(c)) {
			c = string.codePointAt(i);
			if (isPrintableUnicodeChar(c)) {
				builder.appendCodePoint(c);
			} else {
				String substitute = REPLACE_NON_XML_CHAR + "#" + c + ";";
				if (row == null) {
					builder.append(substitute);
				} else {
					row.add(createLabel(builder.toString(), true, differenceFound));
					Label label = createLabel(substitute, true, differenceFound);
					label.setBackground(Echo2Application.getButtonRolloverBackgroundColor());
					row.add(label);
					builder = new StringBuilder();
				}
			}
		}
		if (row != null) {
			row.add(createLabel(builder.toString(), true, differenceFound));
		}
		return builder.toString();
	}

	// Copied from IAF XmlUtils
	public static boolean isPrintableUnicodeChar(int c) {
		return (c == 0x0009)
			|| (c == 0x000A)
			|| (c == 0x000D)
			|| (c >= 0x0020 && c <= 0xD7FF)
			|| (c >= 0xE000 && c <= 0xFFFD)
			/* Prevent application crash 
			|| (c >= 0x0010000 && c <= 0x0010FFFF)*/
			// But allow (some) emoticons (https://en.wikipedia.org/wiki/Emoticons_(Unicode_block))
			|| (c >= 0x001F600 && c <= 0x0001F64F);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (e.getActionCommand() == null) {
			// Prevent NPE when super handles an event based on e.getSource()
		} else if (e.getActionCommand().equals("ExpandAll")) {
			treePane.expandAll(node);
		} else if (e.getActionCommand().equals("CollapseAll")) {
			treePane.collapseAll(node);
		} else if (e.getActionCommand().equals("Close") || e.getActionCommand().equals("CloseOk")) {
			if (overwriteChanges(e.getActionCommand(), "CloseOk", "CloseCancel")) {
				if (getParent().getParent().getParent() instanceof ReportPane) {
					((Echo2Application)getApplicationInstance()).closeReport();
				} else {
					treePane.closeReport(report);
				}
			}
		} else if (e.getActionCommand().equals("Save")) {
			save();
		} else if (e.getActionCommand().equals("Download")) {
			if ("Both".equals(downloadSelectField.getSelectedItem())) {
				// Override in ReportComponent and CheckpointComponent
			} else if ("Report".equals(downloadSelectField.getSelectedItem())) {
				displayAndLogError(Download.download(report));
			} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
				// Override in ReportComponent and CheckpointComponent
			} else {
				displayError("No download type selected");
			}
		} else if (e.getActionCommand().equals("ToggleShowLineNumbers")) {
			toggleShowLineNumbers();
		} else if (e.getActionCommand().equals("ToggleEdit") || e.getActionCommand().equals("ToggleEditOk")) {
			if (overwriteChanges(e.getActionCommand(), "ToggleEditOk", "ToggleEditCancel")) {
				toggleEdit();
			}
		} else if (e.getActionCommand().equals("Rerun")) {
			String correlationId = TestTool.getCorrelationId();
			String errorMessage = testTool.rerun(correlationId, report, echo2Application);
			if (errorMessage == null) {
				String message = "Rerun succeeded";
				Report runResultReport = null;
				try {
					runResultReport = ReportRunner.getRunResultReport(debugStorage, correlationId);
				} catch(StorageException storageException) {
					displayAndLogError(storageException);
				}
				if (runResultReport != null) {
					message = message + " " + ReportRunner.getRunResultInfo(report, runResultReport);
				} else {
					message = message + " (no run result info available)";
				}
				displayOkay(message);
			} else {
				displayAndLogError(errorMessage);
			}
		}
	}

	protected boolean overwriteChanges(String currentActionCommand, String actionCommandOk, String actionCommandCancel) {
		if (currentActionCommand.equals(actionCommandOk)) {
			return true;
		} else if (hasChanges()) {
			List<String> actionLabels = new ArrayList<String>();
			List<String> actionCommands = new ArrayList<String>();
			List<ActionListener> actionListeners = new ArrayList<ActionListener>();
			actionLabels.add("Yes, discard changes");
			actionCommands.add(actionCommandOk);
			actionListeners.add(this);
			actionLabels.add("No, cancel this action");
			actionCommands.add(actionCommandCancel);
			actionListeners.add(this);
			PopupWindow popupWindow = new PopupWindow("",
					"Are you sure you want to continue and discard your changes?", 450, 100,
					actionLabels, actionCommands, actionListeners);
			echo2Application.getContentPane().add(popupWindow);
			return false;
		} else {
			return true;
		}
	}

	protected boolean hasChanges() {
		if (infoPane.edit()) {
			if (hasChanges(message, messageTextArea.getText())) {
				return true;
			}
		}
		return false;
	}

	protected boolean hasChanges(String string1, String string2) {
		if (StringUtils.isEmpty(string1) && !StringUtils.isEmpty(string2)) {
			return true;
		}
		if (!StringUtils.isEmpty(string1) && StringUtils.isEmpty(string2)) {
			return true;
		}
		if (StringUtils.isEmpty(string1) && StringUtils.isEmpty(string2)) {
			return false;
		} else {
			return !string1.equals(string2);
		}
	}

}
