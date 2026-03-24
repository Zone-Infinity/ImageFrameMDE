package me.isoham.imageframe.mde.storage;

public record ModerationRequest(
        String id,
        String playerUUID,
        String playerName,
        String command,
        String url,
        String hash,
        RequestStatus status,
        long createdAt
) {
}
