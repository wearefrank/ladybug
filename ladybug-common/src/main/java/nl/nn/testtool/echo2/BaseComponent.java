/*
   Copyright 2020, 2022, 2024, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.event.ActionEvent;

/**
 * @author Jaco de Groot
 */
public class BaseComponent extends Column {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected Label errorLabel;
	protected Label okayLabel;

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		setInsets(new Insets(10));

		// Construct

		errorLabel = Echo2Application.createErrorLabelWithColumnLayoutData();
		errorLabel.setVisible(false);

		okayLabel = Echo2Application.createOkayLabelWithColumnLayoutData();
		okayLabel.setVisible(false);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 * @param e ...
	 */
	public void actionPerformed(ActionEvent e) {
		hideMessages();
	}

	public void displayOkay(String message) {
		if (message != null) {
			displayMessage(okayLabel, message);
		}
	}

	public void displayError(String message) {
		if (message != null) {
			handleError(message, false, null, null);
		}
	}

	public void displayAndLogError(String message) {
		if (message != null) {
			handleError(message, true, message, null);
		}
	}

	public void displayAndLogError(Throwable t) {
		handleError(t.getClass().getName() + ": " + t.getMessage(), true, null, t);
	}

	private void handleError(String displayMessage, boolean useLog, String logMessage, Throwable t) {
		if (useLog) {
			if (t == null) {
				log.error(logMessage);
			} else {
				if (logMessage == null) {
					logMessage = t.getMessage();
				}
				log.error(logMessage, t);
			}
		}
		displayMessage(errorLabel, displayMessage);
	}

	private void displayMessage(Label label, String message) {
		if (label.isVisible()) {
			label.setText(label.getText() + " [" + message + "]");
		} else {
			label.setText("[" + message + "]");
			label.setVisible(true);
		}
	}

	protected void hideMessages() {
		errorLabel.setVisible(false);
		okayLabel.setVisible(false);
	}

}
