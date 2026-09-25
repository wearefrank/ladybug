/*
   Copyright 2026 WeAreFrank!

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
package org.wearefrank.ladybug.util;

import lombok.extern.slf4j.Slf4j;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class SpecialEncodings {
	private SpecialEncodings() {}

	public static final String SPECIALLY_ENCODED_CLASSES_RESOURCE = "ladybug/speciallyEncodedClasses.txt";
	// Maps each class name (first column) to the name of the static method that creates an instance
	// from the result of toString() (second column). The second column is "new" to use the constructor.
	private static final Map<String, String> FACTORY_METHODS = readSpeciallyEncodedClasses();
	public static final List<String> SPECIALLY_ENCODED_CLASSES = List.copyOf(FACTORY_METHODS.keySet());
	private static final String ENCODER_SUFFIX = ".toString()";
	private static final String CONSTRUCTOR = "new";

	private static Map<String, String> readSpeciallyEncodedClasses() {
		try (InputStream in = SpecialEncodings.class.getClassLoader().getResourceAsStream(SPECIALLY_ENCODED_CLASSES_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Resource not found: " + SPECIALLY_ENCODED_CLASSES_RESOURCE);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			Map<String, String> result = new LinkedHashMap<>();
			reader.lines()
					.map(String::trim)
					.filter(line -> !line.isEmpty())
					.forEach(line -> {
						String[] columns = line.split(",");
						if (columns.length != 2 || columns[0].isBlank() || columns[1].isBlank()) {
							throw new IllegalStateException(String.format(
									"Expected [className,factoryMethod] in resource %s, got [%s]", SPECIALLY_ENCODED_CLASSES_RESOURCE, line));
						}
						result.put(columns[0].trim(), columns[1].trim());
					});
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read resource " + SPECIALLY_ENCODED_CLASSES_RESOURCE, e);
		}
	}

	public static String getSpecialEncoderIfApplicable(String className) {
		if (SPECIALLY_ENCODED_CLASSES.contains(className)) {
			return className + ENCODER_SUFFIX;
		} else {
			return null;
		}
	}

	public static String getClassOfEncoderIfApplicable(String encoder) {
		if (encoder.endsWith(ENCODER_SUFFIX)) {
			String candidateClass = encoder.substring(0, encoder.length() - ENCODER_SUFFIX.length());
			if (SPECIALLY_ENCODED_CLASSES.contains(candidateClass)) {
				return candidateClass;
			}
		}
		return null;
	}

	/**
	 * Returns the second column of speciallyEncodedClasses.txt for the given class, the name of the
	 * static method that creates an instance or "new" for the constructor. Null for other classes.
	 */
	public static String getFactoryName(String className) {
		return FACTORY_METHODS.get(className);
	}

	/**
	 * Creates an instance of a specially encoded class from the result of its toString(), using
	 * the static factory method or constructor configured in speciallyEncodedClasses.txt.
	 */
	public static Object instantiate(Class<?> clazz, String value) throws ReflectiveOperationException {
		Executable factory = getFactory(clazz);
		if (factory instanceof Constructor<?> constructor) {
			return constructor.newInstance(value);
		}
		return ((Method) factory).invoke(null, value);
	}

	// Finds the configured public constructor (when the second column is "new") or public static
	// method that takes one argument to which a String can be passed. For example
	// java.time.Instant.parse() takes a CharSequence.
	private static Executable getFactory(Class<?> clazz) throws NoSuchMethodException {
		String methodName = FACTORY_METHODS.get(clazz.getName());
		if (CONSTRUCTOR.equals(methodName)) {
			for (Constructor<?> constructor: clazz.getConstructors()) {
				if (acceptsString(constructor)) {
					return constructor;
				}
			}
		} else if (methodName != null) {
			for (Method method: clazz.getMethods()) {
				if (method.getName().equals(methodName)
						&& Modifier.isStatic(method.getModifiers())
						&& acceptsString(method)
						&& clazz.isAssignableFrom(method.getReturnType())) {
					return method;
				}
			}
		}
		throw new NoSuchMethodException(String.format(
				"No public static method or constructor [%s] with a String argument that returns a [%s]", methodName, clazz.getName()));
	}

	private static boolean acceptsString(Executable executable) {
		return executable.getParameterCount() == 1 && executable.getParameterTypes()[0].isAssignableFrom(String.class);
	}

	// org.springframework.http.MediaType and the other classes of speciallyEncodedClasses.txt
	// have a caveat. They do not have a public no-arg constructor. Therefore they need special
	// care for serialization using XMLEncoder. While serializing, XMLEncoder reconstructs
	// objects. This fails by default because a no-arg public constructor is expected.
	// This function tells XMLEncoder to reconstruct the objects of said classes using the
	// static method or constructor of the second column of speciallyEncodedClasses.txt, for example
	// valueOf(), parse() or new. This method is referenced in the serialization result, that
	// is, the XML output. Therefore, no special treatment is needed for de-serialization
	// that would complement what we do for serialization.
	//
	// There is another issue with de-serialization. This project uses a modified version
	// of XMLDecoder that only accepts a list of safe classes. Otherwise, malicious code could
	// be injected by uploading Ladybug reports. The classes of speciallyEncodedClasses.txt
	// are automatically added as safe classes that can only be instantiated, see class SafeClasses.
	public static void registerExportDelegates(XMLEncoder xmlEncoder) {
		for (String className: SPECIALLY_ENCODED_CLASSES) {
			try {
				Class<?> clazz = Class.forName(className);
				// Validates the configuration. XMLEncoder understands "new" as calling a constructor
				getFactory(clazz);
				String methodName = FACTORY_METHODS.get(className);
				xmlEncoder.setPersistenceDelegate(clazz, new PersistenceDelegate() {
					@Override
					protected Expression instantiate(Object oldInstance, Encoder out) {
						return new Expression(oldInstance, clazz, methodName, new Object[]{oldInstance.toString()});
					}
				});
			} catch (ClassNotFoundException e) {
				// The unit tests use a stub that is available in src/test/java.
				// When this exception occurs it is not an error. Ladybug may be used
				// in environments where some of the specially encoded classes are
				// not on the classpath.
				log.warn("Cannot encode or decode class [{}] - only OK if this class does not play a role in the instrumented application", className);
			} catch (NoSuchMethodException e) {
				log.error("Cannot encode or decode class [{}], check speciallyEncodedClasses.txt", className, e);
			}
		}
	}
}
