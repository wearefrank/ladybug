package nl.nn.testtool.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

public class ApiException extends WebApplicationException implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public ApiException(String msg, Throwable t) {
		super(t, 500);
		logger.error(msg, t);
		System.err.println(msg);
	}

	public ApiException(String msg) {
		super(500);
		logger.error(msg);
		System.err.println(msg);
	}
}
