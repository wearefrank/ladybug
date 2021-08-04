/*
   Copyright 2021 WeAreFrank!

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

	/**
	 * This method is crucial to connect the ApplicationContext from framework, with the context from Servlets of the
	 * api.
	 * @param applicationContext Application context from the framework.
	 * @throws BeansException Thrown when an error occurs with a Bean.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApiBase.setApplicationContext(applicationContext);
	}

	/**
	 * Sets the roles map for {@link AuthenticationInterceptor}. {@link AuthenticationInterceptor} is used to filter
	 * the requests based on their permission requirements.
	 *
	 * @param authenticationRolesMap is a map explaining {@link AuthenticationInterceptor} configuration, where keys
	 *                                  are strings explaining possible requests (such as "POST,PUT/path/to/endpoint").
	 *                                  Method part can be left empty in order to apply it for all methods
	 *                                  (such as "/path/to/endpoint").
	 */
	public void setAuthenticationRolesMap(Map<String, List<String>> authenticationRolesMap) {
		AuthenticationInterceptor.setRolesMap(authenticationRolesMap);
	}
}