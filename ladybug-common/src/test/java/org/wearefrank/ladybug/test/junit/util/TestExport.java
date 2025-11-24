/*
   Copyright 2021, 2023-2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.memory.MemoryStorage;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.wearefrank.ladybug.util.Export;

/**
 * @author Jaco de Groot
 */
public class TestExport {
	public static final String RESOURCE_PATH = "org/wearefrank/ladybug/test/junit/util/";

	@Test
	public void testExport() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			IOException, StorageException {
		// Find all bean properties and change default values to test that transient properties are not added to the
		// XMLEncoder xml (properties with default values will never be added to the xml by XMLEncoder)
		Report report = new Report();
		Map<String, Method> setMethods = new HashMap<>();
		getBeanProperties(report.getClass(), "set", setMethods);
		Map<String, Method> getMethods = new HashMap<>();
		getBeanProperties(report.getClass(), "get", getMethods);
		Map<String, Method> isMethods = new HashMap<>();
		getBeanProperties(report.getClass(), "is", isMethods);
		for (String name : setMethods.keySet()) {
			Method method = setMethods.get(name);
			if (getMethods.get(name) != null || isMethods.get(name) != null) {
				if (method.getParameters()[0].getType() == long.class) {
					Long defaultValue = (Long)getMethods.get(name).invoke(report, new Object[0]);
					method.invoke(report, defaultValue + name.length());
				} else if (method.getParameters()[0].getType() == Integer.class) {
					Integer defaultValue = (Integer)getMethods.get(name).invoke(report, new Object[0]);
					if (defaultValue == null) {
						method.invoke(report, name.length());
					} else {
						method.invoke(report, defaultValue + name.length());
					}
				} else if (method.getParameters()[0].getType() == boolean.class) {
					assertIsMethodAvailable(isMethods, name);
					Boolean defaultValue = (Boolean)isMethods.get(name).invoke(report, new Object[0]);
					if (defaultValue) {
						method.invoke(report, false);
					} else {
						method.invoke(report, true);
					}
				} else if (method.getParameters()[0].getType() == String.class) {
					if (name.equals("variablesCsv")) {
						method.invoke(report, name + "\n" + name);
					} else {
						method.invoke(report, name);
					}
				} else if (name.equals("storage")) {
					report.setStorage(new MemoryStorage());
				} else if (name.equals("originalReport")) {
					report.setOriginalReport(new Report());
				} else if (name.equals("reportXmlTransformer")) {
					report.setReportXmlTransformer(new ReportXmlTransformer());
				} else if (name.equals("globalReportXmlTransformer")) {
					report.setGlobalReportXmlTransformer(new ReportXmlTransformer());
				} else if (name.equals("variables")) {
					report.setVariables(new HashMap<String, String>());
				} else if (!name.equals("storageId") && !name.equals("checkpoints")) {
					// No need to test with multiple storages so CONTEXT_FILE_STORAGE will do.
					method.invoke(report, Common.CONTEXT_FILE_STORAGE.getBean(name));
				} else if (name.equals("checkpoints")) {
					// Ignore, this is done hard coded below (search for "report.setCheckpoints(checkpoints)")
				} else {
					assertNull("Method not handled: " + name);
				}
			}
			
		}
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		setMethods = new HashMap<>();
		getBeanProperties(checkpoint.getClass(), "set", setMethods);
		getMethods = new HashMap<>();
		getBeanProperties(checkpoint.getClass(), "get", getMethods);
		isMethods = new HashMap<>();
		getBeanProperties(checkpoint.getClass(), "is", isMethods);
		for (String name : setMethods.keySet()) {
			Method method = setMethods.get(name);
			if (getMethods.get(name) != null || isMethods.get(name) != null) {
				if (method.getParameters()[0].getType() == int.class) {
					Integer defaultValue = (Integer)getMethods.get(name).invoke(checkpoint, new Object[0]);
					method.invoke(checkpoint, defaultValue + name.length());
				} else if (method.getParameters()[0].getType() == boolean.class) {
					assertIsMethodAvailable(isMethods, name);
					Boolean defaultValue = (Boolean)isMethods.get(name).invoke(checkpoint, new Object[0]);
					if (defaultValue) {
						method.invoke(checkpoint, false);
					} else {
						method.invoke(checkpoint, true);
					}
				} else if (method.getParameters()[0].getType() == String.class) {
					method.invoke(checkpoint, name);
				} else if (name.equals("report")) {
					// Ignore, this is done hardcoded above (search for "checkpoint.setReport(report)")
				} else if (name.equals("messageContext")) {
					Map<String, Object> messageContext = new HashMap<String, Object>();
					messageContext.put("test", 1000);
					checkpoint.setMessageContext(messageContext);
				} else if (name.equals("span")) {
					checkpoint.setSpan(new Span());
				} else {
					assertNull("Method not handled: " + name);
				}
			}
			
		}
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		checkpoints.add(checkpoint);
		report.setCheckpoints(checkpoints);
		assertExport(RESOURCE_PATH, "test", report, false, false, false, true);
	}

	private static void getBeanProperties(Class<?> clazz, String verb, Map<String, Method> beanProperties) {
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().startsWith(verb)) {
				String name = methods[i].getName().substring(verb.length(), verb.length() + 1).toLowerCase()
						+ methods[i].getName().substring(verb.length() + 1);
				if (clazz.equals(Checkpoint.class) && verb.equals("set") && name.equals("message")
						&& methods[i].getParameters()[0].getType() == Object.class) {
					// Ignore Checkpoint.setMessage(Object), use Checkpoint.setMessage(String)
				} else if (clazz.equals(Checkpoint.class) && verb.equals("get") && name.equals("typeAsString")
						&& methods[i].getParameters().length > 0) {
					// Ignore Checkpoint.getTypeAsString(int)
				} else if (clazz.equals(Checkpoint.class) && verb.equals("get") && name.equals("messageAsObject")
						&& methods[i].getParameters().length > 0) {
					// Ignore Checkpoint.getMessageAsObject(T)
				} else if (clazz.equals(Report.class) && verb.equals("get") && name.equals("checkpoint")
						&& methods[i].getParameters().length > 0) {
					// Ignore all Report.getCheckpoint() methods (which wouldn't pass the assertNull below)
				} else {
					assertNull("More than one " + verb + " " + name + " method found on " + clazz,
							beanProperties.get(name));
					beanProperties.put(name, methods[i]);
				}
			}
		}
	}

	private static void assertIsMethodAvailable(Map<String, Method> isMethods, String name) {
		assertNotNull(isMethods.get(name), "No is method available for " + name + " (maybe a get instead of is method was used)");
	}

	// TODO: We have too many boolean arguments, not clear. We can make a new class, say XmlComparator,
	// that has setters for all the ignores that we need. Then objects of that class can perform the
	// comparing.
	public static void assertExport(String resourcePath, String testCaseName, Report report,
			boolean applyToXmlIgnores, boolean applyEpochTimestampIgnores, boolean applyStackTraceIgnores,
			boolean ignoreStorageId)
			throws IOException, StorageException {
		byte[] bytes = Export.getReportBytesPortable(report);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
		StringBuilder builder = new StringBuilder();
		int i = gzipInputStream.read();
		while (i != -1) {
			builder.append((char)i);
			i = gzipInputStream.read();
		}
		String actual = builder.toString();
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		if (ignoreStorageId) {
			actual = ReportRelatedTestCase.ignoreStorageId(actual, report);
		}
		if (applyToXmlIgnores) {
			actual = ReportRelatedTestCase.applyToXmlIgnores(actual, report);
		}
		if (applyEpochTimestampIgnores) {
			actual = ReportRelatedTestCase.applyEpochTimestampIgnores(actual);
		}
		if (applyStackTraceIgnores) {
			actual = ReportRelatedTestCase.applyStackTraceIgnores(actual);
		}
		ReportRelatedTestCase.assertXml(resourcePath, testCaseName + "Export", actual);
	}

}
