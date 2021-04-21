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


import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

@WebListener
public class ServletListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		ServletContext context = servletContextEvent.getServletContext();

		// Add ladybug backend
		ServletRegistration.Dynamic serv = context.addServlet("ladybug", ApiServlet.class);
		serv.setLoadOnStartup(0);
		serv.addMapping("/ladybug/*");
		serv.setInitParameters(ApiServlet.getInitParameters());

		// Add ladybug frontend server
		serv = context.addServlet("ladybug-frontend", FrontendServlet.class);
		serv.setLoadOnStartup(0);
		serv.addMapping("/ladybug/frontend/*");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}
}
