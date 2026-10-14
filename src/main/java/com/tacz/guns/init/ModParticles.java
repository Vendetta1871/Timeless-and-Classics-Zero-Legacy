package com.tacz.guns.init;

public class ModParticles {
    // TODO(port): 1.12.2 has no particle type registry (particles are the EnumParticleTypes enum), and BulletHoleOption is
    //  not ported yet (reserved: main session). The bullet hole is a client-side particle spawned from the hit packet.
//    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GunMod.MOD_ID);
//
//    public static final RegistryObject<ParticleType<BulletHoleOption>> BULLET_HOLE = PARTICLE_TYPES.register("bullet_hole", () -> new ModParticleType<>(false, BulletHoleOption.DESERIALIZER, BulletHoleOption.CODEC));
//
//    @SuppressWarnings("deprecation")
//    private static class ModParticleType<T extends ParticleOptions> extends ParticleType<T> {
//        private final Codec<T> codec;
//
//        public ModParticleType(boolean overrideLimiter, ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
//            super(overrideLimiter, deserializer);
//            this.codec = codec;
//        }
//
//        @Override
//        public @NotNull Codec<T> codec() {
//            return this.codec;
//        }
//    }
}
