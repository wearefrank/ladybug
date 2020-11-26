/*
   Copyright 2018 Nationale-Nederlanden

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
