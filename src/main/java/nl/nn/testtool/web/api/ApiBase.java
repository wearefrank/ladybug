package nl.nn.testtool.web.api;

import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.storage.Storage;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public abstract class ApiBase implements SecurityContext {
	@Context
	protected HttpServletRequest httpRequest;

	protected static ApplicationContext applicationContext;
	protected static Map<String, Storage> storages;

	protected Object getBeanObject(String beanName) {
		if (applicationContext == null)
			throw new ApiException("Api could not access the ladybug context.");

		Object bean = applicationContext.getBean(beanName);
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
	public Principal getUserPrincipal() {
		return httpRequest.getUserPrincipal();
	}

	@Override
	public boolean isUserInRoles(List<String> roles) {
		if (getUserPrincipal() == null) {
			// The servlet container didn't authenticate the user (not
			// configured in the web.xml or explicitly overwritten by the
			// servlet container (e.g. when running locally in WSAD)). In this
			// case allow everything.
			return true;
		}
		for (String role : roles) {
			if (httpRequest.isUserInRole(role))
				return true;
		}
		return false;
	}

	public static void setApplicationContext(ApplicationContext context) {
		applicationContext = context;
	}
}
