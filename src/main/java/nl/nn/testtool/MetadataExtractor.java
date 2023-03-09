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

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jaco de Groot
 */
public class MetadataExtractor {
	private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final int VALUE_TYPE_OBJECT = 0;
	public static final int VALUE_TYPE_STRING = 1;
	public static final int VALUE_TYPE_GUI = 2;
	private static final SimpleDateFormat FORMAT_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public List<MetadataFieldExtractor> extraMetadataFieldExtractors;
	
	public void setExtraMetadataFieldExtractors(List<MetadataFieldExtractor> extraMetadataFieldExtractors) {
		this.extraMetadataFieldExtractors = extraMetadataFieldExtractors;
	}

	public String getLabel(String metadataName) {
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
		String label = null;
		if (extraMetadataFieldExtractors != null) {
			Iterator<MetadataFieldExtractor> iterator = extraMetadataFieldExtractors.iterator();
			while (iterator.hasNext() && label == null) {
				MetadataFieldExtractor metadataFieldExtractor = iterator.next();
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
		String shortLabel = null;
		if (extraMetadataFieldExtractors != null) {
			Iterator<MetadataFieldExtractor> iterator = extraMetadataFieldExtractors.iterator();
			while (iterator.hasNext() && shortLabel == null) {
				MetadataFieldExtractor metadataFieldExtractor = iterator.next();
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
		Object metadata = null;
		if (extraMetadataFieldExtractors != null) {
			Iterator<MetadataFieldExtractor> iterator = extraMetadataFieldExtractors.iterator();
			while (iterator.hasNext() && metadata == null) {
				MetadataFieldExtractor metadataFieldExtractor = iterator.next();
				if (metadataFieldExtractor.getName().equals(metadataName)) {
					metadata = metadataFieldExtractor.extractMetadata(report);
				}
			}
		}
		return metadata;
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
		if (metadataName.equals("startTime")) {
			return FORMAT_DATE_TIME.format(metadataValue);
		}
		if (metadataName.equals("endTime")) {
			return FORMAT_DATE_TIME.format(metadataValue);
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

}
