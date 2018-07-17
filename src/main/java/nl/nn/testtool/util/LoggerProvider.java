package nl.nn.testtool.util;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 */
public interface LoggerProvider {

	public Logger getLogger(String name);

}
