package nl.nn.testtool.echo2;

import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import nl.nn.testtool.echo2.run.InfoPane;
import nl.nn.testtool.echo2.run.TreePane;

public class RunPane extends Tab implements BeanParent {
	private static final long serialVersionUID = 1L;
	private String title = "Test";
	private TreePane treePane;
	private InfoPane infoPane;

	public RunPane() {
		super();
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
	}

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

		// Construct

		TabPaneLayoutData tabPaneLayoutTestPane = new TabPaneLayoutData();
		tabPaneLayoutTestPane.setTitle(title);
		setLayoutData(tabPaneLayoutTestPane);

		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane1.setResizable(true);
		splitPane1.setSeparatorPosition(new Extent(400, Extent.PX));

		// Wire

		splitPane1.add(treePane);
		splitPane1.add(infoPane);
		add(splitPane1);

	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		treePane.initBean(this);
		infoPane.initBean(this);
	}

}