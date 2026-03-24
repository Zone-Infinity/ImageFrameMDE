package me.isoham.imageframe.mde.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class RequestRepository {

    private final Connection connection;

    public RequestRepository(Connection connection) {
        this.connection = connection;
    }

    public void insert(ModerationRequest request) throws Exception {
        String sql = """
                INSERT INTO requests(id,player_uuid,player_name,command,url,hash,status,created_at)
                VALUES(?,?,?,?,?,?,?,?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, request.id());
            ps.setString(2, request.playerUUID());
            ps.setString(3, request.playerName());
            ps.setString(4, request.command());
            ps.setString(5, request.url());
            ps.setString(6, request.hash());
            ps.setString(7, request.status().name());
            ps.setLong(8, request.createdAt());

            ps.executeUpdate();
        }
    }

    public Optional<ModerationRequest> findById(String id) throws Exception {
        String sql = "SELECT * FROM requests WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ModerationRequest(
                        rs.getString("id"),
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getString("command"),
                        rs.getString("url"),
                        rs.getString("hash"),
                        RequestStatus.valueOf(rs.getString("status")),
                        rs.getLong("created_at")
                ));
            }
        }
    }

    public void updateStatus(String id, RequestStatus status) throws Exception {
        String sql = "UPDATE requests SET status=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, id);

            ps.executeUpdate();
        }
    }

    public void deleteOldRequests(long cutoffTimestamp) throws Exception {
        String sql = """
                DELETE FROM requests
                WHERE created_at < ?
                AND status != 'PENDING'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, cutoffTimestamp);
            ps.executeUpdate();
        }
    }

    public void loadPendingCache(Map<UUID, Integer> pendingRequests,
                                 Set<String> pendingHashes) throws Exception {
        String sql = """
                SELECT player_uuid, hash
                FROM requests
                WHERE status = 'PENDING'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String hash = rs.getString("hash");

                pendingRequests.merge(uuid, 1, Integer::sum);
                pendingHashes.add(hash);
            }
        }
    }
}
