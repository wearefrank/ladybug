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
package org.wearefrank.ladybug.echo2;

public interface BeanParent {

	/**
	 * @see Echo2Application#initBean()
	 * @param beanParent ...
	 */
	public void initBean(BeanParent beanParent);

	/**
	 * Echo2 Components already have a getParent() method which in some cases
	 * will do, but it's not always the same/needed hierarchy.
	 * @return ...
	 */
	public BeanParent getBeanParent();

}
