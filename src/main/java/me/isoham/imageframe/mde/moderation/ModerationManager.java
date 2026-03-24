package me.isoham.imageframe.mde.moderation;

import me.isoham.imageframe.mde.storage.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModerationManager {
    private final JavaPlugin plugin;
    private final UrlRepository urlRepository;
    private final RequestRepository requestRepository;
    private final int maxPendingRequests;

    private final Map<String, UrlStatus> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingRequests = new ConcurrentHashMap<>();

    public ModerationManager(JavaPlugin plugin, UrlRepository urlRepository, RequestRepository requestRepository, int maxPendingRequests) {
        this.plugin = plugin;
        this.urlRepository = urlRepository;
        this.requestRepository = requestRepository;
        this.maxPendingRequests = maxPendingRequests;
    }

    public UrlStatus check(String hash) {
        UrlStatus cached = cache.get(hash);
        if (cached != null) {
            return cached;
        }

        try {
            UrlStatus status = urlRepository.getStatus(hash);

            if (status != UrlStatus.UNKNOWN) {
                cache.put(hash, status);
            }

            return status;
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
            return UrlStatus.UNKNOWN;
        }
    }

    public String createRequest(Player player, String command, String url, String hash) {
        if (isRateLimited(player)) {
            return null;
        }

        UUID uuid = player.getUniqueId();
        String id = UUID.randomUUID().toString();
        ModerationRequest request = new ModerationRequest(
                id,
                uuid.toString(),
                player.getName(),
                command,
                url,
                hash,
                RequestStatus.PENDING,
                System.currentTimeMillis()
        );

        pendingRequests.merge(uuid, 1, Integer::sum);
        try {
            requestRepository.insert(request);
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
        }

        return id;
    }

    public boolean approve(String requestId, String moderator) {
        try {
            Optional<ModerationRequest> optional = requestRepository.findById(requestId);
            if (optional.isEmpty()) {
                return false;
            }

            ModerationRequest req = optional.get();
            urlRepository.insertOrUpdate(
                    req.hash(),
                    req.url(),
                    UrlStatus.APPROVED,
                    moderator
            );

            cache.put(req.hash(), UrlStatus.APPROVED);
            requestRepository.updateStatus(requestId, RequestStatus.APPROVED);

            UUID uuid = UUID.fromString(req.playerUUID());
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.sendMessage(Color.GREEN + "Your image request has been approved by a moderator.");
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(player, req.command().substring(1))
                );
            }

            pendingRequests.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));

            return true;
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
            return false;
        }
    }

    public boolean reject(String requestId, String moderator) {
        try {
            Optional<ModerationRequest> optional = requestRepository.findById(requestId);
            if (optional.isEmpty()) {
                return false;
            }

            ModerationRequest req = optional.get();
            urlRepository.insertOrUpdate(
                    req.hash(),
                    req.url(),
                    UrlStatus.REJECTED,
                    moderator
            );

            cache.put(req.hash(), UrlStatus.REJECTED);
            requestRepository.updateStatus(requestId, RequestStatus.REJECTED);
            UUID uuid = UUID.fromString(req.playerUUID());

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Color.RED + "Your image request was rejected by a moderator.");
            }

            pendingRequests.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
            return true;
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
            return false;
        }
    }

    public void loadPendingCache() {
        try {
            var map = requestRepository.loadPendingCounts();
            for (var entry : map.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                pendingRequests.put(uuid, entry.getValue());
            }
            plugin.getLogger().info("Pending cache loaded!");
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
        }
    }

    public boolean isRateLimited(Player player) {
        UUID uuid = player.getUniqueId();
        int pending = pendingRequests.getOrDefault(uuid, 0);
        return pending >= maxPendingRequests;
    }
}