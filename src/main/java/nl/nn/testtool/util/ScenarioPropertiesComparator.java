package nl.nn.testtool.util;

import java.util.Comparator;

public class ScenarioPropertiesComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int rank1 = getRank(o1), rank2 = getRank(o2);
        int rankDiff = Integer.compare(rank1, rank2);
        if (rankDiff != 0) {
            return rankDiff;
        } else {
            return String.CASE_INSENSITIVE_ORDER.compare(o1, o2);
        }
    }

    private static int getRank(String s) {
        s = s.trim();
        String[] sParts = s.split("\\.");
        switch (sParts[0]) {
            case "scenario":
                return 0;
            case "include":
                return 1;
            default:
                if (sParts[0].startsWith("step")) {
                    return 3 + Integer.parseInt(sParts[0].substring(4));
                }
        }
        //substring from second dot (or first if there is only 1);
        int j = s.indexOf(".");
        String s1 = s.substring(Math.max(s.indexOf(".", j + 1) + 1, j + 1));
        if (s1.matches("^param\\d+\\.(name|value)$")) {
            return 2;
        }
        return -1;
    }
}
