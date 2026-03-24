package me.isoham.imageframe.mde.listeners;

import com.loohp.imageframe.api.events.ImageMapAddedEvent;
import com.loohp.imageframe.api.events.ImageMapUpdatedEvent;
import com.loohp.imageframe.objectholders.ImageMap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ImageMapListener implements Listener {

    @EventHandler
    public void onImageMapAdded(ImageMapAddedEvent event) {
        ImageMap map = event.getImageMap();

        Bukkit.getLogger().info(
                "[ImageFrameMDE] Image map created: "
                        + map.getName()
                        + " (index=" + map.getImageIndex() + ")"
        );
    }

    @EventHandler
    public void onImageMapUpdated(ImageMapUpdatedEvent event) {
        ImageMap map = event.getImageMap();

        Bukkit.getLogger().warning(
                "[ImageFrameMDE] Image map updated: "
                        + map.getName()
                        + " (index=" + map.getImageIndex() + ")"
                        + " — ensure URL moderation still applies."
        );
    }
}