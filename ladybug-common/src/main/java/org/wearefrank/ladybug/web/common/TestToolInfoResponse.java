package org.wearefrank.ladybug.web.common;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class TestToolInfoResponse {
	private @Getter @Setter boolean generatorEnabled;
	private @Getter @Setter long estMemory;
	private @Getter @Setter String regexFilter;
	private @Getter @Setter long reportsInProgress;
	private @Getter @Setter List<String> stubStrategies;
	private @Getter @Setter String transformation;
	private @Getter @Setter TestPropertiesConfiguration.UI_TEST_MODE uiTestMode;
	private @Getter @Setter List<String> roles;
}
