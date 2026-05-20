# LightReboot

LightReboot is a Paper plugin for safe, configurable server restarts. It gives staff a clean reboot command, countdown warnings, optional schedules, and a controlled shutdown flow that saves data before calling `Bukkit.shutdown()`.

## Features

- Manual reboot countdowns
- Configurable scheduled reboots
- Broadcast, title, actionbar, and bossbar countdown messages
- Reload, status, cancel, and preview commands
- Safe duplicate-countdown prevention
- Config validation with clean console warnings
- Optional plugin-disable list before shutdown
- Optional PlaceholderAPI support in player messages
- GitHub Actions build and release workflows

## Requirements

- Paper 1.20.4 or compatible server
- Java 17
- Maven 3.9+ to build from source

## Installation

1. Download the jar from Releases.
2. Put it in your server `plugins` folder.
3. Restart the server.
4. Edit `plugins/LightReboot/config.yml`.
5. Run `/lightreboot reload`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/lightreboot start [time] [reason]` | `lightreboot.start` | Start a reboot countdown |
| `/lightreboot cancel` | `lightreboot.cancel` | Cancel the active countdown |
| `/lightreboot status` | `lightreboot.status` | Show reboot status |
| `/lightreboot preview [time]` | `lightreboot.preview` | Preview title/actionbar/broadcast messages |
| `/lightreboot reload` | `lightreboot.reload` | Reload `config.yml` |
| `/lightreboot help` | none | Show command help |

Aliases: `/reboot`, `/lreboot`

The old short form still works:

```text
/reboot 60
```

Time values support seconds or shorthand:

```text
30
30s
5m
1h
```

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `lightreboot.start` | op | Start reboot countdowns |
| `lightreboot.cancel` | op | Cancel active countdowns |
| `lightreboot.status` | op | View reboot status |
| `lightreboot.preview` | op | Preview countdown messages |
| `lightreboot.reload` | op | Reload configuration |
| `lightreboot.admin` | op | Includes all LightReboot permissions |

## Configuration

Example schedule:

```yaml
schedule:
  enabled: true
  timezone: "UTC"
  days:
    - MONDAY
    - TUESDAY
    - WEDNESDAY
    - THURSDAY
    - FRIDAY
    - SATURDAY
    - SUNDAY
  times:
    - "03:00"
```

Countdown channels can be enabled or disabled separately:

```yaml
countdown:
  warning-times:
    - 300
    - 120
    - 60
    - 30
    - 10
    - 5
    - 4
    - 3
    - 2
    - 1

  broadcast:
    enabled: true
    message: "&8[&eRestart&8] &7Server restart in &e{time}&7."

  title:
    enabled: true
    title: "&cRestarting Soon"
    subtitle: "&7Restart in &e{time}&7."

  actionbar:
    enabled: true
    message: "&eRestart in &c{time} &8| &7Online: &f{online}"

  bossbar:
    enabled: true
    message: "&eServer restart in &c{time}"
    color: YELLOW
    style: SOLID
```

Available built-in placeholders:

| Placeholder | Meaning |
| --- | --- |
| `{time}` | Friendly time, such as `5m` or `30s` |
| `{seconds}` | Raw seconds left |
| `{online}` | Current online player count |
| `{reason}` | Manual or scheduled reason |
| `{player}` | Player or command sender name where relevant |

PlaceholderAPI placeholders are parsed for player-facing title and actionbar messages when PlaceholderAPI is installed.

## Building

```bash
mvn clean package
```

The jar is created in `target/lightreboot-<version>.jar`.

## License

MIT. See `LICENSE`.
