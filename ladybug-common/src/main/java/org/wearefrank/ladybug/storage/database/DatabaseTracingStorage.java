package org.wearefrank.ladybug.storage.database;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class DatabaseTracingStorage {
    protected @Setter @Getter @Inject @Autowired JdbcTemplate ladybugJdbcTemplate;

    public void store(Span span) throws SQLException {
        try {
            String json = JsonFormat.printer().print(span);

            String query = "insert into LADYBUGTRACING (TRACEID, SPANID, PARENTSPANID, SPANJSON) values (?, ?, ?, ?)";

            ladybugJdbcTemplate.update(
                    query,
                    byteStringToHex(span.getTraceId()),
                    byteStringToHex(span.getSpanId()),
                    byteStringToHex(span.getParentSpanId()),
                    json
            );
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public List<Span> getAllSpans() throws SQLException {
        String query = "SELECT SPANJSON FROM LADYBUGTRACING";

        return ladybugJdbcTemplate.query(query, (rs, rowNum) -> {
            String json = rs.getString("SPANJSON");

            Span.Builder builder = Span.newBuilder();
            try {
                JsonFormat.parser().merge(json, builder);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            return builder.build();
        });
    }

    public int getAllUniqueTraceIDs() throws SQLException {
        String query = "SELECT COUNT(DISTINCT TRACEID) FROM LADYBUGTRACING";
        return ladybugJdbcTemplate.queryForObject(query, Integer.class);
    }

    public String byteStringToHex(ByteString byteString) {
        return Hex.encodeHexString(byteString.toByteArray());
    }

    public ByteString hexToByteString(String hex) {
        try {
            return ByteString.copyFrom(Hex.decodeHex(hex));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode hex string", e);
        }
    }
}
