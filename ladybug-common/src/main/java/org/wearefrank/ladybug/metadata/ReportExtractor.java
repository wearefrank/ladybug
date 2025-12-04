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
package org.wearefrank.ladybug.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MetadataFieldExtractor;
import org.wearefrank.ladybug.Report;

import lombok.Getter;
import lombok.Setter;

class ReportExtractor implements MetadataFieldExtractor {
	protected @Setter @Getter String name;
	protected @Setter @Getter String label;
	protected @Setter @Getter String shortLabel;
	protected @Setter @Getter String defaultValue;
	protected @Setter @Getter String checkpointName;
	protected @Setter @Getter List<ExtractionStrategy> extractionStrategies = new ArrayList<>() {
	};
	protected @Setter @Getter boolean isSessionKey;
	protected @Setter @Getter String SESSION_KEY_PREFIX = "SessionKey ";

	@Override
	public Object extractMetadata(Report report) {
		return report.getCheckpoints().stream()
				.filter(this::isRelevantCheckpoint)
				.map(this::extractValueFromCheckpoint)
				.findFirst()
				.flatMap(Function.identity())
				.orElse(defaultValue);
	}

	private Boolean isRelevantCheckpoint(Checkpoint checkpoint) {
		final String checkpointName = isSessionKey
				? checkpoint.getName().substring(SESSION_KEY_PREFIX.length())
				: checkpoint.getName();

		return checkpointName.equals(this.checkpointName);
	}

	public Optional<String> extractValueFromCheckpoint(Checkpoint checkpoint) {
		return Optional.ofNullable(checkpoint.getMessage()) // Start with the message
				.flatMap(message -> combineStrategies().apply(Optional.of(message))); // Apply processing
	}

	public Function<Optional<String>, Optional<String>> combineStrategies() {
		return extractionStrategies.stream()
				.map(strategy -> (Function<Optional<String>, Optional<String>>) msg ->
						msg.flatMap(strategy::extract)
				)
				.reduce(Function.identity(), Function::andThen);
	}
}
