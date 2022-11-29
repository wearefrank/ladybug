/*
   Copyright 2020, 2022 WeAreFrank!, 2018, 2019 Nationale-Nederlanden

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
package nl.nn.testtool.filter;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.storage.LogStorage;

/**
 * @author Jaco de Groot
 */
@Dependent
public class View implements BeanParent {
	private String name;
	private @Setter @Getter @Inject @Autowired LogStorage debugStorage;
	private @Setter @Getter @Inject @Autowired List<String> metadataNames;
	private Map<String, String> metadataFilter;
	private List<CheckpointMatcher> checkpointMatchers;
	private BeanParent beanParent;
	private Echo2Application echo2Application;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
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
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
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
		return name;
	}

}
