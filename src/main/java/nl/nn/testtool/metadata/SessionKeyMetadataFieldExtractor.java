package nl.nn.testtool.metadata;

import java.util.Iterator;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author Peter Leeuwenburgh
 */
public class SessionKeyMetadataFieldExtractor extends
		DefaultValueMetadataFieldExtractor {
	private static Logger log = LogUtil
			.getLogger(SessionKeyMetadataFieldExtractor.class);
	protected String sessionKey;

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public Object extractMetadata(Report report) {
		String value = null;
		Iterator iterator = report.getCheckpoints().iterator();
		while (value == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint) iterator.next();
			String checkpointName = checkpoint.getName();
			if (checkpointName.startsWith("SessionKey ")) {
				String sessionKeyName = checkpointName.substring("SessionKey "
						.length());
				if (sessionKeyName.equals(sessionKey)) {
					value = checkpoint.getMessage();
				}
			}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

}
