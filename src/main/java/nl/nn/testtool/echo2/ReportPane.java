package nl.nn.testtool.echo2;

import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.reports.InfoPane;
import nl.nn.testtool.echo2.reports.TreePane;

public class ReportPane extends ContentPane {
//	private Logger log = LogUtil.getLogger(this);
	private Report report;
	private TreePane treePane;
	private InfoPane infoPane;
//	private ReportsListPane reportsListPane;

	public ReportPane() {
		super();
	}

	public void setReport(Report report) {
		this.report = report;
	}

	public Report getReport() {
		return report;
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
	}

//	public void setReportsListPane(ReportsListPane reportsListPane) {
//		this.reportsListPane = reportsListPane;
//	}

	public TreePane getTreePane() {
		return treePane;
	}
	
	public InfoPane getInfoPane() {
		return infoPane;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		SplitPane splitPane = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane.setResizable(true);
		splitPane.setSeparatorPosition(new Extent(400, Extent.PX));
		splitPane.add(treePane);
		splitPane.add(infoPane);
		add(splitPane);
	}

}