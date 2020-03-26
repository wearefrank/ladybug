package nl.nn.testtool.storage.xml;

import nl.nn.testtool.Report;
import nl.nn.testtool.util.EscapeUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class Metadata {
	private long storageId, nrChpts, memoryUsage, storageSize, duration, endTime;
	private String name, correlationId, status;

	public Metadata(long storageId, long duration, long nrChpts, long memoryUsage, long storageSize, long endTime, String name, String correlationId, String status) {
		this.storageId = storageId;
		this.duration = duration;
		this.nrChpts = nrChpts;
		this.memoryUsage = memoryUsage;
		this.storageSize = storageSize;
		this.endTime = endTime;
		this.name = name;
		this.correlationId = correlationId;
		this.status = status;
	}

	public List<Object> toObjectList() {
		return Arrays.asList(new Object[] {
				storageId,
				new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(endTime),
				duration,
				name,
				correlationId,
				status,
				nrChpts,
				memoryUsage,
				storageSize
		});
	}

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
				"</Metadata>\n";
		return String.format(template, storageId, endTime, duration, EscapeUtil.escapeXml(name), EscapeUtil.escapeXml(correlationId), EscapeUtil.escapeXml(status), nrChpts, memoryUsage, storageSize);
	}

	public static Metadata fromReport(Report report, long storageId) {
		return new Metadata(
				storageId,
				report.getEndTime() - report.getStartTime(),
				report.getNumberOfCheckpoints(),
				report.getEstimatedMemoryUsage(),
				0L,
				report.getEndTime(),
				report.getName(),
				report.getCorrelationId(),
				"SUCCESS");
		// TODO: Remove success and find out where status comes from!!
	}

	public static Metadata fromXml(String xml) {
		xml = extractTagValue(xml, "Metadata");
		long storageId = safeParse(extractTagValue(xml, "StorageId"), -1);
		long duration = safeParse(extractTagValue(xml, "Duration"), -1);
		long nrChpts = safeParse(extractTagValue(xml, "NrChpts"), -1);
		long estMemUsage = safeParse(extractTagValue(xml, "EstMemUsage"), -1);
		long stroageSize = safeParse(extractTagValue(xml, "StroageSize"), -1);
		long endTime = safeParse(extractTagValue(xml, "EndTime"), -1);
		String name = extractTagValue(xml, "Name");
		String correlationId = extractTagValue(xml, "CorrelationId");
		String status = extractTagValue(xml, "Status");

		return new Metadata(storageId, duration, nrChpts, estMemUsage, stroageSize, endTime, name, correlationId, status);
	}

	private static String extractTagValue(String xml, String tag) {
		String startTag = "<" + tag + ">";
		int start = xml.indexOf(startTag);
		int end = xml.indexOf("</" + tag + ">", start + startTag.length());

		if (start < 0 || end < 0) {
			return null;
		}

		return xml.substring(start + startTag.length(), end);
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

	public long getStorageId() {
		return storageId;
	}

	public boolean equals(Object other) {
		if (other instanceof Metadata) {
			Metadata m = (Metadata) other;
			return m.storageId == this.storageId;
		}
		return false;
	}
}
