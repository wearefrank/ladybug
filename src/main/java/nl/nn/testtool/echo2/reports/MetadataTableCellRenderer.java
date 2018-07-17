package nl.nn.testtool.echo2.reports;

import java.util.List;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.layout.TableLayoutData;
import nextapp.echo2.app.table.DefaultTableCellRenderer;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.echo2.Echo2Application;

public class MetadataTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final TableLayoutData tableLayoutData = new TableLayoutData();
	static {
		tableLayoutData.setInsets(new Insets(2, 0, 4, 0));
	}
	private MetadataExtractor metadataExtractor;
	private List metadataNames;

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}

	public void setMetadataNames(List metadataNames) {
		this.metadataNames = metadataNames;
	}

	public Component getTableCellRendererComponent(Table table, Object value,
			int column, int row) {
		Label label = (Label)super.getTableCellRendererComponent(table, value, column, row);
		if (label == null) {
			label = new Label();
		}
		label.setLayoutData(tableLayoutData);
		label.setToolTipText(metadataExtractor.getLabel((String)metadataNames.get(column)));
		return label;
	}

}
