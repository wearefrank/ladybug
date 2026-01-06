/*
   Copyright 2026 WeAreFrank!

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
