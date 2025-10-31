/*
   Copyright 2021-2022, 2024-2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.web.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import nl.nn.testtool.SecurityContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public abstract class ApiBase implements SecurityContext {
	protected static ApplicationContext applicationContext;

	private static HttpServletRequest getHttpServletRequest() {
		ServletRequestAttributes requestAttributes =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return requestAttributes.getRequest();
	}

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
		getHttpServletRequest().getSession().setAttribute(key, value);
	}

	protected <T> T getSessionAttr(String key) {
		return getSessionAttr(key, true);
	}

	protected <T> T getSessionAttr(String key, boolean throwException) {
		T object = (T) getHttpServletRequest().getSession().getAttribute(key);
		if (object == null && throwException)
			throw new ApiException("No session attribute with name [" + key + "] found.");
		return object;
	}

	@Override
	public Principal getUserPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication != null) {
			return authentication.getName();
		}
		return null;
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
			if (getHttpServletRequest().isUserInRole(role))
				return true;
		}
		return false;
	}

	/**
	 * Checks if the given map contains the given arrays of keys, and only those keys.
	 * @param map Map to be checked.
	 * @param mandatory List of mandatory keys. If any of them is not present, will return false.
	 * @param optional List of optional keys.
	 * @return True if the given two arrays are the only keys in the given map.
	 */
	public boolean mapContainsOnly(Map<String, ?> map,String[] mandatory, String[] optional) {
		int count = 0;
		if (mandatory != null) {
			for (String field : mandatory) {
				if (!map.containsKey(field)) return false;
			}
			count = mandatory.length;
		}
		if (optional != null) {
			for (String field: optional) {
				if (map.containsKey(field)) count++;
			}
		}
		return map.size() == count;
	}

	public static void setApplicationContext(ApplicationContext context) {
		applicationContext = context;
	}
}
