package nl.nn.testtool.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;

/**
 * @author Jaco de Groot
 */
public interface CheckpointMatcher {

	public boolean match(Report report, Checkpoint checkpoint);
	
}
