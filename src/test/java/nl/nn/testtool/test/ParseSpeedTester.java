/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.testtool.test;

import nl.nn.testtool.Report;
//import nl.nn.testtool.util.ValueExtractor;

public class ParseSpeedTester {
    public final static int NR_TEST_REPEATS = 20;
    /**
     * Test parsing / evaluation speeds with re-usable XPath SOURCE (DOM) and
     * with XPath SOURCE that re-parses the XML every time (SAX).
     * 
     * This is not just a test of DOM vs. SAX speeds for XPath
     * evaluation, but also how the stuff is used in the TestTool code,
     * with more-or-less realistic input scenarios. Many different XMLs are
     * evaluated, each XML is evaluated with a small number of different XPath Expressions,
     * and some other processing / evaluation is going on too.
     * 
     * The winner, DOM or SAX, depends on the number of XPath queries to be
     * evaluated and the type of XPath queries. More Xpath queries against same
     * XML seems to make DOM win out with nearly no extra time for the DOM version;
     * also, extracting (relatively) large strings turns the tables in favour of
     * DOM parsing.
     * 
     * The DOM version always seems to have a huge up-front parsing overhead, and
     * most of the times SAX wins.
     * 
     * @param args
     */
//	TODO [junit] weer aanzetten en repareren
/*
    public static void main(String[] args) {
        Storage storage; 
        storage = new nl.nn.testtool.storage.file.Storage();
        Report[] allReports = loadAllReports(storage);
        storage.close();
        
        ValueExtractor.DOM_SOURCE_FOR_XPATH = true;
        parseAllValues(allReports, NR_TEST_REPEATS);
        ValueExtractor.DOM_SOURCE_FOR_XPATH = false;
        parseAllValues(allReports, NR_TEST_REPEATS);
        System.out.println("Done.");
    }
*/
    private static void parseAllValues(Report[] allReports, int nrOfTimes) {
//        System.out.println("Begin parsing " + nrOfTimes + " times, Using DOM Source: " + ValueExtractor.DOM_SOURCE_FOR_XPATH);
        long start = System.currentTimeMillis();
        long duration = 0L;
        long slowest = -1L;
        long fastest = -1L;
        for(int i = 0; i < nrOfTimes; ++i) {
            long start1 = System.currentTimeMillis();
            for (int j = 0; j < allReports.length; j++) {
                Report report = allReports[j];
//                report.reparseExtraValues();
            }
            long end1 = System.currentTimeMillis();
            long duration1 = end1 - start1;
            duration += duration1;
            if (duration1 > slowest) {
                System.out.println(i + " is slowest so far, duration: " + duration1 + "ms");
                slowest = duration1;
            }
            if (fastest == -1L || duration1 < fastest) {
                System.out.println(i + " is fastest so far, duration: " + duration1 + "ms");
                fastest = duration1;
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Time spent: " + duration + "ms");
        System.out.println("Overhead: " + ((end-start) - duration) + "ms");
        System.out.println("Fastest: " + fastest + "ms");
        System.out.println("Slowest: " + slowest + "ms");
        System.out.println("Average: " + (duration/nrOfTimes) + "ms");
        System.out.println("Average excluding extremes: " + ((duration-fastest-slowest)/(nrOfTimes-2)) + "ms");
    }

//	TODO [junit] weer aanzetten en repareren
/*
    private static Report[] loadAllReports(Storage storage) {
        Reports reports = storage.getReports();
        Metadata metadata = reports.getMetadata();
        List storageIds = new ArrayList(metadata.getStorageIds());
        Collections.reverse(storageIds);
        System.out.println("Nr of reports in file: " + storageIds.size());
        System.out.println("Error message:" + storage.getErrorMessage());
        Report[] allReports = new Report[storageIds.size()];
        
        for (int i = 0; i < allReports.length; i++) {
            Integer storageId = (Integer)storageIds.get(i);
            allReports[i] = reports.getReport(storageId);
            System.out.println("  Loaded report#" + i +
                    ": " + allReports[i].getName());
        }
        return allReports;
    }
*/
}
