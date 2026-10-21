package de.rexlcloud.example.proxy.common;

import de.rexlcloud.core.api.MessageChannel;
import de.rexlcloud.core.api.RexlCloudApi;
import de.rexlcloud.core.api.Server;
import de.rexlcloud.core.api.exceptions.NoConnectionException;
import de.rexlcloud.core.api.exceptions.PlayerNotFoundException;
import de.rexlcloud.core.api.exceptions.ServerNotFoundException;
import de.rexlcloud.core.api.exceptions.TimeOutException;
import de.rexlcloud.core.server.ServerType;
import de.rexlcloud.example.shared.Announcement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The part of the example that only talks to the cloud, the same on Velocity and BungeeCord. The platform classes
 * ({@code ExampleVelocityPlugin}, {@code ExampleBungeePlugin}) turn it into commands and events.
 * <p>
 * Texts are MiniMessage; each platform shows them its own way.
 */
public final class ExampleProxyCore {

    /**
     * What the core needs from the proxy it runs on.
     */
    public interface Platform {
        /**
         * Shows a MiniMessage text to every player on this proxy.
         */
        void broadcast(String miniMessage);
    }

    private final MessageChannel<Announcement> announcements;

    public ExampleProxyCore(Platform platform) {
        // Message channels can be registered right away, also before the cloud plugin is connected
        announcements = RexlCloudApi.getMessageChannelManager()
                .registerMessageChannel(Announcement.CHANNEL, Announcement.class);
        // /announce of the Paper example: shown to everyone on this proxy, and so (with every proxy doing the same)
        // to the whole network
        announcements.addListener((from, announcement) -> platform.broadcast(
                "<gold>[" + announcement.sender() + " on " + announcement.from() + "]</gold> " + announcement.message()));
    }

    public void close() {
        RexlCloudApi.getMessageChannelManager().unregisterMessageChannel(announcements);
    }

    /**
     * {@code /network}: every group with its servers, from the API's cache (no request to the master).
     */
    public List<String> network() {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return List.of(notConnected());
        }
        var lines = new ArrayList<String>();
        lines.add("<aqua>Network seen from " + cloud.getServerName() + ": <white>"
                + cloud.getCachedPlayers().size() + " <aqua>players online");
        cloud.getCachedServers().stream()
                .sorted(Comparator.comparing((Server server) -> server.getServerGroup().getName())
                        .thenComparing(Server::getName))
                .forEach(server -> lines.add((server.isOnline() ? "<green>" : "<yellow>") + server.getName()
                        + " <gray>" + server.getServerGroup().getName() + ", " + server.getState()
                        + " on " + server.getWrapper().getName()));
        return lines;
    }

    /**
     * {@code /find <player>}: on which server and proxy a player of the whole network is.
     */
    public CompletableFuture<String> find(String playerName) {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return CompletableFuture.completedFuture(notConnected());
        }
        return cloud.getPlayer(playerName)
                .thenApply(player -> "<aqua>" + player.getName() + " <gray>is on <white>" + player.getServer().getName()
                        + " <gray>(via " + player.getProxyServer().getName() + ")")
                .exceptionally(ExampleProxyCore::describe);
    }

    /**
     * {@code /send <player> <server>}: moves a player to a server, even if the player is on another proxy; the
     * cloud asks that player's proxy to do it.
     */
    public CompletableFuture<String> send(String playerName, String serverName) {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return CompletableFuture.completedFuture(notConnected());
        }
        return cloud.getPlayer(playerName)
                .thenCompose(player -> player.connect(serverName))
                .thenApply(ok -> "<green>Sending " + playerName + " to " + serverName)
                .exceptionally(ExampleProxyCore::describe);
    }

    /**
     * {@code /alert <message>}: sends an {@link Announcement} to every server; the Paper example shows it to its
     * players.
     */
    public String alert(String sender, String message) {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return notConnected();
        }
        var servers = cloud.getCachedServers().stream()
                .filter(Server::isOnline)
                .filter(server -> server.getServerGroup().getType() == ServerType.SERVER)
                .toArray(Server[]::new);
        if (servers.length == 0) {
            return "<red>No server is online.";
        }
        announcements.sendMessage(new Announcement(cloud.getServerName(), sender, message), servers);
        return "<green>Sent to " + servers.length + " server" + (servers.length == 1 ? "" : "s") + ".";
    }

    /**
     * Shown to a player after they switched servers: the group of the new server, from the cache.
     */
    public String switched(String serverName) {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return null;
        }
        return cloud.getCachedServer(serverName)
                .map(server -> "<gray>You are on <aqua>" + server.getName() + "</aqua> (group "
                        + server.getServerGroup().getName() + ")")
                .orElse(null);
    }

    /**
     * Server names for tab completion.
     */
    public List<String> serverNames(String prefix) {
        var cloud = RexlCloudApi.getInstance();
        if (cloud == null) {
            return List.of();
        }
        return cloud.getCachedServers().stream()
                .map(Server::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .toList();
    }

    private static String notConnected() {
        return "<red>This proxy is not connected to the cloud yet, try again in a moment.";
    }

    private static String describe(Throwable error) {
        // CompletableFuture wraps the real exception
        var cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return switch (cause) {
            case PlayerNotFoundException e -> "<red>That player is not online.";
            case ServerNotFoundException e -> "<red>There is no such server.";
            case NoConnectionException e -> "<red>The proxy lost its connection to the cloud.";
            case TimeOutException e -> "<red>The cloud did not answer in time.";
            default -> "<red>Something went wrong: " + cause.getMessage();
        };
    }

}
