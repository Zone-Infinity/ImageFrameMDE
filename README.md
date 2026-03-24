# ImageFrameMDE

ImageFrame Moderation on Discord Extension

ImageFrameMDE is a moderation extension for
the [ImageFrame Minecraft plugin](https://www.spigotmc.org/resources/imageframe-load-images-on-maps-item-frames-support-gifs-map-markers-survival-friendly.106031/).
It intercepts image-loading commands and sends them to Discord for moderator approval
before allowing the image to load in-game.

Supports **Minecraft 1.21.11**.


---

## Features

- Intercepts ImageFrame commands that include image URLs
- Sends moderation requests to Discord
- One-click **Approve / Reject** buttons for moderators
- Image preview shown in the moderation message
- Approved images load instantly without re-moderation
- Configurable rate limit for pending requests
- SQLite database for moderation decisions

---

## Requirements

- Minecraft server **1.21.11**
- Paper / Spigot
- Java **21**
- ImageFrame plugin
- Discord bot token

---

## Installation

1. Download the plugin `.jar`
2. Place it inside the server `plugins` folder
3. Start the server once to generate the config
4. Configure the Discord bot token and moderation channel
5. Restart the server

---

## Configuration Example

config.yml

moderation:
max-pending-requests-per-player: 3

discord:
token: "YOUR_BOT_TOKEN"
moderation-channel-id: 123456789012345678


---

## Moderation Flow

1. Player runs an ImageFrame command containing an image URL
2. Plugin intercepts the command
3. If the image has not been moderated:
    - A request is sent to Discord
    - The command is blocked
4. A moderator approves or rejects the request
5. Approved images can then be used normally

---

## Database

The plugin uses SQLite and stores two types of data.

urls
Stores the final moderation decision for an image.

requests
Stores moderation request history.


---

## Dependencies

- [ImageFrame](https://github.com/LOOHP/ImageFrame)

- [JDA (Java Discord API)](https://github.com/discord-jda/JDA)

---

## License

MIT License
Copyright (c) 2026 iSoham