package nl.nn.testtool.web.api;

import nl.nn.testtool.storage.Storage;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Map;

public abstract class ApiBase implements ApplicationContextAware {
	@Context
	private HttpServletRequest httpRequest;

	ApplicationContext appContext;
	protected static Map<String, Storage> storages;

	protected Object getBeanObject(String beanName) {
		if (appContext == null)
			throw new ApiException("APPLICATION CONTEXT IS NULL!!!!");

		Object bean = appContext.getBean(beanName);
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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		System.err.println("SETTING THE APPLICATION CONTEXT!!!");
		appContext = applicationContext;
	}
}
