package nl.nn.testtool.metadata;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;


import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

class ReportExtractor implements MetadataFieldExtractor {
    protected @Setter @Getter String name;
    protected @Setter @Getter String label;
    protected @Setter @Getter String shortLabel;
    protected @Setter @Getter String defaultValue;
    protected @Setter @Getter String checkpointName;
    protected @Setter @Getter List<ExtractionStrategy> extractionStrategies = new ArrayList<ExtractionStrategy>() {};
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
