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

public class AttachmentLockCommand extends CommandBase {
    private static final String ATTACHMENT_LOCK_NAME = "attachment_lock";

    @Override
    public String getName() {
        return ATTACHMENT_LOCK_NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tacz attachment_lock <target> <true|false>";
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
        boolean locked = parseBoolean(args[1]);
        int cnt = 0;
        for (Entity entity : entities) {
            if (entity instanceof EntityLivingBase living) {
                ItemStack stack = living.getHeldItemMainhand();
                if (stack.getItem() instanceof IGun iGun) {
                    iGun.setAttachmentLock(stack, locked);
                    cnt++;
                }
            }
        }
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, cnt);
    }
}
