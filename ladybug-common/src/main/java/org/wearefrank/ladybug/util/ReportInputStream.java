/*
   Copyright 2025 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;

import org.wearefrank.ladybug.xmldecoder.DocumentHandler;

public class ReportInputStream extends ObjectInputStream {

	public ReportInputStream(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass classDescriptor = super.readClassDescriptor();
		String name = classDescriptor.getName();
		if (DocumentHandler.OLD_REPORT_REPLACEMENT_CLASSES.containsKey(name)) {
			name = DocumentHandler.OLD_REPORT_REPLACEMENT_CLASSES.get(name);
			// https://stackoverflow.com/questions/10936625/using-readclassdescriptor-and-maybe-resolveclass-to-permit-serialization-ver/14608062#14608062
			try {
				Field f = classDescriptor.getClass().getDeclaredField("name");
				f.setAccessible(true);
				f.set(classDescriptor, name);
			} catch (NoSuchFieldException e) {
				throw new IOException(e);
			} catch (IllegalAccessException e) {
				throw new IOException(e);
			}
		}
		return classDescriptor;
	}
}
