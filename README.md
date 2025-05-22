# EasyStaff

A comprehensive staff management plugin for Minecraft servers.

## Features

- Punishment system (ban, mute, kick, warn)
- Staff mode with vanish functionality
- Report system
- Maintenance mode
- Custom server ping (player count text and hover)

## Custom Server Ping

The plugin now supports customizing the player count text and hover text in the server list.

### Requirements

- ProtocolLib plugin is required for full functionality

### Maintenance Mode Integration

When maintenance mode is enabled, the server ping will automatically change to show:
- Player count text: "Maintenance" (configurable)
- Hover text: The maintenance message from config
- When maintenance is disabled, the original settings are restored

### Commands

- `/serverping toggle` - Toggle custom player count display
- `/serverping settext <text>` - Set player count text
- `/serverping addhover <text>` - Add hover text line
- `/serverping removehover <line>` - Remove hover text line
- `/serverping listhover` - List all hover text lines
- `/serverping clearhover` - Clear all hover text lines
- `/serverping reload` - Reload settings from config

### Placeholders

You can use the following placeholders in your custom text:
- `{online}` - Number of online players
- `{max}` - Maximum player count

### Configuration

```yaml
server-ping:
  custom-player-count:
    # Enable or disable custom player count display
    enabled: false
    # Text to display in the player count
    # Use {online} for online players and {max} for max players
    text: "&a{online}/{max} &fPlayers"
    # Text to display when hovering over the player count
    # Use {online} for online players and {max} for max players
    hover-text:
      - "&fWelcome to our server!"
      - "&fCurrently &a{online}&f players online"
      - "&fJoin us now!"
  custom-motd:
    # Enable or disable custom MOTD (Message of the Day)
    # This is a fallback for when ProtocolLib is not available
    enabled: false
    # Text to display in the MOTD
    text:
      - "&6&lYour Server Name"
      - "&fA unique Minecraft experience!"

maintenance:
  # If maintenance is enabled
  enabled: false
  # Players that can join while maintenance being enabled
  exempt:
    - Notch
    - billy_bob_123
  # The message that you get if you join while maintenance being enabled.
  # This will also be used for the hover text in the server list
  message:
    - "&cMaintenance!"
    - ""
    - "&fMaintenance has been enabled please be patient while we resolve the issues"
    - "&fJoin our discord for updates: &bdiscord.gg/"
  # Server ping settings for maintenance mode
  server-ping:
    # Text to display in the player count during maintenance
    # This will override the normal player count text
    text: "&c&lMaintenance"
```

### Permissions

- `easystaff.serverping` - Access to all server ping commands