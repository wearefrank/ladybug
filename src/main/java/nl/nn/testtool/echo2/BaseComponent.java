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
package nl.nn.testtool.echo2;

import org.apache.log4j.Logger;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nl.nn.testtool.util.LogUtil;

/**
 * @author Jaco de Groot
 */
public class BaseComponent extends Column {
	private static final long serialVersionUID = 1L;
	protected Logger log = LogUtil.getLogger(this);
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

	public void displayError(Throwable t) {
		displayError(null, t.getClass().getName() + ": " + t.getMessage(), t);
	}

	public void displayError(String message) {
		displayError(message, message, null);
	}

	private void displayError(String logMessage, String displayMessage, Throwable t) {
		if (t == null) {
			log.error(logMessage);
		} else {
			if (logMessage == null) {
				// don't use log.error(t) as it will print the name of the Throwable but no stack trace
				logMessage = t.getMessage();
			}
			log.error(logMessage, t);
		}
		if (errorLabel.isVisible()) {
			errorLabel.setText(errorLabel.getText() + " [" + displayMessage + "]");
		} else {
			errorLabel.setText("[" + displayMessage + "]");
			errorLabel.setVisible(true);
		}
	}

	protected void displayOkay(String message) {
		okayLabel.setText(message);
		okayLabel.setVisible(true);
	}

	protected void hideMessages() {
		errorLabel.setVisible(false);
		okayLabel.setVisible(false);
	}

	protected void hideErrorMessage() {
		errorLabel.setVisible(false);
	}

}
