package de.rexlcloud.example.proxy.bungee;

import de.rexlcloud.example.proxy.common.ExampleProxyCore;
import de.rexlcloud.proxy.bungee.BungeeText;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.event.EventHandler;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The example on BungeeCord: commands and events of BungeeCord, the cloud part in {@link ExampleProxyCore}.
 * BungeeCord's own chat API has no MiniMessage; {@link BungeeText} from the cloud's proxy plugin converts Adventure
 * components for it.
 */
public final class ExampleBungeePlugin extends Plugin implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ExampleProxyCore core;

    @Override
    public void onEnable() {
        core = new ExampleProxyCore(text -> {
            var message = BungeeText.of(MINI_MESSAGE.deserialize(text));
            getProxy().getPlayers().forEach(player -> player.sendMessage(message));
        });

        register("network", "example.network", (sender, args) -> core.network().forEach(line -> send(sender, line)));
        register("find", "example.find", (sender, args) -> {
            if (args.length != 1) {
                send(sender, "<red>Usage: /find <player>");
                return;
            }
            core.find(args[0]).thenAccept(text -> send(sender, text));
        });
        register("send", "example.send", (sender, args) -> {
            if (args.length != 2) {
                send(sender, "<red>Usage: /send <player> <server>");
                return;
            }
            core.send(args[0], args[1]).thenAccept(text -> send(sender, text));
        });
        register("alert", "example.alert", (sender, args) -> {
            if (args.length == 0) {
                send(sender, "<red>Usage: /alert <message>");
                return;
            }
            var name = sender instanceof ProxiedPlayer player ? player.getName() : "Console";
            send(sender, core.alert(name, String.join(" ", args)));
        });
        getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.close();
        }
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        var text = core.switched(event.getServer().getInfo().getName());
        if (text != null) {
            send(event.getPlayer(), text);
        }
    }

    private static void send(CommandSender sender, String miniMessage) {
        BungeeText.send(sender, MINI_MESSAGE.deserialize(miniMessage));
    }

    private void register(String name, String permission, BiConsumer<CommandSender, String[]> action) {
        getProxy().getPluginManager().registerCommand(this, new ExampleCommand(name, permission, action));
    }

    private final class ExampleCommand extends Command implements TabExecutor {

        private final BiConsumer<CommandSender, String[]> action;

        ExampleCommand(String name, String permission, BiConsumer<CommandSender, String[]> action) {
            super(name, permission);
            this.action = action;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            action.accept(sender, args);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            // The server argument of /send
            if (getName().equals("send") && args.length == 2) {
                return core.serverNames(args[1]);
            }
            return List.of();
        }
    }

}
