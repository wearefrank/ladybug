/*
 * Created on 01-Nov-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool;

import nl.nn.testtool.echo2.DebugPane;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface TestToolListener {

	public void checkpoint(String correlationId, String name, Object message,
			int checkpointType,	boolean startpoint, boolean endpoint,
			int levelChange);

}
