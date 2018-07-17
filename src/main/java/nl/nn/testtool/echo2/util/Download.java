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
