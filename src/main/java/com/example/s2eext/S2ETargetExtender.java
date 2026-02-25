package com.example.s2eext;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class S2ETargetExtender extends JavaPlugin implements CommandExecutor, TabCompleter {

    private TargetManager targetManager;

    @Override
    public void onEnable() {
        targetManager = new TargetManager(this);
        getServer().getPluginManager().registerEvents(new TargetListener(targetManager), this);

        if (getCommand("td") != null) {
            getCommand("td").setExecutor(this);
            getCommand("td").setTabCompleter(this);
        }

        targetManager.checkInitialTargets();
        getLogger().info("S2E Target Extender enabled.");
    }

    @Override
    public void onDisable() {
        if (targetManager != null) {
            targetManager.stopGeneration();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("s2eext.admin")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("target")) {
            sender.sendMessage("§eUsage: /td target set <number> | /td target count");
            return true;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("count")) {
            sender.sendMessage("§aCurrent tracked target count: §f" + targetManager.getCurrentTargetCount());
            sender.sendMessage("§aConfigured target amount: §f" + targetManager.getTargetAmount());
            return true;
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            try {
                int newValue = Integer.parseInt(args[2]);
                boolean accepted = targetManager.updateTargetAmount(newValue);
                if (!accepted) {
                    sender.sendMessage("§cInvalid amount. Allowed range: 1-" + targetManager.getMaxTargetSlots());
                    return true;
                }
                sender.sendMessage("§aTarget amount updated to §f" + newValue + "§a. Regenerating...");
                targetManager.regenerateToConfiguredAmount();
                return true;
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cPlease input a number.");
                return true;
            }
        }

        sender.sendMessage("§eUsage: /td target set <number> | /td target count");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("target");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("target")) {
            return List.of("set", "count");
        }
        return List.of();
    }
}
