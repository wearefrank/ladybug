package nl.nn.testtool.storage.xml;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Metadata {
	protected int storageId, nrChpts;
	protected long memoryUsage, storageSize, duration, endTime, lastModified;
	protected String name, correlationId, status, path, description;

	public Metadata(int storageId, long duration, int nrChpts, long memoryUsage, long storageSize, long endTime, String name, String correlationId, String status, String path, String description, long lastModified) {
		this.storageId = storageId;
		this.duration = duration;
		this.nrChpts = nrChpts;
		this.memoryUsage = memoryUsage;
		this.storageSize = storageSize;
		this.endTime = endTime;
		this.name = name;
		this.correlationId = correlationId;
		this.status = status;
		this.path = path;
		this.description = description;
		this.lastModified = lastModified;
	}

	/**
	 * Returns a list of field values, with the given order.
	 *
	 * @param names Order of fields to be returned.
	 * @param type  Type of objects to be returned.
	 * @return A list of values.
	 */
	public List<Object> toObjectList(List<String> names, int type) {
		List<Object> list = new ArrayList<>(names.size());
		for (String n : names) {
			if (type == MetadataExtractor.VALUE_TYPE_STRING) {
				list.add(String.valueOf(getFieldValue(n)));
			} else {
				list.add(getFieldValue(n));
			}
		}

		return list;
	}

	/**
	 * Translates the metadata to xml format.
	 *
	 * @return XML string.
	 */
	public String toXml() {
		String template = "<Metadata>\n" +
				"    <StorageId>%d</StorageId>\n" +
				"    <EndTime>%d</EndTime>\n" +
				"    <Duration>%d</Duration>\n" +
				"    <Name>%s</Name>\n" +
				"    <CorrelationId>%s</CorrelationId>\n" +
				"    <Status>%s</Status>\n" +
				"    <NrChpts>%d</NrChpts>\n" +
				"    <EstMemUsage>%d</EstMemUsage>\n" +
				"    <StroageSize>%d</StroageSize>\n" +
				"    <Path>%s</Path>\n" +
				"    <LastModified>%d</LastModified>\n" +
				"    <Description>%s</Description>\n" +
				"</Metadata>\n";
		return String.format(template, storageId, endTime, duration, StringEscapeUtils.escapeXml(name), StringEscapeUtils.escapeXml(correlationId), StringEscapeUtils.escapeXml(status), nrChpts, memoryUsage, storageSize, StringEscapeUtils.escapeXml(path), lastModified, StringEscapeUtils.escapeXml(description));
	}

	/**
	 * Creates a metadata object from the given report and storage id.
	 *
	 * @param report    Report to be used for creating a metadata.
	 * @param storageId Storage id for metadata.
	 * @return Metadata object that is created from the given report.
	 */
	public static Metadata fromReport(Report report, int storageId, long lastModified) {
		return new Metadata(
				storageId,
				report.getEndTime() - report.getStartTime(),
				report.getNumberOfCheckpoints(),
				report.getEstimatedMemoryUsage(),
				report.getStorageSize() != null ? report.getStorageSize() : 0L,
				report.getEndTime(),
				report.getName(),
				report.getCorrelationId(),
				"Success",
				report.getPath(),
				report.getDescription(),
				lastModified);
	}

	public static Metadata fromXml(String xml) {
		xml = extractTagValue(xml, "Metadata");
		long storageId = safeParse(extractTagValue(xml, "StorageId"), -1);
		long duration = safeParse(extractTagValue(xml, "Duration"), -1);
		long nrChpts = safeParse(extractTagValue(xml, "NrChpts"), -1);
		long estMemUsage = safeParse(extractTagValue(xml, "EstMemUsage"), -1);
		long stroageSize = safeParse(extractTagValue(xml, "StroageSize"), -1);
		long endTime = safeParse(extractTagValue(xml, "EndTime"), -1);
		long lastModified = safeParse(extractTagValue(xml, "LastModified"), -1);
		String name = extractTagValue(xml, "Name");
		String correlationId = extractTagValue(xml, "CorrelationId");
		String status = extractTagValue(xml, "Status");
		String path = extractTagValue(xml, "Path");
		String description = extractTagValue(xml, "Description");

		return new Metadata(((Long) storageId).intValue(), duration, ((Long) nrChpts).intValue(), estMemUsage, stroageSize, endTime, name, correlationId, status, path, description, lastModified);
	}

	/**
	 * Finds the tags in a given string and returns the string in-between.
	 *
	 * @param xml Xml string to be serached.
	 * @param tag Tag to be found.
	 * @return String that is contained in between tags. Null, if not found.
	 */
	private static String extractTagValue(String xml, String tag) {
		String startTag = "<" + tag + ">";
		int start = xml.indexOf(startTag);
		int end = xml.indexOf("</" + tag + ">", start + startTag.length());

		if (start < 0 || end < 0) {
			return null;
		}

		return StringEscapeUtils.unescapeXml(xml.substring(start + startTag.length(), end));
	}

	private static long safeParse(String str, long defaultValue) {
		if (str == null)
			return defaultValue;
		try {
			return Long.parseLong(str);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String formatTime(long time) {
		return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(time);
	}

	public int getStorageId() {
		return storageId;
	}

	/**
	 * Checks if the given pattern can be found in the given field value.
	 *
	 * @param field   Field to be used for value.
	 * @param pattern Pattern to be matched.
	 * @return True if the given pattern can be found in the value corresponding to the given field.
	 */
	public boolean fieldEquals(String field, Pattern pattern) {
		if (StringUtils.isEmpty(field) || pattern == null)
			return true;

		Object fieldval = getFieldValue(field);
		return fieldval != null && pattern.matcher(String.valueOf(fieldval)).find();
	}

	/**
	 * Returns the value of the given field.
	 *
	 * @param field Field to be used.
	 * @return Value of the field. Null, if field can not be found.
	 */
	private Object getFieldValue(String field) {
		// Todo: Maybe use reflect? Performance problems though
		if (StringUtils.isEmpty(name))
			return null;

		if (field.equalsIgnoreCase("storageId")) {
			return storageId;
		} else if (field.equalsIgnoreCase("endtime")) {
			return formatTime(endTime);
		} else if (field.equalsIgnoreCase("duration")) {
			return duration;
		} else if (field.equalsIgnoreCase("name")) {
			return name;
		} else if (field.equalsIgnoreCase("correlationId")) {
			return correlationId;
		} else if (field.equalsIgnoreCase("status")) {
			return status;
		} else if (field.equalsIgnoreCase("numberOfCheckpoints")) {
			return nrChpts;
		} else if (field.equalsIgnoreCase("estimatedMemoryUsage")) {
			return memoryUsage;
		} else if (field.equalsIgnoreCase("storageSize")) {
			return storageSize;
		} else if (field.equalsIgnoreCase("path")) {
			return path;
		} else if (field.equalsIgnoreCase("description")) {
			return description;
		} else if (field.equalsIgnoreCase("lastmodified")) {
			return lastModified;
		}

		return null;
	}
}
