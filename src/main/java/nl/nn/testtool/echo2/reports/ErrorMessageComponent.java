
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nl.nn.testtool.echo2.Echo2Application;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ErrorMessageComponent extends Column {
	private Label errorLabel;

	public ErrorMessageComponent() {
		super();
		setInsets(new Insets(10));
		errorLabel = Echo2Application.createErrorLabelWithColumnLayoutData();
		add(errorLabel);
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
	}

	public void displayErrorMessage(String errorMessage) {
		errorLabel.setText(errorMessage);
	}
}
