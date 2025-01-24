/*
   Copyright 2020, 2022-2025 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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
package nl.nn.testtool;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.metadata.StatusMetadataFieldExtractor;

/**
 * @author Jaco de Groot
 */
public class MetadataExtractor {
	public static final int VALUE_TYPE_OBJECT = 0;
	public static final int VALUE_TYPE_STRING = 1;
	public static final int VALUE_TYPE_GUI = 2;
	public static final String DATE_TIME_PATTERN;
	public static final String DATE_TIME_RANGE_START_SUFFIX;
	public static final String DATE_TIME_RANGE_END_SUFFIX;
	public static final String[] DATE_TIME_RANGE_END_SPECIALS = new String[4];
	static {
		DATE_TIME_PATTERN               = "yyyy-MM-dd HH:mm:ss.SSS";
		DATE_TIME_RANGE_START_SUFFIX    = "0000-00-00 00:00:00.000";
		DATE_TIME_RANGE_END_SUFFIX      = "9999-12-31 23:59:59.999";
		DATE_TIME_RANGE_END_SPECIALS[0] = "     0";
		DATE_TIME_RANGE_END_SPECIALS[1] = "        1";
		DATE_TIME_RANGE_END_SPECIALS[2] = "        2";
		DATE_TIME_RANGE_END_SPECIALS[3] = "           1";
		// Examples of special cases:
		//   2024-0 should not become 2024-02 but 2024-09
		//   2024-10-1 should not become 2024-10-11 but 2024-10-19
		//   2024-10-2 should not become 2024-10-21 but 2024-10-29
		//   2024-10-25 1 should not become 2024-10-25 13 but 2024-10-25 19
	}
	public static final String DATE_TIME_RANGE_END_SPECIAL2 = "        3";
	public static final String DATE_TIME_RANGE_END_SPECIAL3 = "           2";
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.systemDefault());
	private List<MetadataFieldExtractor> metadataFieldExtractors;
	/**
	 * Add extra fields or override standard fields (e.g. replace standard status field with application specific status
	 * field)
	 */
	public @Setter @Getter List<MetadataFieldExtractor> extraMetadataFieldExtractors;

	@PostConstruct
	public void init() {
		metadataFieldExtractors = new ArrayList<MetadataFieldExtractor>();
		metadataFieldExtractors.add(new StatusMetadataFieldExtractor());
	}

	public String getLabel(String metadataName) {
		String label = null;
		if (extraMetadataFieldExtractors != null) {
			for (MetadataFieldExtractor metadataFieldExtractor : extraMetadataFieldExtractors) {
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					label = metadataFieldExtractor.getLabel();
				}
			}
		}
		if (label == null) {
			// TODO use reflection or spring or something like that for getLabel and getShortLabel?
			if (metadataName.equals("storageId")) {
				return "Storage Id";
			}
			if (metadataName.equals("storageSize")) {
				return "Storage size";
			}
			if (metadataName.equals("name")) {
				return "Name";
			}
			if (metadataName.equals("description")) {
				return "Description";
			}
			if (metadataName.equals("path")) {
				return "Path";
			}
			if (metadataName.equals("fullpath")) {
				return "Full path";
			}
			if (metadataName.equals("correlationId")) {
				return "Correlation Id";
			}
			if (metadataName.equals("startTime")) {
				return "Start time";
			}
			if (metadataName.equals("endTime")) {
				return "End time";
			}
			if (metadataName.equals("estimatedMemoryUsage")) {
				return "Estimated memory usage";
			}
			if (metadataName.equals("numberOfCheckpoints")) {
				return "Number of checkpoints";
			}
			if (metadataName.equals("duration")) {
				return "Duration";
			}
			for (MetadataFieldExtractor metadataFieldExtractor : metadataFieldExtractors) {
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					label = metadataFieldExtractor.getLabel();
				}
			}
		}
		if (label == null) {
			label = metadataName;
		}
		return label;
	}

	public String getShortLabel(String metadataName) {
		String shortLabel = null;
		if (extraMetadataFieldExtractors != null) {
			for (MetadataFieldExtractor metadataFieldExtractor : extraMetadataFieldExtractors) {
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					shortLabel = metadataFieldExtractor.getShortLabel();
				}
			}
		}
		if (shortLabel == null) {
			if (metadataName.equals("storageId")) {
				return "StorageId";
			}
			if (metadataName.equals("storageSize")) {
				return "StorageSize";
			}
			if (metadataName.equals("name")) {
				return "Name";
			}
			if (metadataName.equals("description")) {
				return "Description";
			}
			if (metadataName.equals("path")) {
				return "Path";
			}
			if (metadataName.equals("fullpath")) {
				return "FullPath";
			}
			if (metadataName.equals("correlationId")) {
				return "CorrelationId";
			}
			if (metadataName.equals("startTime")) {
				return "StartTime";
			}
			if (metadataName.equals("endTime")) {
				return "EndTime";
			}
			if (metadataName.equals("estimatedMemoryUsage")) {
				return "EstMemUsage";
			}
			if (metadataName.equals("numberOfCheckpoints")) {
				return "NrChpts";
			}
			if (metadataName.equals("duration")) {
				return "Duration";
			}
			for (MetadataFieldExtractor metadataFieldExtractor : metadataFieldExtractors) {
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					shortLabel = metadataFieldExtractor.getShortLabel();
				}
			}
		}
		if (shortLabel == null) {
			shortLabel = getLabel(metadataName);
		}
		return shortLabel;
	}

	public Object getMetadata(Report report, String metadataName, int metadataValueType) {
		return fromObjectToMetadataValueType(metadataName, getMetadataAsObject(report, metadataName), metadataValueType);
	}

	private Object getMetadataAsObject(Report report, String metadataName) {
		if (extraMetadataFieldExtractors != null) {
			for (MetadataFieldExtractor metadataFieldExtractor : extraMetadataFieldExtractors) {
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					return metadataFieldExtractor.extractMetadata(report);
				}
			}
		}
		// TODO use reflection or spring or something like that?
		if (metadataName.equals("storageId")) {
			return report.getStorageId();
		}
		if (metadataName.equals("storageSize")) {
			return report.getStorageSize();
		}
		if (metadataName.equals("name")) {
			return report.getName();
		}
		if (metadataName.equals("description")) {
			return report.getDescription();
		}
		if (metadataName.equals("path")) {
			return report.getPath();
		}
		if (metadataName.equals("fullpath")) {
			return report.getFullPath();
		}
		if (metadataName.equals("correlationId")) {
			return report.getCorrelationId();
		}
		if (metadataName.equals("startTime")) {
			return Long.valueOf(report.getStartTime());
		}
		if (metadataName.equals("endTime")) {
			return Long.valueOf(report.getEndTime());
		}
		if (metadataName.equals("estimatedMemoryUsage")) {
			return Long.valueOf(report.getEstimatedMemoryUsage());
		}
		if (metadataName.equals("numberOfCheckpoints")) {
			return Integer.valueOf(report.getNumberOfCheckpoints());
		}
		if (metadataName.equals("duration")) {
			return Long.valueOf(report.getEndTime() - report.getStartTime());
		}
		if (metadataName.equals("variableCsv")) {
			return report.getVariableCsv();
		}
		if (metadataName.equals("variableMap")) {
			return report.getVariablesAsMap();
		}
		for (MetadataFieldExtractor metadataFieldExtractor : metadataFieldExtractors) {
			if (metadataFieldExtractor.getName().equals(metadataName)) {
				return metadataFieldExtractor.extractMetadata(report);
			}
		}
		return null;
	}

	public Object fromObjectToMetadataValueType(String metadataName, Object metadataValue, int metadataValueType) {
		if (metadataValueType == VALUE_TYPE_STRING) {
			metadataValue = fromObjectToString(metadataName, metadataValue);
		} else if (metadataValueType == VALUE_TYPE_GUI) {
			metadataValue = fromObjectToGUI(metadataName, metadataValue);
		}
		return metadataValue;
	}

	public String fromObjectToString(String metadataName, Object metadataValue) {
		if (metadataValue == null) {
			return null;
		} else {
			return metadataValue.toString();
		}
	}

	public Object fromObjectToGUI(String metadataName, Object metadataValue) {
		if (metadataName.equals("startTime") || metadataName.equals("endTime")) {
			return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli((Long)metadataValue));
		}
		return fromObjectToString(metadataName, metadataValue);
	}

	public Object fromStringToMetadataValueType(String metadataName, String metadataValue, int metadataValueType) {
		Object value = metadataValue;
		if (metadataValueType == VALUE_TYPE_OBJECT) {
			value = fromStringtoObject(metadataName, metadataValue);
		} else if (metadataValueType == VALUE_TYPE_GUI) {
			value = fromStringtoObject(metadataName, metadataValue);
			value = fromObjectToGUI(metadataName, value);
		}
		return value;
	}

	public Object fromStringtoObject(String metadataName, String metadataValue) {
		if (metadataName.equals("storageId")) {
			return Integer.valueOf(metadataValue);
		}
		if (metadataName.equals("storageSize")) {
			return Long.valueOf(metadataValue);
		}
		if (metadataName.equals("startTime")) {
			return Long.valueOf(metadataValue);
		}
		if (metadataName.equals("endTime")) {
			return Long.valueOf(metadataValue);
		}
		if (metadataName.equals("estimatedMemoryUsage")) {
			return Long.valueOf(metadataValue);
		}
		if (metadataName.equals("numberOfCheckpoints")) {
			return Integer.valueOf(metadataValue);
		}
		return metadataValue;
	}

	public Object fromGUIToObject(String metadataName, String metadataValue) {
		if (metadataName.equals("startTime") || metadataName.equals("endTime")) {
			return LocalDateTime.parse(metadataValue, DATE_TIME_FORMATTER).atZone(
					ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
		return fromStringtoObject(metadataName, metadataValue);
	}

	public boolean isInteger(String metadataName) {
		return metadataName.equals("storageId") || metadataName.equals("numberOfCheckpoints");
	}

	public boolean isLong(String metadataName) {
		return metadataName.equals("duration") || metadataName.equals("estimatedMemoryUsage")
				|| metadataName.equals("storageSize");
	}

	public boolean isTimestamp(String metadataName) {
		return metadataName.equals("endTime") || metadataName.equals("startTime");
	}

	public String getType(String metadataName) {
		if (this.isInteger(metadataName)) {
			return "int";
		}
		if (this.isLong(metadataName)) {
			return "long";
		}
		if (this.isTimestamp(metadataName)) {
			return "timestamp";
		}
		return "string";
	}
}
