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
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public final class SpecialEncodings {
	private SpecialEncodings() {}

	public static final String SPECIALLY_ENCODED_CLASSES_RESOURCE = "ladybug/speciallyEncodedClasses.txt";
	public static final List<String> SPECIALLY_ENCODED_CLASSES = readSpeciallyEncodedClasses();
	private static final String ENCODER_SUFFIX = ".toString()";

	private static List<String> readSpeciallyEncodedClasses() {
		try (InputStream in = SpecialEncodings.class.getClassLoader().getResourceAsStream(SPECIALLY_ENCODED_CLASSES_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Resource not found: " + SPECIALLY_ENCODED_CLASSES_RESOURCE);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			return reader.lines()
					.map(String::trim)
					.filter(line -> !line.isEmpty())
					.toList();
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

	// org.springframework.http.MediaType and the other classes of speciallyEncodedClasses.txt
	// have a caveat. They do not have a public no-arg constructor. Therefore they need special
	// care for serialization using XMLEncoder. While serializing, XMLEncoder reconstructs
	// objects. This fails by default because a no-arg public constructor is expected.
	// This function tells XMLEncoder to reconstruct the objects of said classes using
	// static method valueOf(). This method is referenced in the serialization result, that
	// is, the XML output. Therefore, no special treatment is needed for de-serialization
	// that would complement what we do for serialization.
	//
	// There is another issue with de-serialization. This project uses a modified version
	// of XMLDecoder that only accepts a list of safe classes. Otherwise, malicious code could
	// be injected by uploading Ladybug reports. The classes of speciallyEncodedClasses.txt
	// are automatically added as safe classes, see field DocumentHandler.SAFE_CLASSES.
	public static void registerExportDelegates(XMLEncoder xmlEncoder) {
		for (String className: SPECIALLY_ENCODED_CLASSES) {
			try {
				Class<?> clazz = Class.forName(className);
				xmlEncoder.setPersistenceDelegate(clazz, new PersistenceDelegate() {
					@Override
					protected Expression instantiate(Object oldInstance, Encoder out) {
						return new Expression(oldInstance, clazz, "valueOf", new Object[]{oldInstance.toString()});
					}
				});
			} catch (ClassNotFoundException e) {
				// The unit tests use a stub that is available in src/test/java.
				// When this exception occurs it is not an error. Ladybug may be used
				// in environments where some of the specially encoded classes are
				// not on the classpath.
				log.warn("Cannot encode or decode class [{}] - only OK if this class does not play a role in the instrumented application", className);
			}
		}
	}
}
