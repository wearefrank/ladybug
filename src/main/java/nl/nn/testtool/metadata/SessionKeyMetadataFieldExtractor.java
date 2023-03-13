/*
   Copyright 2020, 2022, 2023 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.metadata;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;

/**
 * @author Peter Leeuwenburgh
 */
public class SessionKeyMetadataFieldExtractor extends
		DefaultValueMetadataFieldExtractor {
	private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected String sessionKey;
	protected String regex;
	protected Pattern pattern;

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public void setRegex(String regex) {
		this.regex = regex;
		if (regex == null) {
			pattern = null;
		} else {
			pattern = Pattern.compile(regex);
		}
	}

	public Object extractMetadata(Report report) {
		String value = null;
		Iterator iterator = report.getCheckpoints().iterator();
		while (value == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint) iterator.next();
			String checkpointName = checkpoint.getName();
			if (checkpointName.startsWith("SessionKey ")) {
				String sessionKeyName = checkpointName.substring("SessionKey "
						.length());
				if (sessionKeyName.equals(sessionKey)) {
					value = checkpoint.getMessage();
					if((value != null) && (pattern != null)) {
						Matcher matcher = pattern.matcher(value);
						if (matcher.find()) {
							value = matcher.group(matcher.groupCount());
						}
					}
				}
			}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

}
