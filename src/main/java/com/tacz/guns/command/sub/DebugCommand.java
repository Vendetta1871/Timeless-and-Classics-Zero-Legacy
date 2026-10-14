package com.tacz.guns.command.sub;

import com.tacz.guns.legacy.util.Components;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class DebugCommand extends CommandBase {
    public static boolean DEBUG = false;
    private static final String DEBUG_NAME = "debug";

    @Override
    public String getName() {
        return DEBUG_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz debug <true|false>";
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
        DEBUG = parseBoolean(args[0]);
        if (sender.getCommandSenderEntity() instanceof EntityPlayerMP serverPlayer) {
            if (DEBUG) {
                serverPlayer.sendMessage(Components.literal("TacZ Debug Mode is Turn On"));
            } else {
                serverPlayer.sendMessage(Components.literal("TacZ Debug Mode is Turn Off"));
            }
        }
    }
}
