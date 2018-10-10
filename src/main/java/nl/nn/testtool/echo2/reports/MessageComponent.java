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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.layout.RowLayoutData;
import nl.nn.testtool.echo2.Echo2Application;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MessageComponent extends Column {
	private static final char REPLACE_NON_XML_CHAR = 0x00BF; // Inverted question mark.
	private String message;
	private String messageCompare;
	private boolean compare;
	protected Column messageColumn;
	protected Button lineNumbersButton;
	protected Button editButton;
	protected Button saveButton;
	protected InfoPane infoPane;
	private TextArea editTextArea;
	
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	protected void initBeanPre() {
		messageColumn = new Column();
		messageColumn.setInsets(new Insets(0, 5, 0, 0));
		editTextArea = new TextArea();
		editTextArea.setWidth(new Extent(100, Extent.PERCENT));
		editTextArea.setHeight(new Extent(300));
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	protected void initBeanPost() {
		updateLineNumbersButton();
		updateEditButton();
		updateSaveButton();
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
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
				removeLineNumbers();
			} else {
				infoPane.showLineNumbers(true);
				addLineNumbers();
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
	
	protected String save() {
		message = editTextArea.getText();
		return message;
	}
	
	private void updateMessageComponents() {
		updateLineNumbersButton();
		updateEditButton();
		updateSaveButton();
		messageColumn.removeAll();
		if (infoPane.edit()) {
			editTextArea.setText(replaceNonValidXmlCharacters(message, null, false));
			messageColumn.add(editTextArea);
		} else {
			if (message != null) {
				LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(message));
				String line = readLineIgnoringExceptions(lineNumberReader);
				LineNumberReader lineNumberReaderCompare = null;
				String lineCompare = null;
				if (messageCompare != null) {
					lineNumberReaderCompare = new LineNumberReader(new StringReader(messageCompare));
					lineCompare = readLineIgnoringExceptions(lineNumberReaderCompare);
				}
				while (line != null) {
					addLine(line, lineCompare, compare);
					line = readLineIgnoringExceptions(lineNumberReader);
					if (messageCompare != null) {
						lineCompare = readLineIgnoringExceptions(lineNumberReaderCompare);
					}
				}
			}
			if (infoPane.showLineNumbers()) {
				addLineNumbers();
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

	private void addLine(String line, String lineCompare, boolean compare) {
		Row row = new Row();
		messageColumn.add(row);
		boolean differenceFound = compare && !line.equals(lineCompare);
		replaceNonValidXmlCharacters(line, row, differenceFound);
	}

	private Label createLabel(String text, boolean applyIndent, boolean differenceFound) {
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

	private void setIndentAndText(Label label, String line) {
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

	private void setIndent(Label label, int preNumberOfChars, int postNumberOfChars, boolean whiteSpaceOnly) {
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
	
	private void addLineNumbers() {
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

	private void removeLineNumbers() {
		for (int i = 0; i < messageColumn.getComponentCount(); i++) {
			Row row = (Row)messageColumn.getComponent(i);
			row.remove(0);
		}
	}

	private String replaceNonValidXmlCharacters(String string, Row row, boolean differenceFound) {
		StringBuffer buffer = new StringBuffer();
		int c;
		for (int i = 0; i < string.length(); i += Character.charCount(c)) {
			c = string.codePointAt(i);
			if (isPrintableUnicodeChar(c)) {
				buffer.appendCodePoint(c);
			} else {
				String substitute = REPLACE_NON_XML_CHAR + "#" + c + ";";
				if (row == null) {
					buffer.append(substitute);
				} else {
					row.add(createLabel(buffer.toString(), true, differenceFound));
					Label label = createLabel(substitute, true, differenceFound);
					label.setBackground(Echo2Application.getButtonRolloverBackgroundColor());
					row.add(label);
					buffer = new StringBuffer();
				}
			}
		}
		if (row != null) {
			row.add(createLabel(buffer.toString(), true, differenceFound));
		}
		return buffer.toString();
	}

	// Copied from IAF XmlUtils
	public static boolean isPrintableUnicodeChar(int c) {
		return (c == 0x0009)
			|| (c == 0x000A)
			|| (c == 0x000D)
			|| (c >= 0x0020 && c <= 0xD7FF)
			|| (c >= 0xE000 && c <= 0xFFFD)
			/* Prevent application crash 
			|| (c >= 0x0010000 && c <= 0x0010FFFF)*/;
	}

}
