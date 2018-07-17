/*
 * Created on 28-Jan-10
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool;

import java.security.Principal;
import java.util.List;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface Debugger {

	public List getStubStrategies();

	public String getDefaultStubStrategy();

	/**
	 * Checks whether a checkpoint will be stubbed so code can be skipped.
	 * 
	 * @param checkpoint  the checkpoint that may be stubbed
	 * @param strategy    the used subbing strategy
	 * @return            <code>true</code> when this checkpoint will be stubbed 
	 */
	public boolean stub(Checkpoint checkpoint, String strategy);

	/**
	 * Rerun a previous generated report. This method should at least trigger
	 * the same first checkpoint as has been triggered when the original report
	 * was created.
	 *  
	 * @param correlationId   the correlationId to be used
	 * @param originalReport  the original report that should be rerun
	 * @return                an error message when an error occurred 
	 */
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext);

}
