/*
   Copyright 2026 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.web.common.shownreport;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.wearefrank.ladybug.StubType;

import java.io.Serializable;
import java.util.Map;

public class ShownCheckpoint extends TreeNode implements Serializable {
	private static final long serialVersionUID = 104;
	private @Getter @Setter String message;
	private @Getter @Setter String encoding;
	private @Getter @Setter Map<String, Object> messageContext;
	private @Getter @Setter @NotNull int type;
	private @Getter @Setter @NotNull int level = 0;
	private @Getter @Setter @NotNull int stub = StubType.FOLLOW_REPORT_STRATEGY.toInt();
	private @Getter @Setter @NotNull boolean stubbed = false;
	private @Getter @Setter String stubNotFound;
	private @Getter @Setter @NotNull int preTruncatedMessageLength = -1;
	private @Getter @Setter @NotNull String typeAsString;
	private @Getter @Setter @NotNull String threadName;
	private @Getter @Setter String sourceClassName;
	private @Getter @Setter String messageClassName;
	private @Getter @Setter @NotNull String uid;
}
