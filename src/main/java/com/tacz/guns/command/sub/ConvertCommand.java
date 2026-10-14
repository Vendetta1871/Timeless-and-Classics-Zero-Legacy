package com.tacz.guns.command.sub;

import com.tacz.guns.resource.PackConvertor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ConvertCommand extends CommandBase {
    private static final String CONVERT_NAME = "convert";

    @Override
    public String getName() {
        return CONVERT_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz convert";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            PackConvertor.convert(sender);
        }
    }
}
