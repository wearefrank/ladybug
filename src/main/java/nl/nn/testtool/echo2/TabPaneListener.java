/*
   Copyright 2019 Nationale-Nederlanden

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TabPaneListener implements PropertyChangeListener {
	private static final long serialVersionUID = 1L;
	private Echo2Application echo2Application;

	TabPaneListener(Echo2Application echo2Application) {
		this.echo2Application = echo2Application;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("activeTabIndex")) {
			echo2Application.addToActiveTabIndexHistory((Integer)event.getNewValue());
		}
	}

}
