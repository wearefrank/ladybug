package nl.nn.testtool;

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

    public SpanKind getKind() {
        return kind;
    }

    public String getKindString(){
        if (kind == null) {
            return null;
        }
        return kind.toString();
    }
}

enum SpanKind {
    INTERNAL,
    SERVER,
    CLIENT,
    PRODUCER,
    CONSUMER
}
