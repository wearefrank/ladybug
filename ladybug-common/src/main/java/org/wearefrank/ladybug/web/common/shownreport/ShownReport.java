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
package org.wearefrank.ladybug.web.common.shownreport;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

// When editing, please compare with frontend interface definition HierarchicalReport.
public class ShownReport extends TreeNode implements Serializable {
	private static final long serialVersionUID = 103;

	private @Setter @Getter String description;
	private @Setter @Getter String path;
	private @Setter @Getter String stubStrategy;
	private @Setter @Getter String linkMethod;
	private @Setter @Getter String transformation;
	// Primitive type, cannot be null.
	private @Setter @Getter int storageId;
	private @Setter @Getter String storageName;
	private @Setter @Getter boolean crudStorage;
	private @Setter @Getter long estimatedMemoryUsage;
	private @Setter @Getter String correlationId;
	private @Setter @Getter Map<String, String> variables;
	private @Setter @Getter long startTime;

	public void validate() {
		checkNotNull(getName(), "name");
		checkNotNull(stubStrategy, "stubStrategy");
		checkNotNull(linkMethod, "linkMethod");
		checkNotNull(storageName, "storageName");
		checkNotNull(correlationId, "correlationId");
		if (getChildren() != null) {
			getChildren().forEach(ShownCheckpoint::validate);
		}
	}

	private void checkNotNull(String value, String name) {
		if (value == null) {
			String message = String.format("ShownReport.%s should not be null", name);
			throw new IllegalArgumentException(message);
		}
	}
}
