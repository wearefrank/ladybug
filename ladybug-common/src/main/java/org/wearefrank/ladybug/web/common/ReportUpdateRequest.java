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
	// Empty string means path has to be cleared.
	private @Getter boolean pathModified = false;
	private @Getter String path;
	private @Getter @Setter Map<String, String> variables;
	// Empty string means description has to be cleared.
	private @Getter boolean descriptionModified = false;
	private @Getter String description;
	// Empty string means transformation has to be cleared.
	private @Getter boolean transformationModified = false;
	private @Getter String transformation;
	private @Getter @Setter Integer checkpointId;
	private @Getter @Setter String checkpointMessage;
	private @Getter @Setter String stub;
	private @Getter @Setter String stubStrategy;

	public void setPath(String path) {
		this.pathModified = true;
		this.path = path;
	}

	public void setDescription(String description) {
		this.descriptionModified = true;
		this.description = description;
	}

	public void setTransformation(String transformation) {
		this.transformationModified = true;
		this.transformation = transformation;
	}
}
