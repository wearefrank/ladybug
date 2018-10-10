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

import java.security.Principal;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import echopointng.tree.DefaultMutableTreeNode;
import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.app.Border;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Color;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Font;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.LayoutData;
import nextapp.echo2.app.RadioButton;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.Window;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import nextapp.echo2.webcontainer.ContainerContext;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.reports.CheckpointComponent;
import nl.nn.testtool.echo2.reports.ErrorMessageComponent;
import nl.nn.testtool.echo2.reports.InfoPane;
import nl.nn.testtool.echo2.reports.PathComponent;
import nl.nn.testtool.echo2.reports.ReportComponent;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.echo2.reports.ReportsTreeCellRenderer;
import nl.nn.testtool.echo2.reports.TreePane;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.LogUtil;

public class Echo2Application extends ApplicationInstance implements ApplicationContextAware, BeanParent, SecurityContext {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogUtil.getLogger(Echo2Application.class);

	// Three main colors on which both black an white text should be readable
	// and which are best explained by their usage in buttons.
	private static Color MAIN_COLOR_BUTTON = new Color(135, 160, 184);
	private static Color MAIN_COLOR_BUTTON_ROLLOVER = new Color(162, 182, 200);
	private static Color MAIN_COLOR_BUTTON_PRESSED = new Color(202, 214, 223);
	
	private static Color applicationBackgroundColor = Color.WHITE;
	private static Color paneBackgroundColor = MAIN_COLOR_BUTTON_PRESSED;
	private static Color buttonForegroundColor = Color.WHITE; // Text on button
	private static Color buttonBackgroundColor = MAIN_COLOR_BUTTON;
	private static Color buttonPressedBackgroundColor = MAIN_COLOR_BUTTON_PRESSED;
	private static Color buttonRolloverBackgroundColor = MAIN_COLOR_BUTTON_ROLLOVER;
	private static Border buttonBorder = new Border(1, Color.BLACK, Border.STYLE_DOTTED);
	private static Border buttonRolloverBorder = new Border(1, Color.DARKGRAY, Border.STYLE_DOTTED);
	private static Color errorBackgroundColor = new Color(216, 73, 83);
	private static Color errorForegroundColor = Color.WHITE;
	private static Color okayBackgroundColor = new Color(83, 216, 73);
	private static Color okayForegroundColor = Color.WHITE;
	private static Color differenceFoundLabelColor = Color.RED;
	private static Color noDifferenceFoundLabelColor = new Color(59, 152, 59);
	private static Color differenceFoundTextColor = Color.RED;
	private static Color noDifferenceFoundTextColor = noDifferenceFoundLabelColor;
	private static Color lineNumberTextColor = Color.WHITE;
	private static Font messageFont = new Font(Font.MONOSPACE, Font.PLAIN, new Extent(12));
	private static ColumnLayoutData columnLayoutDataForLabel;
	static {
		columnLayoutDataForLabel = new ColumnLayoutData();
		columnLayoutDataForLabel.setInsets(new Insets(0, 5, 0, 0));
	}
	private static RowLayoutData rowLayoutDataForLabel;
	
	private ApplicationContext applicationContext;
	private ContentPane contentPane;
	private TabPane tabPane;
	private TransformationWindow transformationWindow;
	private TestTool testTool;
	private ReportXmlTransformer reportXmlTransformer;
	private ReportsTreeCellRenderer reportsTreeCellRenderer;
	private Tabs tabs;
	private CrudStorage runStorage;

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setContentPane(ContentPane contentPane) {
		this.contentPane = contentPane;
	}

	public ContentPane getContentPane() {
		return contentPane;
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	public void setReportsTreeCellRenderer(ReportsTreeCellRenderer reportsTreeCellRenderer) {
		this.reportsTreeCellRenderer = reportsTreeCellRenderer;
	}

	public void setTabs(Tabs tabs) {
		this.tabs = tabs;
	}

	public Tabs getTabs() {
		return tabs;
	}

	public void setRunStorage(CrudStorage runStorage) {
		this.runStorage = runStorage;
	}

	public TransformationWindow getTransformationWindow() {
		return transformationWindow;
	}

	/**
	 * The <code>initBean()</code> methods on the classes in the
	 * <code>nl.nn.testtool.echo2</code> package should be called when they are
	 * instantiated (e.g. by Spring (after the set methods have been called)).
	 * 
	 * After a tree of beans has been created some child prototype beans need
	 * references to other beans in the tree for runtime user actions and
	 * settings (e.g. references which are not possible to create with Spring
	 * because of "unresolvable circular references"). For this the
	 * <code>initBean(BeanParent beanParent)</code> will be called to give the
	 * beans a change to require references to each other.
	 * 
	 * In Spring configuration all beans that implement <code>BeanParent</code>
	 * must have scope prototype. When one of the beans in the tree is scoped as
	 * singleton it's children will effectively be part of the last created
	 * tree. Hence part of the tree will always be part of the tree of the most
	 * recent user.
	 * 
	 * After the <code>WebContainerServlet</code> (e.g.
	 * <code>nl.nn.ibistesttool.Servlet</code>) returns the
	 * <code>ApplicationInstance</code> (this class) the Echo2 framework will
	 * call the init methods when building a hierarchy of <code>Component</code>
	 * objects (see
	 * http://echo.nextapp.com/content/echo2/doc/api/2.1/public/app/nextapp/echo2/app/Component.html#init())
	 * When a component is added because of a user action the
	 * <code>init()</code> method will also be called (again). The dispose()
	 * method will be called when a component is removed from the hierarchy.
	 */
	public void initBean() {

		// Construct

		contentPane.setBackground(applicationBackgroundColor);

		transformationWindow = new TransformationWindow();

		tabPane = new TabPane();
		tabPane.setBackground(paneBackgroundColor);
		tabPane.setTabActiveBackground(buttonBackgroundColor);
		tabPane.setTabActiveForeground(buttonForegroundColor);
		tabPane.setTabInactiveBackground(paneBackgroundColor);

		// Wire

		transformationWindow.setReportXmlTransformer(reportXmlTransformer);

		for (ContentPane tab : tabs) {
			tabPane.add(tab);
		}

		contentPane.add(transformationWindow);
		contentPane.add(tabPane);

		// Init

		transformationWindow.initBean();
		initBean(this);
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		tabs.initBean(this);
	}

	public BeanParent getBeanParent() {
		return null;
	}

	public static Echo2Application getEcho2Application(BeanParent beanParent, Object child) {
		String message = beanParent.getClass().getSimpleName() + "(" + beanParent.hashCode() + "), " + child.getClass().getSimpleName() + "(" + child.hashCode() + ")";
		while (beanParent.getBeanParent() != null) {
			beanParent = beanParent.getBeanParent();
			message = beanParent.getClass().getSimpleName() + "(" + beanParent.hashCode() + "), " + message;
		}
		message = "BeanParent path: " + message;
		log.debug(message);
		return (Echo2Application)beanParent;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public Window init() {
		String version = TestTool.getName() + " " + TestTool.getVersion();
		if (testTool.getConfigName() != null) {
			version = version + " - " + testTool.getConfigName();
			if (testTool.getConfigVersion() != null) {
				version = version + " " + testTool.getConfigVersion();
			}
		}
		Window window = new Window();
		window.setTitle(version);
		window.setContent(contentPane);
		return window;
	}

	public void openReport(Report report) {
//		tabPane.setActiveTabIndex(0);
//		reportsComponent.openReport(report, false);


		boolean reportFound = false;
		for (int i = 0; i < tabPane.getComponentCount(); i++) {
			Component component = tabPane.getComponent(i);
			if (component instanceof ReportPane) {
				// TODO ook nog checken dat het dezelfde storage is (reports comparen werkt niet omdat bij het opnieuw ophalen van een report uit de storage een nieuw object wordt aangemaakt, of we moeten hier het report uit storage halen a.d.h.v. storage id?)
				if (((ReportPane)component).getReport().getStorageId().intValue() == report.getStorageId().intValue()) {
					tabPane.setActiveTabIndex(i);
					reportFound = true;
				}
			}
		}

		if (!reportFound) {
			// Construct
	
			// TODO Via Spring doen, zie ook applicationContext.getBean("reportsComponent") verderop
			ReportPane reportPane = new ReportPane();
	
			TabPaneLayoutData tabPaneLayoutReportPane = new TabPaneLayoutData();
	
			TreePane treePane = new TreePane();
	
			InfoPane infoPane = new InfoPane();
			
			PathComponent pathComponent = new PathComponent();
	
			ErrorMessageComponent errorMessageComponent = new ErrorMessageComponent();
	
			// Wire
	
			// TODO op reportPane zou reportsComponent gezet moeten worden en op reportsComponent de setTree en setInfo?
			reportPane.setLayoutData(tabPaneLayoutReportPane);
			reportPane.setReport(report);
			reportPane.setTreePane(treePane);
			reportPane.setInfoPane(infoPane);
	
			String title = report.getName();
			if (title.length() > 10) {
				title = title.subSequence(0, 10) + "...";
			}
			tabPaneLayoutReportPane.setTitle(title);
	
			treePane.setTestTool(testTool);
			treePane.setInfoPane(infoPane);
			treePane.setReportsTreeCellRenderer(reportsTreeCellRenderer);

			// TODO van pathComponent, checkpointComponent (nu gedaan) en errorMessageComponent moeten ook nieuwe instanties worden aangemaakt (bij switchen tussen tabs die allebei op zelfde component staan wordt er eentje leeg)
			ReportComponent reportComponent = new ReportComponent();
			reportComponent.setTestTool(testTool);
			reportComponent.setRunStorage(runStorage);
			reportComponent.setTreePane(treePane);
			reportComponent.setInfoPane(infoPane);
			reportComponent.initBean();
			reportComponent.initBean(this);

			// TODO code sharen met DebugPane? misschien makkelijkste door kleinere componenten ook met Spring te doen?
			CheckpointComponent checkpointComponent = new CheckpointComponent();
			checkpointComponent.setTestTool(testTool);
			checkpointComponent.setTreePane(treePane);
			checkpointComponent.setInfoPane(infoPane);
			checkpointComponent.initBean();
			checkpointComponent.initBean(this);
			
			infoPane.setReportComponent(reportComponent);
			infoPane.setPathComponent(pathComponent);
			infoPane.setCheckpointComponent(checkpointComponent);
			infoPane.setErrorMessageComponent(errorMessageComponent);
	
			
			// Init
			reportPane.initBean();
			treePane.initBean();
			infoPane.initBean();
			pathComponent.initBean();
	
			tabPane.add(reportPane);
			tabPane.setActiveTabIndex(tabPane.getComponentCount() - 1);
	
			// TODO bovenstaande ook via spring
			// TODO reportComponent maken/gebruiken i.p.v. reportsComponent? ReportsComponent hernoemen/aanpassen naar ReportTreeInfoComponent (er is al een ReportComponent)
			ReportsComponent reportsComponent = (ReportsComponent)applicationContext.getBean("reportsComponent");
			reportsComponent.setTreePane(treePane);
	//		reportsComponent.setReportUploadListener(reportUploadListener);
			reportsComponent.setReportXmlTransformer(reportXmlTransformer);
			reportsComponent.initBean(this);
			
			// TODO reportsComponent.openReport in reportPane (initBean) doen?
			reportsComponent.openReport(report);
		}
	}

	public void closeReport()  {
		if (tabPane.getActiveTabIndex() > tabPane.getComponentCount() - 1) {
			// Workaround (when closing the two latest reports after each other
			// the getActiveTabIndex() is one too large on the second close)
			tabPane.remove(tabPane.getComponentCount() - 1);
		} else {
			tabPane.remove(tabPane.getActiveTabIndex());
		}
	}

	public void openReportCompare(Report report1, Report report2) {
		tabPane.setActiveTabIndex(2);
		ComparePane comparePane = null;
		for (Tab tab : tabs) {
			if (tab instanceof ComparePane) {
				comparePane = (ComparePane)tab;
			}
		}
		DefaultMutableTreeNode reportNode1;
		DefaultMutableTreeNode reportNode2;
		reportNode1 = comparePane.getTreePane1().addReport(report1, comparePane.getReportsComponent1().getViews().getDefaultView(), false);
		reportNode2 = comparePane.getTreePane2().addReport(report2, comparePane.getReportsComponent2().getViews().getDefaultView(), false);
		comparePane.compare(report1, report2);
		comparePane.getTreePane1().selectNode(reportNode1);
	}

	public static String store(CrudStorage storage, Report report) {
		String errorMessage = null;
		try {
			storage.store(report);
		} catch (StorageException e) {
			log.error(e);
			errorMessage = e.getMessage();
		}
		return errorMessage;
	}

	public static String update(CrudStorage storage, Report report) {
		String errorMessage = null;
		try {
			storage.update(report);
		} catch (StorageException e) {
			log.error(e);
			errorMessage = e.getMessage();
		}
		return errorMessage;
	}

	public static String delete(CrudStorage storage, Report report) {
		String errorMessage = null;
		try {
			storage.delete(report);
		} catch (StorageException e) {
			log.error(e);
			errorMessage = e.getMessage();
		}
		return errorMessage;
	}

	public static String deleteAll(CrudStorage storage) {
		String errorMessage = null;
		List ids;
		try {
			ids = storage.getStorageIds();
			for (int i = 0; i < ids.size(); i++) {
				Integer id = (Integer)ids.get(i);
				Report report = storage.getReport(id);
				// TODO op basis van storageId doen i.p.v. report?
				storage.delete(report);
			}
		} catch (StorageException e) {
			log.error(e);
			errorMessage = e.getMessage();
		}
		return errorMessage;
	}

	public static Color getApplicationBackgroundColor() {
		return applicationBackgroundColor;
	}

	public static Color getPaneBackgroundColor() {
		return paneBackgroundColor;
	}

	public static Color getButtonBackgroundColor() {
		return buttonBackgroundColor;
	}

	public static Color getButtonForegroundColor() {
		return buttonForegroundColor;
	}

	public static Color getButtonPressedBackgroundColor() {
		return buttonPressedBackgroundColor;
	}

	public static Color getButtonRolloverBackgroundColor() {
		return buttonRolloverBackgroundColor;
	}
	
	public static Color getErrorBackgroundColor() {
		return errorBackgroundColor;
	}
	
	public static Color getErrorForegroundColor() {
		return errorForegroundColor;
	}

	public static Color getOkayBackgroundColor() {
		return okayBackgroundColor;
	}
	
	public static Color getOkayForegroundColor() {
		return okayForegroundColor;
	}

	public static Color getDifferenceFoundLabelColor() {
		return differenceFoundLabelColor;
	}
	
	public static Color getNoDifferenceFoundLabelColor() {
		return noDifferenceFoundLabelColor;
	}

	public static Color getDifferenceFoundTextColor() {
		return differenceFoundTextColor;
	}

	public static Color getNoDifferenceFoundTextColor() {
		return noDifferenceFoundTextColor;
	}

	public static Color getLineNumberTextColor() {
		return lineNumberTextColor;
	}

	public static Row getNewRow() {
		Row row = new Row();
		row.setCellSpacing(new Extent(5));
		return row; 
	}
	
	public static void decorateButton(Button button) {
		button.setInsets(new Insets(5, 2));
		button.setForeground(Color.WHITE);
		button.setBackground(buttonBackgroundColor);
		button.setPressedEnabled(true);
		button.setPressedBackground(buttonPressedBackgroundColor);
		button.setBorder(buttonBorder);
		button.setRolloverEnabled(true);
		button.setRolloverBorder(buttonRolloverBorder);
		button.setRolloverBackground(buttonRolloverBackgroundColor);
	}
	
	public static void decorateRadioButton(RadioButton radioButton) {
		radioButton.setInsets(new Insets(5, 2));
		radioButton.setForeground(Color.WHITE);
		radioButton.setBackground(buttonPressedBackgroundColor);
		radioButton.setPressedEnabled(true);
		radioButton.setPressedBackground(buttonRolloverBackgroundColor);
		radioButton.setBorder(buttonRolloverBorder);
		radioButton.setRolloverEnabled(true);
		radioButton.setRolloverBorder(buttonRolloverBorder);
		radioButton.setRolloverBackground(buttonBackgroundColor);
	}

	private static Label createInfoLabel(LayoutData layoutData) {
		Label label = new Label();
		if (layoutData != null) {
			label.setLayoutData(layoutData);
		}
		label.setBackground(getButtonRolloverBackgroundColor());
		return label;
	}

	private static Label createErrorLabel(LayoutData layoutData) {
		Label label = createInfoLabel(layoutData);
		label.setBackground(getErrorBackgroundColor());
		label.setForeground(getErrorForegroundColor());
		return label;
	}

	private static Label createOkayLabel(LayoutData layoutData) {
		Label label = createInfoLabel(layoutData);
		label.setBackground(getOkayBackgroundColor());
		label.setForeground(getOkayForegroundColor());
		return label;
	}

	public static Label createInfoLabel() {
		return createInfoLabel(null);
	}

	public static Label createInfoLabelWithColumnLayoutData() {
		return createInfoLabel(columnLayoutDataForLabel);
	}

	public static Label createErrorLabelWithColumnLayoutData() {
		return createErrorLabel(columnLayoutDataForLabel);
	}

	public static Label createOkayLabelWithColumnLayoutData() {
		return createOkayLabel(columnLayoutDataForLabel);
	}

	public static Label createInfoLabelWithRowLayoutData() {
		return createInfoLabel(rowLayoutDataForLabel);
	}

	public static Label createErrorLabelWithRowLayoutData() {
		return createErrorLabel(rowLayoutDataForLabel);
	}
	
	public static Font getMessageFont() {
		return messageFont;
	}

	private ContainerContext getContainerContext() {
		return (ContainerContext) getContextProperty(ContainerContext.CONTEXT_PROPERTY_NAME);
	}

	public Principal getUserPrincipal() {
		ContainerContext containerContext = getContainerContext();
		if (containerContext != null) {
			return containerContext.getUserPrincipal();
		}
		return null;
	}

	public boolean isUserInRoles(List<String> roles) {
		ContainerContext containerContext = getContainerContext();
		
		if (containerContext.getUserPrincipal() == null) {
			// The servlet container didn't authenticate the user (not
			// configured in the web.xml or explicitly overwritten by the
			// servlet container (e.g. when running locally in WSAD)). In this
			// case allow everything.
			return true;
		} else {
			for (String role : roles) {
				if (containerContext.isUserInRole(role)) {
					return true;
				}
			}
		}
		return false;
	}

	public String getCommandIssuedBy() {
		ContainerContext containerContext = getContainerContext();
		if (containerContext != null) {
			String commandIssuedBy = " remoteHost [" + containerContext.getClientProperties().getString("remoteHost")
			+ "]";
			commandIssuedBy += " remoteUser [" + getUserPrincipal() + "]";
			return commandIssuedBy;
		}
		return null;
	}
}