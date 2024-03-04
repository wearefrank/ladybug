/*
   Copyright 2022 WeAreFrank!

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

package nl.nn.testtool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

public class CsvUtil {

	/**
     * @param text ...
     * @param delimiter ...
     * @return An error message as a result of unsuccessful validation; null if validation was successful.
     */
	public static String validateCsv(String text, String delimiter) {
		return validateCsv(text, delimiter, -1);
	}

	/**
     * @param text ...
     * @param delimiter ...
     * @param maxAmtOfRows ...
     * @return An error message as a result of unsuccessful validation; null if validation was successful.
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