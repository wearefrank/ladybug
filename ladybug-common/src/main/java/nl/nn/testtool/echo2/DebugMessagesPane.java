/*
   Copyright 2020, 2022, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Font;
import nextapp.echo2.app.Label;

/**
 * @author Jaco de Groot
 */
public class DebugMessagesPane extends ContentPane {
	private Column column = new Column();
	private Font font = new Font(Font.MONOSPACE, Font.PLAIN, new Extent(12));
	private CheckBox scrollLockCheckBox;
	private List messages = new ArrayList();
	private long messagesLength = 0;
	private boolean even = true;
	long messageCounter = 0;
	long estimatedMemoryUsage = 0;
	SimpleDateFormat simpleDateFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	DebugMessagesPane(CheckBox lockCheckBox) {
		super();
		this.scrollLockCheckBox = lockCheckBox;
		add(column);
	}

	public void addMessage(String title, String message) {
		messageCounter++;

		Extent oldVerticalScroll = getVerticalScroll();

		if (title == null) {
			title = "";
		}
		title = "[" + simpleDateFormatDateTime.format(new Date()) + "] " + title;
		estimatedMemoryUsage = estimatedMemoryUsage + (title.length() * 2);
		Label titleLabel = new Label(title);
		titleLabel.setFont(font);
		titleLabel.setBackground(Echo2Application.getButtonRolloverBackgroundColor());
		column.add(titleLabel);

		if (message != null) {
			estimatedMemoryUsage = estimatedMemoryUsage + (message.length() * 2);
			Label messagelabel = new Label(message);
			messagelabel.setFont(font);
			column.add(messagelabel);
		}
		
		if (!scrollLockCheckBox.isSelected()) {
			setVerticalScroll(new Extent(-1));
		}

		while (estimatedMemoryUsage > 1000000) {
			Label label = (Label)column.getComponent(0);
			column.remove(0);
			estimatedMemoryUsage = estimatedMemoryUsage - (label.getText().length() * 2);
		}
	}
	
	public void removeAllMessages() {
		column.removeAll();
	}

}
