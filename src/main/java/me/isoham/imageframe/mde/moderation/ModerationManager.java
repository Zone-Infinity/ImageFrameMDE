package me.isoham.imageframe.mde.moderation;

import com.loohp.imageframe.ImageFrame;
import com.loohp.imageframe.objectholders.ImageMap;
import me.isoham.imageframe.mde.config.Config;
import me.isoham.imageframe.mde.config.MessageConfig;
import me.isoham.imageframe.mde.storage.*;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moderation decisions are reversible.
 * <p>
 * If a request is rejected after being approved,
 * the created ImageFrame map will be deleted to
 * ensure rejected images do not remain in-game.
 */

public class ModerationManager {
    private final JavaPlugin plugin;
    private final URLRepository urlRepository;
    private final RequestRepository requestRepository;
    private final Config config;

    private final Map<String, URLStatus> cache = new ConcurrentHashMap<>();
    private final Set<String> pendingHashes = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> pendingRequests = new ConcurrentHashMap<>();

    public ModerationManager(JavaPlugin plugin, URLRepository urlRepository, RequestRepository requestRepository, Config config) {
        this.plugin = plugin;
        this.urlRepository = urlRepository;
        this.requestRepository = requestRepository;
        this.config = config;
    }

    public URLStatus check(String hash) {
        URLStatus cached = cache.get(hash);
        if (cached != null) {
            return cached;
        }

        if (pendingHashes.contains(hash)) {
            return URLStatus.PENDING;
        }

        try {
            URLStatus status = urlRepository.getStatus(hash);

            if (status != URLStatus.UNKNOWN) {
                cache.put(hash, status);
            }

            return status;
        } catch (Exception e) {
            // TODO: Proper logging
            e.printStackTrace();
            return URLStatus.UNKNOWN;
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
        pendingHashes.add(hash);
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
            if (req.status() == RequestStatus.APPROVED) {
                plugin.getLogger().warning("Request " + requestId + " already approved: " + req.url());
                return false;
            }

            urlRepository.insertOrUpdate(
                    req.hash(),
                    req.url(),
                    URLStatus.APPROVED,
                    moderator
            );

            cache.put(req.hash(), URLStatus.APPROVED);
            requestRepository.updateStatus(requestId, RequestStatus.APPROVED);

            UUID uuid = UUID.fromString(req.playerUUID());
            Player player = Bukkit.getPlayer(uuid);

            // TODO: Store this and send it to player when they join!
            if (player != null) {
                player.sendMessage(config.getMessages().get(MessageConfig.Message.MODERATION_APPROVED));
                Bukkit.getScheduler().runTask(plugin, () ->
                        {
                            try {
                                if(!Bukkit.dispatchCommand(player, req.command().substring(1))) {
                                    plugin.getLogger().warning("No target found while dispatching command: " + player.getName());
                                }
                            } catch (CommandException e) {
                                plugin.getLogger().warning("Exception while dispatching command, Report!");
                                e.printStackTrace();
                            }
                        }
                );
            }

            pendingRequests.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
            pendingHashes.remove(req.hash());

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
            if (req.status() == RequestStatus.REJECTED) {
                plugin.getLogger().warning("Request " + requestId + " already rejected: " + req.url());
                return false;
            }

            // If request was previously accepted, delete the image
            if (req.status() == RequestStatus.APPROVED) {
                String[] args = req.command().split("\\s+");
                if (args.length >= 3) {
                    String sub = args[1];
                    if (sub.equalsIgnoreCase("create") || sub.equalsIgnoreCase("overlay")) {
                        String name = args[2];
                        UUID creator = UUID.fromString(req.playerUUID());

                        try {
                            ImageMap map = ImageFrame.imageMapManager.getFromCreator(creator, name);
                            if (map != null) {
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        ImageFrame.imageMapManager.deleteMap(map.getImageIndex())
                                );
                            }
                        } catch (Exception ignored) {
                            // map might not exist yet or was already deleted
                        }
                    }
                }
            }

            urlRepository.insertOrUpdate(
                    req.hash(),
                    req.url(),
                    URLStatus.REJECTED,
                    moderator
            );

            cache.put(req.hash(), URLStatus.REJECTED);
            requestRepository.updateStatus(requestId, RequestStatus.REJECTED);
            UUID uuid = UUID.fromString(req.playerUUID());

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(config.getMessages().get(MessageConfig.Message.MODERATION_REJECTED));
            }

            pendingRequests.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
            pendingHashes.remove(req.hash());

            return true;
        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
            return false;
        }
    }

    public void loadStartupCaches() {
        try {
            requestRepository.loadPendingCache(pendingRequests, pendingHashes);

            plugin.getLogger().info("Moderation caches loaded! Pending players: "
                    + pendingRequests.size()
                    + ", pending URLs: "
                    + pendingHashes.size());

        } catch (Exception e) {
            // TODO: Log this properly
            e.printStackTrace();
        }
    }

    public boolean isRateLimited(Player player) {
        UUID uuid = player.getUniqueId();
        int pending = pendingRequests.getOrDefault(uuid, 0);
        return pending >= config.getMaxPendingRequestsPerPlayer();
    }
}