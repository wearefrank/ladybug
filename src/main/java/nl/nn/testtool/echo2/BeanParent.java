package nl.nn.testtool.echo2;

public interface BeanParent {

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent);

	/**
	 * Echo2 Components already have a getParent() method which in some cases
	 * will do, but it's not always the same/needed hierarchy.
	 */
	public BeanParent getBeanParent();

}
