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
package org.wearefrank.ladybug.filter;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

// This interface is implemented by class org.wearefrank.ladybug.TestTool. The reason for introducing this
// interface is to hide most data of class TestTool for methods working with this interface.
//
// The metadata fields host and application have dynamic behavior that cannot be caught completely in Spring
// configuration files. When an instrumented application runs in a Kubernetes cluster, then the host and the
// application are relevant. The instrumented application should set the host and the application in class TestTool.
// These fields should then be shown in the table of the debug tab. Views should not omit these metadata items.
//
// On the other hand, the instrumented application may also run stand-alone. Then the host and the application are not
// relevant. The instrumented application should not set them on class TestTool. We do not want to configure this case
// in Spring configuration files so the host and the application are in the metadata names configured through Spring.
// Method filterMetadataNames() omits the host and the application as metadatanames of a view in this case.
// Metadata names host and application are still relevant for storages.
public interface HostAndApplicationHolder {
	boolean isHostSet();
	boolean isApplicationSet();

	public default List<String> filterMetadataNames(List<String> rawMetadataNames) {
		Set<String> omit = new HashSet<>();
		if(!isHostSet()) {
			omit.add("host");
		}
		if(!isApplicationSet()) {
			omit.add("application");
		}
		return rawMetadataNames.stream().filter(n -> !omit.contains(n)).collect(Collectors.toList());
	}
}
