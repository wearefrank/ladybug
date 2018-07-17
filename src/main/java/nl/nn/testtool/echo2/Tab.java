package nl.nn.testtool.echo2;

import nextapp.echo2.app.ContentPane;

public class Tab extends ContentPane implements BeanParent {
	private static final long serialVersionUID = 1L;
	BeanParent beanParent;

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

}
