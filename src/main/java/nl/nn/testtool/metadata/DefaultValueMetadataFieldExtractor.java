 package nl.nn.testtool.metadata;

import java.util.Map;

import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;

/**
 * 
 * @author Jaco de Groot
 */
public class DefaultValueMetadataFieldExtractor implements MetadataFieldExtractor {
	protected String name;
	protected String label;
	protected String shortLabel;
	protected String defaultValue;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setShortLabel(String shortLabel) {
		this.shortLabel = shortLabel;
	}

	public String getShortLabel() {
		return shortLabel;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public Object extractMetadata(Report report) {
		return defaultValue;
	}

}
