/*
   Copyright 2023 WeAreFrank!

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

package nl.nn.testtool.metadata;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import java.util.Iterator;

public class CheckpointNameMetadataFieldExtractor extends DefaultValueMetadataFieldExtractor {
    protected String checkpointName;
    protected boolean extractFromFirstCheckpointOnly = true;

    public void setExtractFromFirstCheckpointOnly(boolean extractFromFirstCheckpointOnly) {
        this.extractFromFirstCheckpointOnly = extractFromFirstCheckpointOnly;
    }

    public void setCheckpointName(String checkpointName) {
        this.checkpointName = checkpointName;
    }

    public Object extractMetadata(Report report) {
        StringBuilder value = new StringBuilder();
        Iterator iterator = report.getCheckpoints().iterator();
        while (iterator.hasNext()) {
            Checkpoint checkpoint = (Checkpoint) iterator.next();
            String currentCheckpointName = checkpoint.getName();
            if (currentCheckpointName.equals(checkpointName)) {
                value.append(checkpoint.getMessage());
                if (extractFromFirstCheckpointOnly) {
                    break;
                }
            }
        }
        if (value.length() == 0 && defaultValue != null) {
            value = new StringBuilder(defaultValue);
        }
        return value.toString();
    }
}
