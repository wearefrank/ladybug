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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SpecialEncodings {
	public static final String SPECIALLY_ENCODED_CLASSES_RESOURCE = "ladybug/speciallyEncodedClasses.txt";
	public static final List<String> SPECIALLY_ENCODED_CLASSES = readSpeciallyEncodedClasses();

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
}
