package com.tacz.guns.command;

import com.tacz.guns.command.sub.*;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.command.CommandTreeBase;

/**
 * 1.12.2: a {@link CommandTreeBase} with the sub commands as {@code CommandBase}s, registered in
 * {@link FMLServerStartingEvent} instead of brigadier's dispatcher.
 */
public class RootCommand extends CommandTreeBase {
    private static final String ROOT_NAME = "tacz";

    public RootCommand() {
        this.addSubcommand(new AttachmentLockCommand());
        this.addSubcommand(new DebugCommand());
        this.addSubcommand(new DummyAmmoCommand());
        this.addSubcommand(new OverwriteCommand());
        this.addSubcommand(new ReloadCommand());
        this.addSubcommand(new HideTooltipPartCommand());
        this.addSubcommand(new ConvertCommand());
        this.addSubcommand(new ConfigCommand());
    }

    public static void register(FMLServerStartingEvent event) {
        event.registerServerCommand(new RootCommand());
    }

    @Override
    public String getName() {
        return ROOT_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + ROOT_NAME + " <attachment_lock|debug|dummy|overwrite|reload|hide_tooltip_part|convert|config>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
