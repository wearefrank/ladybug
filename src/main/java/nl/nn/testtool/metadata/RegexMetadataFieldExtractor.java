 package nl.nn.testtool.metadata;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;

/**
 * @author Jaco de Groot
 */
public class RegexMetadataFieldExtractor extends DefaultValueMetadataFieldExtractor {
	protected String regex;
	protected Pattern pattern;
	protected boolean extractFromFirstCheckpointOnly = true;

	public void setRegex(String regex) {
		this.regex = regex;
		if (regex == null) {
			pattern = null;
		} else {
			pattern = Pattern.compile(regex);
		}
	}

	public void setExtractFromFirstCheckpointOnly(boolean extractFromFirstCheckpointOnly) {
		this.extractFromFirstCheckpointOnly = extractFromFirstCheckpointOnly;
	}

	public Object extractMetadata(Report report) {
		String value = null;
		Iterator iterator = report.getCheckpoints().iterator();
		while (value == null && iterator.hasNext()) {
			String message = ((Checkpoint)iterator.next()).getMessage();
			Matcher matcher = pattern.matcher(message);
			if (matcher.find()) {
				value = matcher.group(matcher.groupCount());
			}
			if (extractFromFirstCheckpointOnly) {
				break;
			}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

}
