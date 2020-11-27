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
