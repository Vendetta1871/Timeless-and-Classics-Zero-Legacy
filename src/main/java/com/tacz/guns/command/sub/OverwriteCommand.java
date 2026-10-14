package com.tacz.guns.command.sub;

import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class OverwriteCommand extends CommandBase {
    private static final String OVERWRITE_NAME = "overwrite";

    @Override
    public String getName() {
        return OVERWRITE_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz overwrite <true|false>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        boolean enable = parseBoolean(args[0]);
        PreLoadConfig.override.set(!enable);
        // 1.12.2: the config is not written back by itself (no ModConfig)
        PreLoadConfig.save();
        if (sender.getCommandSenderEntity() instanceof EntityPlayerMP serverPlayer) {
            if (PreLoadConfig.override.get()) {
                serverPlayer.sendMessage(Components.translatable("commands.tacz.reload.overwrite_off"));
            } else {
                serverPlayer.sendMessage(Components.translatable("commands.tacz.reload.overwrite_on"));
            }
        }
    }
}
