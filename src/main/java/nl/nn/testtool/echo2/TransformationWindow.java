package nl.nn.testtool.echo2;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nl.nn.testtool.transform.ReportXmlTransformer;
import echopointng.tree.DefaultTreeCellRenderer;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TransformationWindow extends WindowPane implements ActionListener {
	private static final long serialVersionUID = 1L;
	private TextArea textArea;
	private Label errorLabel;
	private ReportXmlTransformer reportXmlTransformer = null;

	public TransformationWindow() {
		super();

		// Construct

		setTitle("Report XML transformation");
		setVisible(false);
		setWidth(new Extent(800));
		setHeight(new Extent(600));
		setInsets(new Insets(5, 5, 10, 5));
		setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);

		Column column = new Column();
		column.setCellSpacing(new Extent(10));

		textArea = new TextArea();
		// TODO DefaultTreeCellRenderer.DEFAULT_FONT wordt elders ook gebruikt, via Echo2App doen
		// TODO weer courier maken?
		textArea.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
		textArea.setWidth(new Extent(100, Extent.PERCENT));
		textArea.setHeight(new Extent(515));

		Button buttonSaveTranformation  = new Button("Save transformation");
		buttonSaveTranformation.setActionCommand("SaveTransformation");
		Echo2Application.decorateButton(buttonSaveTranformation);
		buttonSaveTranformation.addActionListener(this);

		Button buttonMoreHeight = new Button("More height");
		buttonMoreHeight.setActionCommand("MoreHeight");
		Echo2Application.decorateButton(buttonMoreHeight);
		buttonMoreHeight.addActionListener(this);

		Button buttonLessHeight = new Button("Less height");
		buttonLessHeight.setActionCommand("LessHeight");
		Echo2Application.decorateButton(buttonLessHeight);
		buttonLessHeight.addActionListener(this);

		Row buttonRow = Echo2Application.getNewRow();

		errorLabel = Echo2Application.createErrorLabelWithRowLayoutData();
		errorLabel.setVisible(false);

		// Wire

		buttonRow.add(buttonSaveTranformation);
		buttonRow.add(buttonMoreHeight);
		buttonRow.add(buttonLessHeight);
		buttonRow.add(errorLabel);

		column.add(buttonRow);
		column.add(textArea);

		add(column);

		// Init

	}
	
	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	public ReportXmlTransformer getReportXmlTransformer() {
		return reportXmlTransformer;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		textArea.setText(reportXmlTransformer.getXslt());
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("SaveTransformation")) {
			String errorMessage = reportXmlTransformer.setXslt(textArea.getText());
			if (errorMessage == null) {
				errorLabel.setVisible(false);
			} else {
				errorLabel.setText(errorMessage);
				errorLabel.setVisible(true);
			}
		} else if (e.getActionCommand().equals("MoreHeight")) {
			textArea.setHeight(Extent.add(textArea.getHeight(), new Extent(10)));
		} else if (e.getActionCommand().equals("LessHeight")) {
			textArea.setHeight(Extent.add(textArea.getHeight(), new Extent(-10)));
		}
	}

}
