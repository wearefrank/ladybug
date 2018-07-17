package nl.nn.testtool;

import java.util.Map;

/**
 * @author Jaco de Groot
 */
public interface MetadataFieldExtractor {

	public String getName();

	public String getLabel();

	public String getShortLabel();

	public Object extractMetadata(Report report);
}
