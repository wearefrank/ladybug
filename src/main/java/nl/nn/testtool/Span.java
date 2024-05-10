package nl.nn.testtool;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Span {
    private String traceId;
    private String parentId;
    private String id;
    private SpanKind kind;
    private String name;
    private long timestamp;
    private long duration;
    private Map<String, Object> localEndpoint;
    private Map<String, Object> tags;

    public Span(String traceId, String parentId, String id, SpanKind kind, String name, long timestamp, long duration, Map<String, Object> localEndpoint, Map<String, Object> tags) {
        this.traceId = traceId;
        this.parentId = parentId;
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.timestamp = timestamp;
        this.duration = duration;
        this.localEndpoint = localEndpoint;
        this.tags = tags;
    }

    public Span() {
    }

    public String getTraceId() {
        return traceId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public Map<String, Object> getLocalEndpoint() {
        return localEndpoint;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public String getKind() {
        if (kind == null) {
            return null;
        }
        return kind.toString();
    }

    public Map<String, Object> toHashmap() {
        Map<String, Object> map = new HashMap<>();
        map.put("traceId", this.traceId);
        map.put("parentId", this.parentId);
        map.put("id", this.id);
        map.put("kind", this.kind);
        map.put("name", this.name);
        map.put("time", new Date(this.timestamp / 1000));
        map.put("duration", this.duration);
        map.put("localEndpoint", this.localEndpoint);
        map.put("tags", this.tags);

        return map;
    }
}
