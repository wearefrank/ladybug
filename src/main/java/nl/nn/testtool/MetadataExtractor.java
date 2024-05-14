/*
   Copyright 2020, 2022-2023 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

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
	private static final DateTimeFormatter FORMAT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
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
		if (metadataName.equals("correlationId")) {
			return report.getCorrelationId();
		}
		if (metadataName.equals("startTime")) {
			return new Long(report.getStartTime());
		}
		if (metadataName.equals("endTime")) {
			return new Long(report.getEndTime());
		}
		if (metadataName.equals("estimatedMemoryUsage")) {
			return new Long(report.getEstimatedMemoryUsage());
		}
		if (metadataName.equals("numberOfCheckpoints")) {
			return new Integer(report.getNumberOfCheckpoints());
		}
		if (metadataName.equals("duration")) {
			return new Long(report.getEndTime() - report.getStartTime());
		}
		if (metadataName.equals("variableCsv")) {
			return report.getVariableCsv();
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
		return "" + metadataValue;
	}

	public Object fromObjectToGUI(String metadataName, Object metadataValue) {
		if (metadataName.equals("startTime") || metadataName.equals("endTime")) {
			return FORMAT_DATE_TIME.format(Instant.ofEpochMilli((Long)metadataValue));
		}
		return metadataValue;
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
			return new Integer(metadataValue);
		}
		if (metadataName.equals("storageSize")) {
			return new Long(metadataValue);
		}
		if (metadataName.equals("startTime")) {
			return new Long(metadataValue);
		}
		if (metadataName.equals("endTime")) {
			return new Long(metadataValue);
		}
		if (metadataName.equals("estimatedMemoryUsage")) {
			return new Long(metadataValue);
		}
		if (metadataName.equals("numberOfCheckpoints")) {
			return new Integer(metadataValue);
		}
		return metadataValue;
	}

	public boolean isInteger(String metadataName) {
		return metadataName.equals("storageId") || metadataName.equals("numberOfCheckpoints");
	}

	public boolean isLong(String metadataName) {
		return metadataName.equals("duration") || metadataName.equals("estimatedMemoryUsage") || metadataName.equals("storageSize");
	}

	public boolean isTimestamp(String metadataName) {
		return metadataName.equals("endTime") || metadataName.equals("startTime");
	}

	public String getType(String metadataName) {
		if(this.isInteger(metadataName)) {
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
