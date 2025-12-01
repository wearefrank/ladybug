/*
   Copyright 2020, 2022-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;

/**
 * @author Peter Leeuwenburgh
 */
public class SessionKeyMetadataFieldExtractor extends
		DefaultValueMetadataFieldExtractor {
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

	/**
     * Extracts metadata from the given report by searching through its checkpoints.
     * <p>
     * The method filters checkpoints based on the session key and applies the regex pattern to extract a value
     * from the message of the first relevant checkpoint it finds.
     * If no match is found, the method returns <code>defaultValue</code>.
     *
     * @param report the report containing checkpoints to extract metadata from
     * @return the extracted metadata value or <code>defaultValue</code> if no relevant value is found
     */
	public Object extractMetadata(Report report) {
		return report.getCheckpoints().stream()
			.filter(this::isRelevantCheckpoint)
			.map(this::extractValueFromCheckpoint)
			.filter(value -> value != null)
			.findFirst()
			.orElse(defaultValue);
	}
	
	/**
     * Determines whether the given checkpoint is relevant based on the session key.
     * A checkpoint is considered relevant if its name starts with "SessionKey " followed by the session key.
     *
     * @param checkpoint the checkpoint to check
     * @return <code>true</code> if the checkpoint's name matches the session key, <code>false</code> otherwise
     */
	private boolean isRelevantCheckpoint(Checkpoint checkpoint) {
		String checkpointName = checkpoint.getName();
		return checkpointName.startsWith("SessionKey ") 
			   && checkpointName.substring("SessionKey ".length()).equals(sessionKey);
	}
	
	/**
     * Extracts the value from a checkpoint's message using the set regular expression.
     * If the message matches the pattern, the matched value is returned.
     * If no match is found, <code>null</code> is returned.
     * 
     * @param checkpoint the checkpoint from which to extract the value
     * @return the extracted value, or <code>null</code> if no match is found or if the message or pattern is <code>null</code>
     */
	private String extractValueFromCheckpoint(Checkpoint checkpoint) {
		String message = checkpoint.getMessage();
		if (message == null || pattern == null) {
			return message;
		}
		Matcher matcher = pattern.matcher(message);
		return matcher.find() ? matcher.group(matcher.groupCount()) : null;
	}
}