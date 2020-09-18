package nl.nn.testtool.web.api;

import nl.nn.testtool.storage.Storage;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Map;

public abstract class ApiBase {
	@Context
	ServletContext context;

	@Context
	private HttpServletRequest httpRequest;


	protected static Map<String, Storage> storages;

	protected Object getBeanObject(String beanName) {
		Object bean = context.getAttribute(beanName);
		if (bean == null)
			throw new ApiException("Could not retrieve the requested bean [" + beanName + "]");
		return bean;
	}

	protected <T> T getBean(String beanName) {
		try {
			return (T) getBeanObject(beanName);
		} catch (ClassCastException e) {
			throw new ApiException("Could not cast the bean to required class.", e);
		}
	}

	protected void setSessionAttr(String key, Object value) {
		httpRequest.getSession().setAttribute(key, value);
	}

	protected <T> T getSessionAttr(String key) {
		return getSessionAttr(key, true);
	}

	protected <T> T getSessionAttr(String key, boolean throwException) {
		T object = (T) httpRequest.getSession().getAttribute(key);
		if (object == null && throwException)
			throw new ApiException("No session attribute with name [" + key + "] found.");
		return object;
	}
}
