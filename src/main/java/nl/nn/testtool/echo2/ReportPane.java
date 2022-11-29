/*
   Copyright 2020, 2022 WeAreFrank!, 2018 Nationale-Nederlanden

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

import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.SplitPane;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.reports.InfoPane;
import nl.nn.testtool.echo2.reports.TreePane;

public class ReportPane extends ContentPane {
	private static final long serialVersionUID = 1L;
	private Report report;
	private TreePane treePane;
	private InfoPane infoPane;

	public ReportPane() {
		super();
	}

	public void setReport(Report report) {
		this.report = report;
	}

	public Report getReport() {
		return report;
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}

	public void setInfoPane(InfoPane infoPane) {
		this.infoPane = infoPane;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		SplitPane splitPane = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL);
		splitPane.setResizable(true);
		splitPane.setSeparatorPosition(new Extent(400, Extent.PX));
		splitPane.add(treePane);
		splitPane.add(infoPane);
		add(splitPane);
	}

}