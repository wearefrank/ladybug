package nl.nn.testtool.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 */
public class LogUtil {
	public static final String DEFAULT_TEST_TOOL_PROPERTIES_RESOURCE_NAME = "testtool.properties";
	public static final String DEBUG_LOG_PREFIX = "Test Tool LogUtil class ";
	public static final String DEBUG_LOG_SUFFIX = "";
	public static final String WARN_LOG_PREFIX = DEBUG_LOG_PREFIX;
	public static final String WARN_LOG_SUFFIX = ", leaving it up to log4j's default initialization procedure: http://logging.apache.org/log4j/docs/manual.html#defaultInit";
	private static LoggerProvider loggerProvider;
	static {
		String testToolPropertiesResourceName = System.getProperty(DEFAULT_TEST_TOOL_PROPERTIES_RESOURCE_NAME);
		if (testToolPropertiesResourceName == null) {
			testToolPropertiesResourceName = DEFAULT_TEST_TOOL_PROPERTIES_RESOURCE_NAME;
		}
		Properties properties = getProperties(DEBUG_LOG_PREFIX, DEBUG_LOG_SUFFIX, WARN_LOG_PREFIX, WARN_LOG_SUFFIX, testToolPropertiesResourceName);
		if (properties != null) {
			String loggerProviderClass = properties.getProperty("LoggerProvider.class");
			if (loggerProviderClass == null) {
				System.out.println(WARN_LOG_PREFIX + "did not find LoggerProvider.class property" + WARN_LOG_SUFFIX);
			} else {
				try {
					loggerProvider = (LoggerProvider)LogUtil.class.getClassLoader().loadClass(loggerProviderClass).newInstance();
				} catch(ClassNotFoundException e) {
					System.out.println(WARN_LOG_PREFIX + "got ClassNotFoundException (" + e.getMessage() + ")" + WARN_LOG_SUFFIX);
				} catch (InstantiationException e) {
					System.out.println(WARN_LOG_PREFIX + "got InstantiationException (" + e.getMessage() + ")" + WARN_LOG_SUFFIX);
				} catch (IllegalAccessException e) {
					System.out.println(WARN_LOG_PREFIX + "got IllegalAccessException (" + e.getMessage() + ")" + WARN_LOG_SUFFIX);
				}
			}
		}
	}
	
	public static Logger getLogger(String name) { 
		Logger logger = null;
		if (loggerProvider == null) {
			logger = Logger.getLogger(name);
		} else {
			logger = loggerProvider.getLogger(name);
		}
		return logger;
	}

	public static Logger getLogger(Class clazz) { 
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object object) { 
		return getLogger(object.getClass());
	}

	public static Properties getProperties(String debugLogPrefix,
			String debugLogSuffix, String warnLogPrefix, String warnLogSuffix,
			String resourceName) {
		Properties properties = null;
		URL url = LogUtil.class.getClassLoader().getResource(resourceName);
		if (url == null) {
			System.out.println(warnLogPrefix + "did not find " + resourceName + warnLogSuffix);
		} else {
			properties = getProperties(debugLogPrefix, debugLogSuffix, warnLogPrefix, warnLogSuffix, url);
		}
		return properties;
	}

	public static Properties getProperties(String debugLogPrefix,
			String debugLogSuffix, String warnLogPrefix, String warnLogSuffix,
			URL url) {
		Properties properties = new Properties();
		try {
			properties.load(url.openStream());
			if (System.getProperty("log4j.debug") != null) {
				System.out.println(debugLogPrefix + "loaded properties from " + url.toString() + debugLogSuffix);
			}
		} catch (IOException e) {
			properties = null;
			System.out.println(warnLogPrefix + "could not read " + url + " (" + e.getClass().getName() + ": " + e.getMessage() + ")" + warnLogSuffix);
		}
		return properties;
	}

}
