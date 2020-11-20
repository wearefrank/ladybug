package nl.nn.testtool.web;

import nl.nn.testtool.util.LogUtil;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ApiServlet extends CXFServlet {
	Logger log = LogUtil.getLogger(this);
	// These are the beans that will be available directly under servlet context for endpoints.
	private final String[] beans = new String[]{"testTool", "reportXmlTransformer", "runStorage", "logStorage"};
	// These are the metadata for logstorage.
	// TODO: Get them programatically.
	private static final HashSet<String> metadataFields = new HashSet<String>() {{
		add("storageId");
		add("storageSize");
		add("endTime");
		add("duration");
		add("name");
		add("correlationId");
		add("status");
		add("numberOfCheckpoints");
		add("estimatedMemoryUsage");
		add("path");
	}};

	@Override
	public void init(ServletConfig sc) throws ServletException {
		System.err.println("INITING LADYBUG SERVLET FROM IAF!!!!");
		super.init(sc);

		// Make sure testtool related beans are registered as attributes for servlet context.
		// So they can be accessed from ibis-ladybug.
//		ServletContext servletContext = sc.getServletContext();
//		IbisContext context = IbisApplicationServlet.getIbisContext(servletContext);
//		servletContext.setAttribute("metadataFields", metadataFields);
//		for (String bean : beans) {
//			try {
//				log.debug("Ladybug Servlet registering bean [" + bean + "]");
//				servletContext.setAttribute(bean, context.getBean(bean));
//			} catch (NoSuchBeanDefinitionException e) {
//				log.error("No bean named [" + bean + "] available inside the Ibis Context.", e);
//			}
//		}
	}


	public static Map<String, String> getInitParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("config-location", "LadybugWebContext.xml");
		parameters.put("bus", "ladybug-api-bus");
		return parameters;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}
}
