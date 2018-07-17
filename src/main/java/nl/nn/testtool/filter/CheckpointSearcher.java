package nl.nn.testtool.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
public class CheckpointSearcher implements CheckpointMatcher {
	private int type = -1;
	private String nameSearch;

	public void setType(int type) {
		this.type = type;
	}

	public void setNameSearch(String nameSearch) {
		this.nameSearch = nameSearch;
	}
	
	public boolean match(Report report, Checkpoint checkpoint) {
		if (type != -1 && type != checkpoint.getType()) {
			return false;
		} else {
			return SearchUtil.matches(checkpoint.getName(), nameSearch);
		}
	}

}
