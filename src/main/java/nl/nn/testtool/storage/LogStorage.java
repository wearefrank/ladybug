package nl.nn.testtool.storage;

import nl.nn.testtool.Report;

/**
 * Storage (optimised) for logging. Targeted at minimising the overhead on the
 * process being logged (minimal delay and interruptions).
 * 
 * @author Jaco de Groot
 */
public interface LogStorage extends Storage {

	public void storeWithoutException(Report report);

}
