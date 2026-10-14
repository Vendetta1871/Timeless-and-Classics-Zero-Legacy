package com.tacz.guns.command.sub;

import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class ReloadCommand extends CommandBase {
    private static final String RELOAD_NAME = "reload";

    @Override
    public String getName() {
        return RELOAD_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        StopWatch watch = StopWatch.createStarted();
        {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                // 1.12.2: the command runs on the server thread, the resource reload must run on the client thread
                Minecraft.getMinecraft().addScheduledTask(ReloadCommand::reloadClient);
            } else {
                CommonAssetsManager.reloadAllPack();
            }
        }
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
//        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
//
//            if (OtherConfig.DEFAULT_PACK_DEBUG.get()) {
//                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_off"));
//                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_command.off"));
//            } else {
//                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_on"));
//                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_command.on"));
//                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.backup"));
//            }
//        }
//        GunMod.LOGGER.info("Model loading time: {} ms", time);
        sender.sendMessage(Components.translatable("commands.tacz.reload.success", time));
    }

    public static void reloadClient() {
        ClientAssetsManager.reloadAllPack();
    }
}
