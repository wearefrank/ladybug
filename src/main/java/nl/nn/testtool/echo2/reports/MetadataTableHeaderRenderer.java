package nl.nn.testtool.echo2.reports;

import java.util.List;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Table;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.echo2.Echo2Application;
import echopointng.table.SortableTableHeaderRenderer;

public class MetadataTableHeaderRenderer extends SortableTableHeaderRenderer {
	private static final long serialVersionUID = 1L;
	private MetadataExtractor metadataExtractor;
	private List metadataNames;

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}

	public void setMetadataNames(List metadataNames) {
		this.metadataNames = metadataNames;
	}

	public Component getTableCellRendererComponent(Table table, Object value, int column, int row) {
		Button button = (Button)super.getTableCellRendererComponent(table, value, column, row);
		Echo2Application.decorateButton(button);
		button.setInsets(new Insets(0, 0, 0, 0));
		button.setText(metadataExtractor.getShortLabel((String)metadataNames.get(column)));
		button.setToolTipText(metadataExtractor.getLabel((String)metadataNames.get(column)));
		return button;
	}

}
