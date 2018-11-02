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
package nl.nn.testtool.echo2.util;

import java.util.Iterator;
import java.util.List;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nl.nn.testtool.echo2.Echo2Application;

public class PopupWindow extends WindowPane implements ActionListener {
	private static final long serialVersionUID = 1L;

	public PopupWindow(String title, String message, int with, int height,
			List<String> actionLabels, List<String> actionCommands, List<ActionListener> actionListeners) {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModal(true);
		setTitle(title);
		setInsets(new Insets(10, 10, 10, 0));
		setWidth(new Extent(with));
		setHeight(new Extent(height));

		Label label = new Label(message);
		ColumnLayoutData columnLayoutData = new ColumnLayoutData();
		columnLayoutData.setInsets(new Insets(0, 0, 0, 10));
		label.setLayoutData(columnLayoutData);

		Row buttonRow = Echo2Application.getNewRow();
		Iterator<String> actionLabelsIterator = actionLabels.iterator();
		Iterator<String> actionCommandsIterator = actionCommands.iterator();
		Iterator<ActionListener> actionListenersIterator = actionListeners.iterator();
		while (actionCommandsIterator.hasNext() && actionListenersIterator.hasNext()) {
			String actionLabel = (String)actionLabelsIterator.next();
			String actionCommand = (String)actionCommandsIterator.next();
			ActionListener actionListener = (ActionListener)actionListenersIterator.next();
			Button button = new Button(actionLabel);
			Echo2Application.decorateButton(button);
			button.setActionCommand(actionCommand);
			button.addActionListener(actionListener);
			button.addActionListener(this);
			buttonRow.add(button);
		}

		Column column = new Column();
		column.add(label);
		column.add(buttonRow);
		add(column);
		
	}

	public void actionPerformed(ActionEvent e) {
		userClose();
	}

}
