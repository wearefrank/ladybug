package nl.nn.testtool.web;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FrontendServlet extends HttpServlet {

	private static final long serialVersionUID = 123L;
	private Map<String, MediaType> mediaTypeMap;
	private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			populateMediaTypeMap();
		} catch (IOException e) {
			throw new ServletException("unable to parse MediaType mapping file", e);
		}
	}

	private void populateMediaTypeMap() throws IOException {
		mediaTypeMap = new HashMap<>();
		Properties properties = new Properties();
		URL mappingFile = this.getClass().getResource("/mediaType.mapping");
		if (mappingFile == null) {
			throw new IOException("unable to open mediaType mapping file");
		}

		properties.load(mappingFile.openStream());
		for (String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			mediaTypeMap.put(key, MediaType.valueOf(value));
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if(path == null) {
			path = "/";
		}
		if (path.equals("/")) {
			path += "index.html";
			resp.sendRedirect(path);
			return;
		}

		URL resource = this.getClass().getResource("/webapp/app" + path);
		if (resource == null) {
			resp.sendError(404, "file not found");
			return;
		}

		MediaType mimeType = determineMimeType(path);
		if (mimeType != null) {
			resp.setContentType(mimeType.toString());
		}

		try (InputStream in = resource.openStream()) {
			IOUtils.copy(in, resp.getOutputStream());
		} catch (IOException e) {
			resp.sendError(500, e.getMessage());
			return;
		}

		resp.flushBuffer();
	}

	private MediaType determineMimeType(String path) {
		int i = path.lastIndexOf(".");
		String extension = path.substring(i + 1); //Get the extension
		int p = extension.indexOf("?");
		if (p > -1) {
			extension = extension.substring(0, p); //String all parameters
		}

		log.debug("extruded extension [" + extension + "] from path [" + path + "]");
		return findMediaType(extension);
	}

	private MediaType findMediaType(String extension) {
		log.debug("trying to find MimeType for extension [" + extension + "]");

		MediaType type = mediaTypeMap.get(extension);
		if (type == null) {
			log.warn("unable to find MimeType for extension [" + extension + "] using default [application/octet-stream]");
			type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
		} else {
			log.info("determined MimeType [" + type + "] for extension [" + extension + "]");
		}
		return type;
	}
}
