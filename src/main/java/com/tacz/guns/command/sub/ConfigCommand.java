package com.tacz.guns.command.sub;

import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigCommand extends CommandBase {
    private static final String CONFIG_NAME = "config";

    @Override
    public String getName() {
        return CONFIG_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz config <defaultTableLimit|serverShootNetworkCheck|serverShootCooldownCheck> <true|false>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        ConfigKey key = null;
        for (ConfigKey value : ConfigKey.values()) {
            if (value.name().equals(args[0])) {
                key = value;
            }
        }
        if (key == null) {
            throw new WrongUsageException(getUsage(sender));
        }
        boolean state = parseBoolean(args[1]);

        switch (key) {
            case defaultTableLimit -> SyncConfig.ENABLE_TABLE_FILTER.set(state);
            case serverShootNetworkCheck -> SyncConfig.SERVER_SHOOT_NETWORK_V.set(state);
            case serverShootCooldownCheck -> SyncConfig.SERVER_SHOOT_COOLDOWN_V.set(state);
        }
        sender.sendMessage(Components.translatable(key.lang + "." + (state ? "enabled" : "disabled")));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (ConfigKey key : ConfigKey.values()) {
                names.add(key.name());
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    public enum ConfigKey {
        defaultTableLimit("commands.tacz.config.default_table_limit"),
        serverShootNetworkCheck("commands.tacz.config.server_shoot_network_check"),
        serverShootCooldownCheck("commands.tacz.config.server_shoot_cooldown_check"),
        ;

        public final String lang;
        ConfigKey(String lang) {
            this.lang = lang;
        }
    }
}
