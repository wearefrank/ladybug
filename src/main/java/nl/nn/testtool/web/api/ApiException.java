package nl.nn.testtool.web.api;

import nl.nn.testtool.util.LogUtil;
import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import java.io.Serializable;

public class ApiException extends WebApplicationException implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger logger = LogUtil.getLogger(ApiException.class);

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
