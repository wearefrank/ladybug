package org.wearefrank.ladybug.web.common;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

public class ReportUpdateRequest {
	private @Getter @Setter String name;
	private @Getter @Setter String path;
	private @Getter @Setter Map<String, String> variables;
	private @Getter @Setter String description;
	private @Getter @Setter String transformation;
	private @Getter @Setter String checkpointId;
	private @Getter @Setter String checkpointMessage;
	private @Getter @Setter String stub;
	private @Getter @Setter String stubStrategy;
}
