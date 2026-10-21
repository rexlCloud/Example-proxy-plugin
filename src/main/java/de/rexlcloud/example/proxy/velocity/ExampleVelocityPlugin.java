package de.rexlcloud.example.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.rexlcloud.example.proxy.common.ExampleProxyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The example on Velocity: commands and events of Velocity, the cloud part in {@link ExampleProxyCore}.
 */
@Plugin(id = "rexlcloud_example", name = "ExampleCloudProxyPlugin", version = "1.0.0",
        description = "Shows how a proxy plugin uses the rexlCloud API", authors = {"rexlCloud"},
        // The cloud's proxy plugin has to be loaded first
        dependencies = @Dependency(id = "rexlcloud_proxy"))
public final class ExampleVelocityPlugin {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ProxyServer proxy;
    private ExampleProxyCore core;

    @Inject
    public ExampleVelocityPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        core = new ExampleProxyCore(text -> proxy.sendMessage(MINI_MESSAGE.deserialize(text)));

        register("network", "example.network", (source, args) -> core.network().forEach(line -> send(source, line)));
        register("find", "example.find", (source, args) -> {
            if (args.length != 1) {
                send(source, "<red>Usage: /find <player>");
                return;
            }
            core.find(args[0]).thenAccept(text -> send(source, text));
        });
        register("send", "example.send", (source, args) -> {
            if (args.length != 2) {
                send(source, "<red>Usage: /send <player> <server>");
                return;
            }
            core.send(args[0], args[1]).thenAccept(text -> send(source, text));
        });
        register("alert", "example.alert", (source, args) -> {
            if (args.length == 0) {
                send(source, "<red>Usage: /alert <message>");
                return;
            }
            var sender = source instanceof com.velocitypowered.api.proxy.Player player ? player.getUsername() : "Console";
            send(source, core.alert(sender, String.join(" ", args)));
        });
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        var player = event.getPlayer();
        player.getCurrentServer().ifPresent(connection -> {
            var text = core.switched(connection.getServerInfo().getName());
            if (text != null) {
                send(player, text);
            }
        });
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.close();
        }
    }

    private void send(CommandSource source, String miniMessage) {
        source.sendMessage(MINI_MESSAGE.deserialize(miniMessage));
    }

    private void register(String name, String permission, BiConsumer<CommandSource, String[]> action) {
        var commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder(name).plugin(this).build(), new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                action.accept(invocation.source(), invocation.arguments());
            }

            @Override
            public boolean hasPermission(Invocation invocation) {
                return invocation.source().hasPermission(permission);
            }

            @Override
            public List<String> suggest(Invocation invocation) {
                var args = invocation.arguments();
                // The server argument of /send
                if (name.equals("send") && args.length == 2) {
                    return core.serverNames(args[1]);
                }
                return List.of();
            }
        });
    }

}
