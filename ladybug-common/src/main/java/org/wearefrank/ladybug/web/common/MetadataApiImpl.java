package org.wearefrank.ladybug.web.common;

import jakarta.inject.Inject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetadataApiImpl {
    private @Setter
    @Inject
    @Autowired TestTool testTool;

    public List<LinkedHashMap<String, String>> getMetadataList(String storageName,
                                                               List<String> metadataNames,
                                                               int limit,
                                                               List<String> filterHeaders,
                                                               List<String> filterParams) throws Exception {
        List<String> searchValues = new ArrayList<>();
        for (String field : metadataNames) {
            boolean changed = false;
            for (int filterHeaderIndex = 0; filterHeaderIndex < filterHeaders.size(); filterHeaderIndex++) {
                if (filterHeaders.get(filterHeaderIndex).equals(field)) {
                    searchValues.add(filterParams.get(filterHeaderIndex));
                    changed = true;
                }
            }
            if (!changed) {
                searchValues.add(null);
            }
        }
        // Get storage, search for metadata, and return the results.
        Storage storage = testTool.getStorage(storageName);
        List<List<Object>> records = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_GUI);
        List<LinkedHashMap<String, String>> metadata = new ArrayList<>();
        for (List<Object> record : records) {
            LinkedHashMap<String, String> metadataItem = new LinkedHashMap<>();
            metadataItem.put("storageId", record.get(0).toString());
            for (int i = 1; i < metadataNames.size(); i++) {
                String metadataValue = null;
                if (record.get(i) != null) {
                    metadataValue = record.get(i).toString();
                }
                metadataItem.put(metadataNames.get(i), metadataValue);
            }
            metadata.add(metadataItem);
        }
        return metadata;
    }

    public Map<String, String> getUserHelp(String storageName, List<String> metadataNames) {
        Map<String, String> userHelp = new LinkedHashMap<>();
        Storage storage = testTool.getStorage(storageName);
        for (String field : metadataNames) {
            userHelp.put(field, storage.getUserHelp(field));
        }
        return userHelp;
    }

    public int getMetadataCount(String storageName) throws Exception {
        Storage storage = testTool.getStorage(storageName);
        return storage.getSize();
    }
}
