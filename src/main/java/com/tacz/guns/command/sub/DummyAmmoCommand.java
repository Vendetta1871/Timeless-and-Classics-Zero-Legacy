package com.tacz.guns.command.sub;

import com.tacz.guns.api.item.IGun;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class DummyAmmoCommand extends CommandBase {
    private static final String DUMMY_NAME = "dummy";

    @Override
    public String getName() {
        return DUMMY_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz dummy <target> <dummyAmount>";
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
        List<Entity> entities = getEntityList(server, sender, args[0]);
        int amount = parseInt(args[1], 0);
        int cnt = 0;
        for (Entity entity : entities) {
            if (entity instanceof EntityLivingBase living) {
                ItemStack stack = living.getHeldItemMainhand();
                if (stack.getItem() instanceof IGun iGun) {
                    iGun.setDummyAmmoAmount(stack, amount);
                    cnt++;
                }
            }
        }
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, cnt);
    }
}
