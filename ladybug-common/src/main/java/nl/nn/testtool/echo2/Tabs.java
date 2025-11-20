/*
   Copyright 2020, 2022, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
