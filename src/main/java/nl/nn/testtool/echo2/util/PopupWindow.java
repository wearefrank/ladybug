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

	public PopupWindow(String title, String message, int with, int height,
			List actionLabels, List actionCommands, List actionListeners) {
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
		Iterator actionLabelsIterator = actionLabels.iterator();
		Iterator actionCommandsIterator = actionCommands.iterator();
		Iterator actionListenersIterator = actionListeners.iterator();
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
