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
package nl.nn.testtool.echo2.util;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.FileDownloadProvider;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;

public class Download {

	public static String download(Storage storage) {
		return download(Export.export(storage));
	}

	public static String download(Storage storage, String filename) {
		return download(Export.export(storage, filename));
	}

	public static String download(Storage storage, String filename,
			boolean exportReport, boolean exportReportXml) {
		return download(Export.export(storage, filename, exportReport,
				exportReportXml));
	}

	public static String download(Report report) {
		return download(Export.export(report));
	}

	public static String download(Report report, boolean exportReport,
			boolean exportReportXml) {
		return download(Export.export(report, exportReport, exportReportXml));
	}

	public static String download(Report report, Checkpoint checkpoint) {
		return download(Export.export(report, checkpoint));
	}

	public static String download(Checkpoint checkpoint) {
		return download(Export.export(checkpoint));
	}

	private static String download(ExportResult exportResult) {
		String errorMessage = exportResult.getErrorMessage();
		if (errorMessage == null) {
				FileDownloadProvider fileDownloadProvider = new FileDownloadProvider(
						exportResult.getTempFile(),
						exportResult.getSuggestedFilename());
				nextapp.echo2.app.filetransfer.Download download = new nextapp.echo2.app.filetransfer.Download();
				download.setProvider(fileDownloadProvider);
				download.setActive(true);
				Echo2Application echo2Application = (Echo2Application)Echo2Application.getActive();
				echo2Application.enqueueCommand(download);
				// TODO file nog deleten? kan pas als ie is gedownload? blijven die nu allemaal ergens op de server aanwezig tot herstart? checken?
		}
		return errorMessage;
	}

}
