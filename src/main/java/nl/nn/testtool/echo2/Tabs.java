package nl.nn.testtool.echo2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jaco de Groot
 */
public class Tabs extends ArrayList<Tab> implements BeanParent {
	private static final long serialVersionUID = 1L;
	BeanParent beanParent;

	public void setTabs(List<Tab> Tabs) {
		addAll(Tabs);
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		for (Tab tab : this) {
			tab.initBean(this);
		}
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}
}
