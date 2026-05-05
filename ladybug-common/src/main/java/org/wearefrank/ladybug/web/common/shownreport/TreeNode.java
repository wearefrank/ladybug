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

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
	// name cannot be null, is tested in ShownReport.validate() and ShownCheckpoint.validate().
	private @Getter @Setter String name;
	private @Getter List<ShownCheckpoint> children = null;

	public void addChild(ShownCheckpoint child) {
		if (children == null) {
			children = new ArrayList<>();
		}
		children.add(child);
	}
}
