package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleDescription;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.motion.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.util.math.MathUtil;
import com.tacz.guns.legacy.util.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import com.tacz.guns.legacy.render.LightTexture;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.List;
import com.tacz.guns.legacy.util.LegacyMath;

@SideOnly(Side.CLIENT)
/**
 * 1.12.2: a plain {@link Particle} in the custom layer 3 of {@link net.minecraft.client.particle.ParticleManager}
 * (ticked and kept by vanilla); {@link #renderParticle} only queues it and {@link MolangWorldParticleRenderType}
 * draws the queue batched per texture/material after the world pass.
 */
public class SnowStormParticle extends Particle {

    private final ParticleInstance particleData;
    private final ParticleEffectDefinition definition;
    private final ParticleEmitterInstance emitter;
    private final boolean fovCompensatedWorldParticle;
    private final float birthFovScale;
    @Nullable private final Matrix3f birthMotionMap;
    @Nullable private final ParticleMotionDynamic dynamicMotion;

    // 锚点跟随：发射器启用 followAnchor 时，粒子保存锚点局部坐标，
    // 渲染时用发射器当前 worldTransform（含旋转+平移）重新求世界坐标——位置与朝向均跟随锚点
    private final boolean followAnchor;
    private final float localX;
    private final float localY;
    private final float localZ;

    // 锚点跟随渲染用的临时世界坐标
    private static final Vector3f FOLLOW_POS = new Vector3f();

    // billboard 朝向模式
    private final ParticleAppearanceBillboard.FaceCameraMode faceCameraMode;

    // 碰撞参数
    @Nullable
    private final ParticleMotionCollision collisionComponent;
    private boolean hasCollision;
    private final float collisionDrag;
    private final float coefficientOfRestitution;
    private final boolean expireOnContact;

    // 光照
    private final boolean environmentLighting;

    // 方块过期组件
    @Nullable
    private final ParticleExpireIfInBlocks expireIfInBlocks;
    @Nullable
    private final ParticleExpireIfNotInBlocks expireIfNotInBlocks;

    // 自定义 RenderType（按纹理+材质缓存）
    private final MolangWorldParticleRenderType renderType;

    public SnowStormParticle(WorldClient level, ParticleInstance particleData,
                             ParticleEffectDefinition definition,
                             ParticleEmitterInstance emitter) {
        super(level, particleData.x, particleData.y, particleData.z);
        this.particleData = particleData;
        this.definition = definition;
        this.emitter = emitter;
        float birthScale = emitter.getWorldParticleScale();
        this.fovCompensatedWorldParticle = emitter.isFovCompensatedWorldParticles()
                && Float.isFinite(birthScale) && birthScale > 1.0e-4f;
        this.birthFovScale = birthScale;
        this.birthMotionMap = fovCompensatedWorldParticle ? createBirthMotionMap(birthScale) : null;
        this.dynamicMotion = fovCompensatedWorldParticle
                ? definition.findComponent(ParticleMotionDynamic.class) : null;

        // 初始速度（blocks/tick）
        this.motionX = particleData.vx / 20f;
        this.motionY = particleData.vy / 20f;
        this.motionZ = particleData.vz / 20f;

        // 颜色
        this.particleRed = particleData.r;
        this.particleGreen = particleData.g;
        this.particleBlue = particleData.b;
        this.particleAlpha = particleData.a;

        // 生命周期（tick 为单位）
        this.particleMaxAge = (int) (particleData.maxLifetime * 20);
        this.particleAge = 0;

        // 不使用原版的重力和摩擦
        // 1.12.2: Particle has no friction field; onUpdate is replaced entirely
        this.particleGravity = 0;

        // billboard 朝向模式
        ParticleAppearanceBillboard billboard = definition.findComponent(ParticleAppearanceBillboard.class);
        this.faceCameraMode = billboard != null
                ? billboard.faceCameraMode()
                : ParticleAppearanceBillboard.FaceCameraMode.ROTATE_XYZ;

        // 碰撞
        ParticleMotionCollision collision = definition.findComponent(ParticleMotionCollision.class);
        this.collisionComponent = collision;
        if (collision != null) {
            // enabled 初始值：如果有 enabled 表达式则求值，否则默认启用
            if (collision.enabled() != null) {
                this.hasCollision = collision.enabled().evaluate(emitter.getMolang().getContext()) != 0;
            } else {
                this.hasCollision = true;
            }
            this.collisionDrag = collision.collisionDrag();
            this.coefficientOfRestitution = collision.coefficientOfRestitution();
            this.expireOnContact = collision.expireOnContact();
            this.canCollide = true;
            float radius = collision.collisionRadius();
            this.setBoundingBox(this.getBoundingBox().grow(radius));
        } else {
            this.hasCollision = false;
            this.collisionDrag = 0;
            this.coefficientOfRestitution = 1;
            this.expireOnContact = false;
        }

        // 光照：组件存在时使用世界光照，否则全亮
        this.environmentLighting = definition.findComponent(ParticleAppearanceLighting.class) != null;

        // 方块过期组件
        this.expireIfInBlocks = definition.findComponent(ParticleExpireIfInBlocks.class);
        this.expireIfNotInBlocks = definition.findComponent(ParticleExpireIfNotInBlocks.class);

        // RenderType
        ParticleDescription desc = definition.getDescription();
        this.renderType = MolangWorldParticleRenderType.get(desc.getMaterial(), desc.getTexture());

        // 锚点跟随：记录出生锚点（发射器 worldTransform），并把当前世界坐标反算为锚点局部坐标；
        // 渲染时用当前 worldTransform 重新变换回世界坐标，位置与朝向均跟随锚点
        this.followAnchor = emitter.isFollowAnchor();
        if (followAnchor) {
            Matrix4f spawnTransform = new Matrix4f(emitter.getWorldTransform()).invert();
            Vector4f local = new Vector4f(particleData.x, particleData.y, particleData.z, 1.0f);
            spawnTransform.transform(local);
            this.localX = local.x;
            this.localY = local.y;
            this.localZ = local.z;
        } else {
            this.localX = 0;
            this.localY = 0;
            this.localZ = 0;
        }
    }

    private static Matrix3f createBirthMotionMap(float scale) {
        Camera camera = Camera.getMainCamera();
        Matrix4f worldToCamera = new Matrix4f()
                .rotationX((float) Math.toRadians(camera.getXRot()))
                .rotateY((float) Math.toRadians(camera.getYRot() + 180f))
                .rotateZ(CameraStateCache.getCameraRollRadians());
        Matrix4f cameraToWorld = new Matrix4f(worldToCamera).invert();
        return new Matrix3f(cameraToWorld.scale(scale, scale, 1.0f).mul(worldToCamera));
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevParticleAngle = this.particleAngle;

        float dt = 1f / 20f;

        // 每帧更新 collision.enabled
        if (collisionComponent != null && collisionComponent.enabled() != null) {
            this.hasCollision = collisionComponent.enabled().evaluate(emitter.getMolang().getContext()) != 0;
        }

        float savedX = particleData.x, savedY = particleData.y, savedZ = particleData.z;

        emitter.updateSingleParticle(particleData, dt);
        applyFovAcceleration(dt);

        particleData.x = savedX;
        particleData.y = savedY;
        particleData.z = savedZ;

        this.motionX = particleData.vx / 20f;
        this.motionY = particleData.vy / 20f;
        this.motionZ = particleData.vz / 20f;

        this.move(this.motionX, this.motionY, this.motionZ);

        particleData.vx = (float) this.motionX * 20f;
        particleData.vy = (float) this.motionY * 20f;
        particleData.vz = (float) this.motionZ * 20f;

        particleData.x = (float) this.posX;
        particleData.y = (float) this.posY;
        particleData.z = (float) this.posZ;

        this.particleAngle = (float) Math.toRadians(particleData.rotation);

        this.particleRed = particleData.r;
        this.particleGreen = particleData.g;
        this.particleBlue = particleData.b;
        this.particleAlpha = particleData.a;

        // 方块过期检测
        if (particleData.alive) {
            checkBlockExpiration();
        }

        if (!particleData.alive) {
            emitter.fireParticleExpirationEvents(particleData);
            this.setExpired();
        }

        this.particleAge++;
    }

    private void applyFovAcceleration(float dt) {
        if (birthMotionMap == null || dynamicMotion == null || dynamicMotion.linearAcceleration() == null) {
            return;
        }
        var acceleration = dynamicMotion.linearAcceleration();
        var context = emitter.getMolang().getContext();
        float ax = (float) acceleration[0].evaluate(context);
        float ay = (float) acceleration[1].evaluate(context);
        float az = (float) acceleration[2].evaluate(context);
        float mx = birthMotionMap.m00() * ax + birthMotionMap.m10() * ay + birthMotionMap.m20() * az;
        float my = birthMotionMap.m01() * ax + birthMotionMap.m11() * ay + birthMotionMap.m21() * az;
        float mz = birthMotionMap.m02() * ax + birthMotionMap.m12() * ay + birthMotionMap.m22() * az;
        float drag = dynamicMotion.linearDragCoefficient() == null ? 1.0f
                : Math.max(0.0f, 1.0f - (float) dynamicMotion.linearDragCoefficient().evaluate(context) * dt);
        particleData.vx += (mx - ax) * drag * dt;
        particleData.vy += (my - ay) * drag * dt;
        particleData.vz += (mz - az) * drag * dt;
    }

    @Override
    public void move(double x, double y, double z) {
        if (!hasCollision) {
            if (x != 0.0 || y != 0.0 || z != 0.0) {
                this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
                this.resetPositionToBB();
            }
            return;
        }

        double origX = x, origY = y, origZ = z;

        // 速度上限
        double velSqr = x * x + y * y + z * z;
        if (this.canCollide && (x != 0.0 || y != 0.0 || z != 0.0) && velSqr < 10000.0) {
            Vec3d collided = collideBoundingBox(new Vec3d(x, y, z), this.getBoundingBox());

            if (x != collided.x) {
                this.motionX = -LegacyMath.sign(motionX) * (Math.abs(motionX) - collisionDrag / 20f) * coefficientOfRestitution;
            }
            if (y != collided.y) {
                this.motionY *= -coefficientOfRestitution;
            }
            if (z != collided.z) {
                this.motionZ = -LegacyMath.sign(motionZ) * (Math.abs(motionZ) - collisionDrag / 20f) * coefficientOfRestitution;
            }

            x = collided.x;
            y = collided.y;
            z = collided.z;
        }

        if (x != 0.0 || y != 0.0 || z != 0.0) {
            this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
            this.resetPositionToBB();
        }

        // 检测碰撞发生
        boolean collided = origX != x || origY != y || origZ != z;
        if (collided) {
            this.onGround = origY != y && origY < 0.0;

            // 触发碰撞事件
            float speed = (float) Math.sqrt(
                    particleData.vx * particleData.vx +
                    particleData.vy * particleData.vy +
                    particleData.vz * particleData.vz
            );
            emitter.fireCollisionEvents(particleData, speed);

            if (expireOnContact) {
                this.setExpired();
            }
        }
    }

    /**
     * 1.12.2 stand-in for 1.20 {@code Entity.collideBoundingBox(null, movement, box, level, List.of())}: clips the
     * movement against the block collision boxes, Y first, then X and Z (no step-up).
     */
    private Vec3d collideBoundingBox(Vec3d movement, AxisAlignedBB box) {
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        List<AxisAlignedBB> boxes = this.world.getCollisionBoxes(null, box.expand(x, y, z));
        AxisAlignedBB moved = box;
        if (y != 0.0) {
            for (AxisAlignedBB other : boxes) {
                y = other.calculateYOffset(moved, y);
            }
            moved = moved.offset(0.0, y, 0.0);
        }
        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0) {
            for (AxisAlignedBB other : boxes) {
                z = other.calculateZOffset(moved, z);
            }
            moved = moved.offset(0.0, 0.0, z);
        }
        if (x != 0.0) {
            for (AxisAlignedBB other : boxes) {
                x = other.calculateXOffset(moved, x);
            }
            moved = moved.offset(x, 0.0, 0.0);
        }
        if (!zFirst && z != 0.0) {
            for (AxisAlignedBB other : boxes) {
                z = other.calculateZOffset(moved, z);
            }
        }
        return new Vec3d(x, y, z);
    }

    private static final Quaternionf QUATERNION = new Quaternionf();
    private static final Vector3f TEMP_VEC = new Vector3f();
    private static final Vector3f TEMP_VEC2 = new Vector3f();
    private static final Vector3f TEMP_VEC3 = new Vector3f();
    private static final Vector3f AXIS_X = new Vector3f();
    private static final Vector3f AXIS_Y = new Vector3f();
    private static final Vector4f TEMP_VEC4 = new Vector4f();
    private static final Matrix4f TEMP_MAT = new Matrix4f();
    private static final Matrix3f TEMP_MAT3 = new Matrix3f();

    /**
     * 1.12.2: vanilla calls this for layer 3 particles inside {@code ParticleManager#renderLitParticles} without a
     * started buffer; the particle is queued and drawn batched by {@link MolangWorldParticleRenderType}.
     */
    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ,
                               float rotationYZ, float rotationXY, float rotationXZ) {
        renderType.queue(this);
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    /**
     * 1.12.2: {@code buffer} has the {@code PARTICLE_POSITION_TEX_COLOR_LMAP} format, and vertices are relative to the
     * GL modelview origin of the world pass (the render view entity's interpolated position,
     * {@link RenderManager#viewerPosX}), not to the camera.
     */
    public void render(BufferBuilder buffer, Camera camera, float partialTicks) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        double originX = renderManager.viewerPosX;
        double originY = renderManager.viewerPosY;
        double originZ = renderManager.viewerPosZ;
        float cx, cy, cz;
        if (followAnchor) {
            // 锚点跟随：用发射器当前 worldTransform（含旋转+平移）把局部坐标变换回世界坐标
            emitter.getWorldTransform().transformPosition(localX, localY, localZ, FOLLOW_POS);
            cx = (float) (FOLLOW_POS.x - originX);
            cy = (float) (FOLLOW_POS.y - originY);
            cz = (float) (FOLLOW_POS.z - originZ);
        } else {
            cx = (float) (LegacyMath.lerp(partialTicks, this.prevPosX, this.posX) - originX);
            cy = (float) (LegacyMath.lerp(partialTicks, this.prevPosY, this.posY) - originY);
            cz = (float) (LegacyMath.lerp(partialTicks, this.prevPosZ, this.posZ) - originZ);
        }

        // 构建朝向四元数
        QUATERNION.identity();
        applyFacingMode(QUATERNION, camera);

        // 应用粒子自旋
        float currentRoll = LegacyMath.lerp(partialTicks, this.prevParticleAngle, this.particleAngle);
        if (currentRoll != 0) {
            QUATERNION.rotateZ(currentRoll);
        }

        // 粒子尺寸：第一人称世界粒子在绘制时按当前 FOV 补偿，避免旧/新粒子尺寸混杂。
        float scale = particleData.spawnScale;
        if (fovCompensatedWorldParticle) {
            scale *= CameraStateCache.getWorldParticleScale() / birthFovScale;
        }
        float hw = particleData.width * scale;
        float hh = particleData.height * scale;

        // UV
        float u0 = particleData.u0;
        float v0 = particleData.v0;
        float u1 = particleData.u1;
        float v1 = particleData.v1;

        int light = getBrightnessForRender(partialTicks);

        AXIS_X.set(1, 0, 0).rotate(QUATERNION).mul(hw);
        AXIS_Y.set(0, 1, 0).rotate(QUATERNION).mul(hh);

        float ax = AXIS_X.x(), ay = AXIS_X.y(), az = AXIS_X.z();
        float bx = AXIS_Y.x(), by = AXIS_Y.y(), bz = AXIS_Y.z();

        float x0 = cx - ax - bx;
        float y0 = cy - ay - by;
        float z0 = cz - az - bz;
        float x1 = cx - ax + bx;
        float y1 = cy - ay + by;
        float z1 = cz - az + bz;
        float x2 = cx + ax + bx;
        float y2 = cy + ay + by;
        float z2 = cz + az + bz;
        float x3 = cx + ax - bx;
        float y3 = cy + ay - by;
        float z3 = cz + az - bz;

        // TODO(port): Sodium vertex writer fast path (SodiumParticleVertexWriter) has no 1.12.2 counterpart

        renderVertex(buffer, x0, y0, z0, u0, v1, light);
        renderVertex(buffer, x1, y1, z1, u0, v0, light);
        renderVertex(buffer, x2, y2, z2, u1, v0, light);
        renderVertex(buffer, x3, y3, z3, u1, v1, light);
    }

    private void renderVertex(BufferBuilder buffer, float x, float y, float z, float u, float v, int light) {
        buffer.pos(x, y, z)
                .tex(u, v).color(particleRed, particleGreen, particleBlue, particleAlpha)
                .lightmap(light >> 16 & 0xFFFF, light & 0xFFFF).endVertex();
    }

    // 应用朝向模式
    private void applyFacingMode(Quaternionf q, Camera camera) {
        switch (faceCameraMode) {
            case ROTATE_XYZ -> q.set(camera.rotation());
            case ROTATE_Y -> {
                Quaternionf camRot = camera.rotation();
                q.set(0, camRot.y, 0, camRot.w).normalize();
            }
            case LOOKAT_XYZ -> {
                // 真正的 lookat：从粒子位置看向摄像机
                Vec3d camPos = camera.getPosition();
                TEMP_VEC.set(
                        (float) (camPos.x - this.posX),
                        (float) (camPos.y - this.posY),
                        (float) (camPos.z - this.posZ)
                ).normalize(); // toCamera
                TEMP_VEC2.set(0, 1, 0); // up
                TEMP_VEC2.cross(TEMP_VEC, TEMP_VEC3); // right = up × toCamera
                TEMP_VEC3.normalize();
                TEMP_VEC.cross(TEMP_VEC3, TEMP_VEC2); // correctedUp = toCamera × right
                q.setFromNormalized(TEMP_MAT3.set(
                        TEMP_VEC3.x, TEMP_VEC2.x, TEMP_VEC.x,
                        TEMP_VEC3.y, TEMP_VEC2.y, TEMP_VEC.y,
                        TEMP_VEC3.z, TEMP_VEC2.z, TEMP_VEC.z
                ).invert());
            }
            case LOOKAT_Y -> {
                // lookat 但只保留 Y 轴旋转分量
                Vec3d camPos = camera.getPosition();
                TEMP_VEC.set(
                        (float) (camPos.x - this.posX),
                        (float) (camPos.y - this.posY),
                        (float) (camPos.z - this.posZ)
                ).normalize(); // toCamera
                TEMP_VEC2.set(0, 1, 0); // up
                TEMP_VEC2.cross(TEMP_VEC, TEMP_VEC3); // right = up × toCamera
                TEMP_VEC3.normalize();
                TEMP_VEC.cross(TEMP_VEC3, TEMP_VEC2); // correctedUp = toCamera × right
                q.setFromNormalized(TEMP_MAT3.set(
                        TEMP_VEC3.x, TEMP_VEC2.x, TEMP_VEC.x,
                        TEMP_VEC3.y, TEMP_VEC2.y, TEMP_VEC.y,
                        TEMP_VEC3.z, TEMP_VEC2.z, TEMP_VEC.z
                ).invert());
                q.x = 0;
                q.z = 0;
                q.normalize();
            }
            case LOOKAT_DIRECTION -> {
                // X 轴沿速度方向（facingDirection），然后绕 X 轴旋转使面片尽量朝向摄像机
                TEMP_VEC.set(particleData.vx, particleData.vy, particleData.vz);
                float len = TEMP_VEC.length();
                if (len > 0.0001f) {
                    TEMP_VEC.normalize();
                    // 从 X 轴 (1,0,0) 旋转到速度方向
                    TEMP_VEC2.set(1, 0, 0);
                    MathUtil.setFromUnitVectors(TEMP_VEC2, TEMP_VEC, q);
                    // 把摄像机方向变换到粒子局部空间，计算绕 X 轴的旋转使面片朝向摄像机
                    Vec3d camPos = camera.getPosition();
                    TEMP_VEC4.set(
                            (float) (camPos.x - this.posX),
                            (float) (camPos.y - this.posY),
                            (float) (camPos.z - this.posZ),
                            0
                    ).mul(TEMP_MAT.rotation(q).invert());
                    q.rotateX((float) MathHelper.atan2(-TEMP_VEC4.y, TEMP_VEC4.z));
                } else {
                    q.set(camera.rotation());
                }
            }
            case DIRECTION_X -> q.rotationXYZ(0, (float) (Math.PI / 2), 0);
            case DIRECTION_Y -> q.rotationXYZ((float) (Math.PI / 2), (float) Math.PI, 0);
            case DIRECTION_Z -> q.identity();
            default -> q.set(camera.rotation());
        }
    }

    public MolangWorldParticleRenderType getRenderType() {
        return renderType;
    }

    protected float getU0() {
        return particleData.u0;
    }

    protected float getU1() {
        return particleData.u1;
    }

    protected float getV0() {
        return particleData.v0;
    }

    protected float getV1() {
        return particleData.v1;
    }

    /**
     * 获取关联的 ParticleInstance 数据。
     */
    public ParticleInstance getParticleData() {
        return particleData;
    }

    /**
     * 获取关联的发射器实例。
     */
    public ParticleEmitterInstance getEmitter() {
        return emitter;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return environmentLighting ? super.getBrightnessForRender(partialTick) : LightTexture.FULL_BRIGHT;
    }

    /**
     * 检查粒子是否因所在方块而过期。
     */
    private void checkBlockExpiration() {
        if (expireIfInBlocks == null && expireIfNotInBlocks == null) return;

        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        String blockId = String.valueOf(this.world.getBlockState(pos).getBlock().getRegistryName());

        if (expireIfInBlocks != null && expireIfInBlocks.blocks().contains(blockId)) {
            particleData.alive = false;
        }
        if (expireIfNotInBlocks != null && !expireIfNotInBlocks.blocks().contains(blockId)) {
            particleData.alive = false;
        }
    }
}
