package com.tacz.guns.command.sub;

import com.tacz.guns.legacy.util.Components;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DebugCommand extends CommandBase {
    public static boolean DEBUG = false;
    private static final String DEBUG_NAME = "debug";
    /**
     * Upstream {@code debugCommand.then(PegboardStressCommand.get())}: the {@code pegboard} literal.
     */
    private static final String PEGBOARD_NAME = "pegboard";

    @Override
    public String getName() {
        return DEBUG_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz debug <true|false> | /tacz debug pegboard <count> <same|different> [x y z]";
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
        if (PEGBOARD_NAME.equals(args[0])) {
            PegboardStressCommand.execute(server, sender, Arrays.copyOfRange(args, 1, args.length));
            return;
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

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "true", "false", PEGBOARD_NAME);
        }
        if (PEGBOARD_NAME.equals(args[0])) {
            return PegboardStressCommand.getTabCompletions(server, sender, Arrays.copyOfRange(args, 1, args.length), targetPos);
        }
        return Collections.emptyList();
    }
}
