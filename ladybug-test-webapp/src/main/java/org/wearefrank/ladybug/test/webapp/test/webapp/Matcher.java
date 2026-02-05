package org.wearefrank.ladybug.test.webapp.test.webapp;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.filter.CheckpointMatcher;

public class Matcher implements CheckpointMatcher {

    public boolean match(Report report, Checkpoint checkpoint) {
        if (checkpoint.getName() != null && checkpoint.getName().equals("Hide this checkpoint")) {
            return false;
        }
        return true;
    }
}
