package nl.nn.testtool.web;

import nl.nn.testtool.web.api.ApiBase;
import nl.nn.testtool.web.interceptors.AuthenticationInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;

public class ApiConfigurationProvider implements ApplicationContextAware {
	Map<String, List<String>> authenticationRolesMap;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApiBase.setApplicationContext(applicationContext);
	}

	public void setAuthenticationRolesMap(Map<String, List<String>> authenticationRolesMap) {
		AuthenticationInterceptor.setRolesMap(authenticationRolesMap);
	}
}