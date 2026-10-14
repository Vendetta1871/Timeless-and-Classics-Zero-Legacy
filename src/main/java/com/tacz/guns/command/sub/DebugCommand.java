package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.util.Components;

public class DebugCommand {
    public static boolean DEBUG = false;
    private static final String DEBUG_NAME = "debug";
    private static final String ENABLE = "enable";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal(DEBUG_NAME);
        RequiredArgumentBuilder<CommandSourceStack, Boolean> enable = Commands.argument(ENABLE, BoolArgumentType.bool());
        debugCommand.then(enable.executes(DebugCommand::setValue));
        return debugCommand;
    }

    private static int setValue(CommandContext<CommandSourceStack> context) {
        DEBUG = BoolArgumentType.getBool(context, ENABLE);
        if (context.getSource().getEntity() instanceof EntityPlayerMP serverPlayer) {
            if (DEBUG) {
                serverPlayer.sendSystemMessage(Components.literal("TacZ Debug Mode is Turn On"));
            } else {
                serverPlayer.sendSystemMessage(Components.literal("TacZ Debug Mode is Turn Off"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
