package com.tacz.guns.event.ammo;

// TODO(port): 1.12.2 has no bell block (BellBlock#attemptToRing), so there is nothing for an ammo hit to ring.
//  Upstream, for reference:
//
//@Mod.EventBusSubscriber
//public class BellRing {
//    @SubscribeEvent
//    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
//        Level level = event.getLevel();
//        BlockState state = event.getState();
//        BlockHitResult hitResult = event.getHitResult();
//        if (state.getBlock() instanceof BellBlock bell) {
//            bell.attemptToRing(level, hitResult.getBlockPos(), hitResult.getDirection());
//        }
//    }
//}
public class BellRing {
}
