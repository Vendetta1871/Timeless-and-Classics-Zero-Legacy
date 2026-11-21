package com.tacz.guns.command.sub;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 1.12.2: upstream's brigadier node {@code pegboard <count> <same|different> [pos]} under {@code /tacz debug}; the
 * arguments come from {@link DebugCommand} (without the {@code pegboard} literal).
 */
public final class PegboardStressCommand {
    private static final int MAX_GUNS = 16_384;
    private static final int BOARDS_PER_ROW = 32;
    private static final int[] DISPLAY_SLOTS = {1, 3, 9, 11};
    private static final String SAME = "same";
    private static final String DIFFERENT = "different";
    private static final String USAGE = "/tacz debug pegboard <count> <same|different> [x y z]";

    private PegboardStressCommand() {
    }

    public static void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2 && args.length != 5) {
            throw new WrongUsageException(USAGE);
        }
        int requested = CommandBase.parseInt(args[0], 1, MAX_GUNS);
        boolean same;
        if (SAME.equals(args[1])) {
            same = true;
        } else if (DIFFERENT.equals(args[1])) {
            same = false;
        } else {
            throw new WrongUsageException(USAGE);
        }
        BlockPos pos = args.length == 5 ? CommandBase.parseBlockPos(sender, args, 2, false) : null;
        generate(sender, requested, same, pos);
    }

    public static List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 2) {
            return CommandBase.getListOfStringsMatchingLastWord(args, SAME, DIFFERENT);
        }
        if (args.length >= 3 && args.length <= 5) {
            return CommandBase.getTabCompletionCoordinate(args, 2, targetPos);
        }
        return Collections.emptyList();
    }

    private static int generate(ICommandSender source, int requested, boolean same, @Nullable BlockPos pos) {
        World level = source.getEntityWorld();

        // The index sort is the order used by the creative gun list; the id makes ties deterministic.
        List<ItemStack> guns = TimelessAPI.getAllCommonGunIndex().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, CommonGunIndex> entry) -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(entry -> GunItemBuilder.create()
                        .setId(entry.getKey())
                        .setFireMode(entry.getValue().getGunData().getFireModeSet().get(0))
                        .build())
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toList());
        if (guns.isEmpty()) {
            sendFailure(source, "commands.tacz.debug.pegboard.no_guns");
            return 0;
        }

        int boardCount = (requested + DISPLAY_SLOTS.length - 1) / DISPLAY_SLOTS.length;
        int columns = Math.min(BOARDS_PER_ROW, boardCount);
        Entity entity = source.getCommandSenderEntity();
        EnumFacing forward = EnumFacing.fromAngle(entity != null ? entity.rotationYaw : 0.0F);
        EnumFacing right = forward.rotateY();
        EnumFacing facing = forward.getOpposite();
        BlockPos origin = pos != null
                ? pos
                : new BlockPos(source.getPositionVector()).offset(forward, 4).up().offset(right, -(columns - 1) / 2);

        List<BlockPos> positions = new ArrayList<>(boardCount);
        for (int i = 0; i < boardCount; i++) {
            BlockPos boardPos = origin.offset(right, i % columns).up(i / columns);
            if (boardPos.getY() < 0 || boardPos.getY() >= level.getHeight()
                    || !level.isBlockLoaded(boardPos) || !level.isAirBlock(boardPos)) {
                sendFailure(source, "commands.tacz.debug.pegboard.blocked", toShortString(boardPos));
                return 0;
            }
            positions.add(boardPos);
        }

        IBlockState state = ModBlocks.GUN_PEGBOARD.get().getDefaultState().withProperty(GunPegboardBlock.FACING, facing);
        int placed = 0;
        for (int board = 0; board < boardCount; board++) {
            BlockPos boardPos = positions.get(board);
            // Block.UPDATE_CLIENTS
            if (!level.setBlockState(boardPos, state, 2)
                    || !(level.getTileEntity(boardPos) instanceof GunPegboardBlockEntity pegboard)) {
                sendFailure(source, "commands.tacz.debug.pegboard.failed", placed, toShortString(boardPos));
                return placed;
            }
            int slots = Math.min(DISPLAY_SLOTS.length, requested - placed);
            Map<Integer, ItemStack> contents = new LinkedHashMap<>(slots);
            for (int slot = 0; slot < slots; slot++) {
                contents.put(DISPLAY_SLOTS[slot], guns.get(same ? 0 : (placed + slot) % guns.size()));
            }
            if (!pegboard.setItems(contents)) {
                sendFailure(source, "commands.tacz.debug.pegboard.failed", placed, toShortString(boardPos));
                return placed;
            }
            placed += slots;
        }

        int distinct = same ? 1 : Math.min(requested, guns.size());
        source.sendMessage(Components.translatable("commands.tacz.debug.pegboard.success",
                placed, boardCount, distinct, toShortString(origin)));
        return placed;
    }

    private static void sendFailure(ICommandSender source, String key, Object... args) {
        source.sendMessage(Components.translatable(key, args).withStyle(TextFormatting.RED));
    }

    /**
     * {@code BlockPos#toShortString}
     */
    private static String toShortString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
