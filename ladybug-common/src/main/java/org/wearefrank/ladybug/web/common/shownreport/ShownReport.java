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

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

public class ShownReport extends TreeNode implements Serializable {
	private static final long serialVersionUID = 103;

	private @Setter @Getter String description;
	private @Setter @Getter String path;
	private @Setter @Getter @NotNull String stubStrategy;
	private @Setter @Getter @NotNull String linkMethod;
	private @Setter @Getter String transformation;
	private @Setter @Getter @NotNull int storageId;
	private @Setter @Getter @NotNull String storageName;
	private @Setter @Getter @NotNull boolean crudStorage;
	private @Setter @Getter @NotNull long estimatedMemoryUsage;
	private @Setter @Getter @NotNull String correlationId;
	private @Setter @Getter Map<String, String> variables;
}
