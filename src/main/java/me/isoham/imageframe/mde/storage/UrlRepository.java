package me.isoham.imageframe.mde.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UrlRepository {
    private final Connection connection;

    public UrlRepository(Connection connection) {
        this.connection = connection;
    }

    public UrlStatus getStatus(String hash) throws Exception {
        String sql = "SELECT status FROM urls WHERE hash=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return UrlStatus.UNKNOWN;
                }

                return UrlStatus.valueOf(rs.getString("status"));
            }
        }
    }

    public void insertOrUpdate(String hash, String url, UrlStatus status, String moderator) throws Exception {
        String sql = """
                INSERT INTO urls(hash, url, status, moderated_by, created_at)
                VALUES(?,?,?,?,?)
                ON CONFLICT(hash)
                DO UPDATE SET
                    status=excluded.status,
                    moderated_by=excluded.moderated_by
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, url);
            ps.setString(3, status.name());
            ps.setString(4, moderator);
            ps.setLong(5, System.currentTimeMillis());

            ps.executeUpdate();
        }
    }
}
