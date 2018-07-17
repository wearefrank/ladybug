package nl.nn.testtool.filter;

import java.util.List;
import java.util.Map;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.storage.Storage;

/**
 * @author Jaco de Groot
 */
public class View implements BeanParent {
	private String name;
	private Storage storage;
	private List metadataNames;
	private Map metadataFilter;
	private List checkpointMatchers;
	private BeanParent beanParent;
	private Echo2Application echo2Application;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	public Storage getStorage() {
		return storage;
	}

	public void setMetadataNames(List metadataNames) {
		this.metadataNames = metadataNames;
	}

	public List getMetadataNames() {
		return metadataNames;
	}

	public void setMetadataFilter(Map metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

	public Map getMetadataFilter() {
		return metadataFilter;
	}

	public void setCheckpointMatchers(List checkpointMatchers) {
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
		return "Allowed";
	}

	public boolean showCheckpoint(Report report, Checkpoint checkpoint) {
		boolean match = false;
		if (checkpointMatchers == null) {
			match = true;
		}
		for (int i = 0; !match && i < checkpointMatchers.size(); i++) {
			CheckpointMatcher checkpointMatcher = (CheckpointMatcher)checkpointMatchers.get(i);
			match = checkpointMatcher.match(report, checkpoint);
		}
		return match;		
	}

	public String toString() {
		return name;
	}

}
