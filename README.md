# rexlCloud example: proxy plugin (Velocity and BungeeCord)

A proxy plugin that uses the [rexlCloud](https://rexlcloud.de) API, as **one jar for Velocity and BungeeCord**.
It lists the network, finds and moves players across all proxies, and talks to the servers over a message
channel.

Every proxy the cloud starts gets the cloud's proxy plugin, which connects to the cloud and offers `RexlCloudApi`.
This plugin only depends on it (`rexlcloud_proxy` on Velocity, `rexlCloudProxy` on BungeeCord).

## Commands

| Command | Permission | Shows |
| --- | --- | --- |
| `/network` | `example.network` | Every server with group, state and wrapper, from the API's cache |
| `/find <player>` | `example.find` | Where a player of the whole network is, also on other proxies |
| `/send <player> <server>` | `example.send` | Moving a player to a server, wherever they are; tab completes the servers |
| `/alert <message>` | `example.alert` | Sending an object over a message channel to every server; the [Paper example](https://github.com/rexlCloud/Example-paper-plugin) shows it to its players |

The other way round, it shows the Paper example's `/announce` messages to every player on this proxy. After a
server switch it tells the player which server and group they are on.

## Build

Needs Java 25 (Gradle downloads it if it is missing):

```bash
./gradlew build
```

The plugin is `build/libs/Example-proxy-plugin-1.0.0.jar`. Put it into the template of your proxy group, e.g.
`templates/Proxy/default/plugins/` on the master, or into `templates/global/proxy/plugins/` for every proxy. The
same jar works on Velocity (it reads `velocity-plugin.json`) and BungeeCord (`bungee.yml`).

## Where to look

| File | What it shows |
| --- | --- |
| [`common/ExampleProxyCore`](src/main/java/de/rexlcloud/example/proxy/common/ExampleProxyCore.java) | All of the cloud code, the same on both proxies: cache, requests, message channel, errors |
| [`velocity/ExampleVelocityPlugin`](src/main/java/de/rexlcloud/example/proxy/velocity/ExampleVelocityPlugin.java) | Velocity: `@Plugin` with the dependency, commands, `ServerPostConnectEvent` |
| [`bungee/ExampleBungeePlugin`](src/main/java/de/rexlcloud/example/proxy/bungee/ExampleBungeePlugin.java) | BungeeCord: commands, `ServerConnectedEvent`, MiniMessage through the cloud's `BungeeText` |
| [`shared/Announcement`](src/main/java/de/rexlcloud/example/shared/Announcement.java) | The object sent; the Paper example has the same record |

## Good to know

- `RexlCloudApi.getInstance()` is `null` until the cloud's proxy plugin has connected, shortly after the start.
- Requests to the master return a `CompletableFuture`; Velocity and BungeeCord accept messages from any thread.
- BungeeCord works with the cloud's legacy forwarding (`forwardingMode` `LEGACY` on the master).
- `rexlCloudVersion` in `build.gradle` should match your cloud's version.

Docs: [plugin guide](https://rexlcloud.de/docs/plugins) · [Proxy API Javadoc](https://rexlcloud.de/api/proxy-api/)
· [Module API Javadoc](https://rexlcloud.de/api/module-api/) (the shared classes: `RexlCloudApi`, `Player`, `Server`)

## Licence

[MIT](LICENSE): copy what you need into your own plugins.
