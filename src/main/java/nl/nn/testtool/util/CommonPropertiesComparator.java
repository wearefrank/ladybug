package nl.nn.testtool.util;

import java.util.Comparator;

public class CommonPropertiesComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int rank1 = getRank(o1), rank2 = getRank(o2);
//        if (rank1 == -1 || rank2 == -1) return -1;
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
            case "include":
                return 0;
            case "adapter":
                return 1;
            case "stub":
                return 2;
            default:
                if (sParts[0].startsWith("ignoreContentBetweenKeys")) {
                    String ignoreIdx = sParts[0].substring(24);
                    if (ignoreIdx.isEmpty()) return 3;
                    return 3 + 2 * Integer.parseInt(ignoreIdx) + Integer.parseInt(sParts[sParts.length - 1].substring(3));
                }
        }
        return -1;
    }
}
