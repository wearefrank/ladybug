package nl.nn.testtool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;

public class CsvUtil {

	/**
     * 
     * @return An error message as a result of unsuccessful validation;
     * null if validation was successful.
     */
	public static String validateCsv(String text, String delimiter) {
		return validateCsv(text, delimiter, -1);
	}

	/**
     * 
     * @return An error message as a result of unsuccessful validation;
     * null if validation was successful.
     */
	public static String validateCsv(String text, String delimiter, int maxAmtOfRows) {
		Scanner scanner = new Scanner(text);
		List<String> lines = new ArrayList<String>();
		while(scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if(StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#")) {
				lines.add(nextLine);
			}
		}
		scanner.close();
		
		if(lines.size() < 2) {
			return "Invalid CSV: must contain at least two rows";
		}
		if(maxAmtOfRows > 1 && lines.size() > maxAmtOfRows) {
			return "Invalid CSV: may only contain a maximum of "+maxAmtOfRows+" rows";
		}
		for(int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if(!(line.split(delimiter).length == lines.get(0).split(delimiter).length)) {
				return "Invalid CSV at row "+(i+1)+": all rows must contain an equal amount of comma-separated values";
			}
		}
		return null;
	}
}