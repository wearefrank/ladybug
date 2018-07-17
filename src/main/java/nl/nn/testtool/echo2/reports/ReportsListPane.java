/*
 * Created on 20-jan-10
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportsListPane extends ContentPane {
    private Logger log = LogUtil.getLogger(this);
    private ReportsComponent reportsComponent;

    public void setReportsComponent(ReportsComponent reportsComponent) {
        this.reportsComponent = reportsComponent;
    }
    
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
    public void initBean() {
        add(reportsComponent, 0);
    }
    
}
