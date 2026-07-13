/*
   Copyright 2020, 2022-2026 WeAreFrank!, 2018, 2019 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.echo2.BeanParent;
import org.wearefrank.ladybug.echo2.Echo2Application;
import org.wearefrank.ladybug.echo2.reports.ReportsComponent;
import org.wearefrank.ladybug.storage.Storage;

/**
 * @author Jaco de Groot
 */
@Dependent
public class View implements BeanParent {
	protected String name;
	protected String nodeLinkStrategy;
	private @Setter @Getter @Inject @Autowired Storage debugStorage;
	private @Setter @Inject @Resource(name="metadataNames") List<String> metadataNames;
	// No autowired annotation here, otherwise we would have a circular dependency.
	// This member variable is instantiated by class TestTool.
	private @Setter TestTool testTool;
	private Map<String, String> metadataFilter;
	private List<CheckpointMatcher> checkpointMatchers;
	private BeanParent beanParent;
	private Echo2Application echo2Application;
	private @Inject @Autowired MetadataExtractor metadataExtractor;

	protected enum NodeLinkStrategy {
		PATH,
		CHECKPOINT_NUMBER,
		NONE
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setNodeLinkStrategy(String nodeLinkStrategy) {
		this.nodeLinkStrategy = nodeLinkStrategy;
	}

	public String getNodeLinkStrategy() {
		if (nodeLinkStrategy == null) {
			return NodeLinkStrategy.PATH.toString();
		}
		return nodeLinkStrategy;
	}

	// Metadata name "application" is special because it is determined dynamically whether it is returned
	// by a View. When a database storage is used and when the database stores reports from different
	// applications, showing column "application" in the metadata table of the debug tab makes sense.
	// If the storage only holds reports of a single application, then metadata name "application"
	// should be omitted. We want that these two situations do not require different Spring configurations
	// for Ladybug. This is the reason we have this method to filter the metadata names.
	public List<String> getMetadataNames() {
		Set<String> omit = new HashSet<>();
		if(testTool.getApplication() == null) {
			omit.add("application");
		}
		return metadataNames.stream().filter(n -> !omit.contains(n)).collect(Collectors.toList());
	}

	public void setMetadataFilter(Map<String, String> metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

	public Map<String, String> getMetadataFilter() {
		return metadataFilter;
	}

	public void setCheckpointMatchers(List<CheckpointMatcher> checkpointMatchers) {
		this.checkpointMatchers = checkpointMatchers;
	}

	/**
	 * @see Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	public Echo2Application getEcho2Application() {
		return echo2Application;
	}

	/**
	 * Was used by Tibet2 when using old GUI. Check is now done in getReport(), see
	 * https://github.com/frankframework/frankframework/commit/33c0af92e338c9d001e6ccf44565b44d0a64b6a3
	 * @param StorageId ...
	 * @return ...
	 */
	@Deprecated
	public String isOpenReportAllowed(Object StorageId) {
		return ReportsComponent.OPEN_REPORT_ALLOWED;
	}

	public boolean showCheckpoint(Report report, Checkpoint checkpoint) {
		boolean match = false;
		if (checkpointMatchers == null) {
			match = true;
		}
		for (int i = 0; !match && i < checkpointMatchers.size(); i++) {
			CheckpointMatcher checkpointMatcher = checkpointMatchers.get(i);
			match = checkpointMatcher.match(report, checkpoint);
		}
		return match;
	}

	public String toString() {
		return getName();
	}

	public List<String> getMetadataLabels(ApplicationMetadataItemHolder applicationMetadataItemHolder) {
		List<String> metadataLabels = new ArrayList<>();
		for (String metadataName : getMetadataNames(applicationMetadataItemHolder)) {
			metadataLabels.add(metadataExtractor.getLabel(metadataName));
		}

		return metadataLabels;
	}

	public boolean hasCheckpointMatchers() {
		return this.checkpointMatchers != null && !this.checkpointMatchers.isEmpty();
	}
}
