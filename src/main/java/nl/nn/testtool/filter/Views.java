package nl.nn.testtool.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.nn.testtool.echo2.BeanParent;

/**
 * @author Jaco de Groot
 */
public class Views extends ArrayList<View> implements BeanParent {
	private static final long serialVersionUID = 1L;
	private View defaultView;
	private BeanParent beanParent;

	public void setViews(List<View> views) {
		clear();
		addAll(views);
		if (size() > 0) {
			defaultView = get(0);
		}
	}

	public View setDefaultView(String defaultView) {
		Iterator iterator = iterator();
		while (iterator.hasNext()) {
			View view = (View)iterator.next();
			if (defaultView.equals(view.toString())) {
				this.defaultView = view;
				return view;
			}
		}
		return null;
	}

	public View getDefaultView() {
		return defaultView;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		for (View view : this) {
			view.initBean(this);
		}
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}
}
