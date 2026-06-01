package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.wearefrank.ladybug.storage.database.DatabaseTracingStorage;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestDatabaseTracingStorage {
    private DatabaseTracingStorage storage;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        storage = new DatabaseTracingStorage();

        jdbcTemplate = mock(JdbcTemplate.class);
        storage.setLadybugJdbcTemplate(jdbcTemplate);
    }

    @Test
    public void testByteStringToHex() {
        ByteString byteString = ByteString.copyFromUtf8("test");

        String result = storage.byteStringToHex(byteString);

        assertEquals("74657374", result);
    }

    @Test
    public void testStoreSpan() throws Exception {
        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace"))
                .setSpanId(ByteString.copyFromUtf8("span"))
                .setParentSpanId(ByteString.copyFromUtf8("parent"))
                .setName("test-span")
                .build();

        storage.store(span);

        verify(jdbcTemplate, times(1))
                .update(
                        contains("insert into LADYBUGTRACING"),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    public void testDropSpansByTraceId() {
        List<String> traceIds = List.of(
                "trace1",
                "trace2"
        );

        storage.dropSpansByTraceId(traceIds);

        verify(jdbcTemplate, times(1))
                .update(
                        contains("DELETE FROM LADYBUGTRACING"),
                        eq("trace1")
                );

        verify(jdbcTemplate, times(1))
                .update(
                        contains("DELETE FROM LADYBUGTRACING"),
                        eq("trace2")
                );
    }

    @Test
    public void testDropAllSpans() {
        storage.dropAllSpans();

        verify(jdbcTemplate, times(1))
                .execute("TRUNCATE TABLE LADYBUGTRACING");
    }
}