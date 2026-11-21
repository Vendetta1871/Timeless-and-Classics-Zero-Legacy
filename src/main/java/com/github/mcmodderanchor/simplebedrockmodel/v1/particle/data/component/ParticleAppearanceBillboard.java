package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

public final class ParticleAppearanceBillboard implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression[] size;
    private final FaceCameraMode faceCameraMode;
    private final UVConfig uv;
    private final FlipbookConfig flipbook;
    private final boolean dynamicSize;

    public ParticleAppearanceBillboard(MolangExpression[] size, FaceCameraMode faceCameraMode, UVConfig uv, FlipbookConfig flipbook, boolean dynamicSize) {
        this.size = size;
        this.faceCameraMode = faceCameraMode;
        this.uv = uv;
        this.flipbook = flipbook;
        this.dynamicSize = dynamicSize;
    }

    public MolangExpression[] size() {
        return size;
    }

    public FaceCameraMode faceCameraMode() {
        return faceCameraMode;
    }

    public UVConfig uv() {
        return uv;
    }

    public FlipbookConfig flipbook() {
        return flipbook;
    }

    public boolean dynamicSize() {
        return dynamicSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleAppearanceBillboard)) return false;
        ParticleAppearanceBillboard that = (ParticleAppearanceBillboard) o;
        return java.util.Objects.equals(size, that.size) && java.util.Objects.equals(faceCameraMode, that.faceCameraMode) && java.util.Objects.equals(uv, that.uv) && java.util.Objects.equals(flipbook, that.flipbook) && java.util.Objects.equals(dynamicSize, that.dynamicSize);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(size, faceCameraMode, uv, flipbook, dynamicSize);
    }

    @Override
    public String toString() {
        return "ParticleAppearanceBillboard[" + "size=" + size + ", " + "faceCameraMode=" + faceCameraMode + ", " + "uv=" + uv + ", " + "flipbook=" + flipbook + ", " + "dynamicSize=" + dynamicSize + "]";
    }


    @Override
    public int order() {
        return 200;
    }

    @Override
    public boolean requireUpdate() {
        return flipbook != null || dynamicSize;
    }

    @Override
    public void apply(ParticleInstance p) {
        updateAppearance(p);
    }

    @Override
    public void update(ParticleInstance p) {
        updateAppearance(p);
    }

    private void updateAppearance(ParticleInstance p) {
        if (p.emitter == null) return;
        MolangContext<?> ctx = p.emitter.getMolang().getContext();
        p.width = (float) size[0].evaluate(ctx);
        p.height = (float) size[1].evaluate(ctx);

        if (flipbook != null) {
            var fb = flipbook;
            int maxFrame = (int) fb.maxFrame().evaluate(ctx);
            float frame = fb.stretchToLifetime() && p.maxLifetime > 0
                    ? (p.age / p.maxLifetime) * maxFrame
                    : p.age * fb.framesPerSecond();
            int frameIdx = Math.max(0, fb.loop() && maxFrame > 0
                    ? ((int) frame) % maxFrame
                    : Math.min((int) frame, maxFrame - 1));

            float baseU = (float) fb.baseUV()[0].evaluate(ctx);
            float baseV = (float) fb.baseUV()[1].evaluate(ctx);
            float sizeU = (float) fb.sizeUV()[0].evaluate(ctx);
            float sizeV = (float) fb.sizeUV()[1].evaluate(ctx);
            float stepU = (float) fb.stepUV()[0].evaluate(ctx);
            float stepV = (float) fb.stepUV()[1].evaluate(ctx);
            float u = baseU + stepU * frameIdx;
            float v = baseV + stepV * frameIdx;
            p.u0 = u / fb.textureWidth();
            p.v0 = v / fb.textureHeight();
            p.u1 = (u + sizeU) / fb.textureWidth();
            p.v1 = (v + sizeV) / fb.textureHeight();
        } else if (uv != null) {
            p.u0 = (float) uv.u().evaluate(ctx) / uv.textureWidth();
            p.v0 = (float) uv.v().evaluate(ctx) / uv.textureHeight();
            p.u1 = ((float) uv.u().evaluate(ctx) + (float) uv.width().evaluate(ctx)) / uv.textureWidth();
            p.v1 = ((float) uv.v().evaluate(ctx) + (float) uv.height().evaluate(ctx)) / uv.textureHeight();
        }
    }

    public enum FaceCameraMode {
        ROTATE_XYZ, ROTATE_Y, LOOKAT_XYZ, LOOKAT_Y, LOOKAT_DIRECTION,
        DIRECTION_X, DIRECTION_Y, DIRECTION_Z,
        EMITTER_TRANSFORM_XY, EMITTER_TRANSFORM_XZ, EMITTER_TRANSFORM_YZ
    }

    public static final class UVConfig {
        private final MolangExpression u;
        private final MolangExpression v;
        private final MolangExpression width;
        private final MolangExpression height;
        private final int textureWidth;
        private final int textureHeight;

        public UVConfig(MolangExpression u, MolangExpression v, MolangExpression width, MolangExpression height, int textureWidth, int textureHeight) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        public MolangExpression u() {
            return u;
        }

        public MolangExpression v() {
            return v;
        }

        public MolangExpression width() {
            return width;
        }

        public MolangExpression height() {
            return height;
        }

        public int textureWidth() {
            return textureWidth;
        }

        public int textureHeight() {
            return textureHeight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UVConfig)) return false;
            UVConfig that = (UVConfig) o;
            return java.util.Objects.equals(u, that.u) && java.util.Objects.equals(v, that.v) && java.util.Objects.equals(width, that.width) && java.util.Objects.equals(height, that.height) && java.util.Objects.equals(textureWidth, that.textureWidth) && java.util.Objects.equals(textureHeight, that.textureHeight);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(u, v, width, height, textureWidth, textureHeight);
        }

        @Override
        public String toString() {
            return "UVConfig[" + "u=" + u + ", " + "v=" + v + ", " + "width=" + width + ", " + "height=" + height + ", " + "textureWidth=" + textureWidth + ", " + "textureHeight=" + textureHeight + "]";
        }
}

    public static final class FlipbookConfig {
        private final MolangExpression[] baseUV;
        private final MolangExpression[] sizeUV;
        private final MolangExpression[] stepUV;
        private final float framesPerSecond;
        private final MolangExpression maxFrame;
        private final boolean stretchToLifetime;
        private final boolean loop;
        private final int textureWidth;
        private final int textureHeight;

        public FlipbookConfig(MolangExpression[] baseUV, MolangExpression[] sizeUV, MolangExpression[] stepUV, float framesPerSecond, MolangExpression maxFrame, boolean stretchToLifetime, boolean loop, int textureWidth, int textureHeight) {
            this.baseUV = baseUV;
            this.sizeUV = sizeUV;
            this.stepUV = stepUV;
            this.framesPerSecond = framesPerSecond;
            this.maxFrame = maxFrame;
            this.stretchToLifetime = stretchToLifetime;
            this.loop = loop;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        public MolangExpression[] baseUV() {
            return baseUV;
        }

        public MolangExpression[] sizeUV() {
            return sizeUV;
        }

        public MolangExpression[] stepUV() {
            return stepUV;
        }

        public float framesPerSecond() {
            return framesPerSecond;
        }

        public MolangExpression maxFrame() {
            return maxFrame;
        }

        public boolean stretchToLifetime() {
            return stretchToLifetime;
        }

        public boolean loop() {
            return loop;
        }

        public int textureWidth() {
            return textureWidth;
        }

        public int textureHeight() {
            return textureHeight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FlipbookConfig)) return false;
            FlipbookConfig that = (FlipbookConfig) o;
            return java.util.Objects.equals(baseUV, that.baseUV) && java.util.Objects.equals(sizeUV, that.sizeUV) && java.util.Objects.equals(stepUV, that.stepUV) && java.util.Objects.equals(framesPerSecond, that.framesPerSecond) && java.util.Objects.equals(maxFrame, that.maxFrame) && java.util.Objects.equals(stretchToLifetime, that.stretchToLifetime) && java.util.Objects.equals(loop, that.loop) && java.util.Objects.equals(textureWidth, that.textureWidth) && java.util.Objects.equals(textureHeight, that.textureHeight);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(baseUV, sizeUV, stepUV, framesPerSecond, maxFrame, stretchToLifetime, loop, textureWidth, textureHeight);
        }

        @Override
        public String toString() {
            return "FlipbookConfig[" + "baseUV=" + baseUV + ", " + "sizeUV=" + sizeUV + ", " + "stepUV=" + stepUV + ", " + "framesPerSecond=" + framesPerSecond + ", " + "maxFrame=" + maxFrame + ", " + "stretchToLifetime=" + stretchToLifetime + ", " + "loop=" + loop + ", " + "textureWidth=" + textureWidth + ", " + "textureHeight=" + textureHeight + "]";
        }
}

    public static ParticleAppearanceBillboard fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        String[] sizeStr = getMolangArray(obj, "size", 2, "0.1", "0.1");
        MolangExpression[] size = new MolangExpression[]{molang.compile(sizeStr[0]), molang.compile(sizeStr[1])};
        boolean dynamicSize = !isNumericLiteral(sizeStr[0]) || !isNumericLiteral(sizeStr[1]);

        String modeStr = getString(obj, "facing_camera_mode", "rotate_xyz");
        FaceCameraMode mode = parseFaceCameraMode(modeStr);

        JsonObject uvObj = obj.has("uv") ? obj.getAsJsonObject("uv") : null;
        UVConfig uv = null;
        FlipbookConfig flipbook = null;

        if (uvObj != null && uvObj.has("flipbook")) {
            flipbook = parseFlipbookConfig(uvObj, molang);
        } else {
            uv = parseUVConfig(uvObj, molang);
        }

        return new ParticleAppearanceBillboard(size, mode, uv, flipbook, dynamicSize);
    }

    private static FaceCameraMode parseFaceCameraMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "rotate_y" -> FaceCameraMode.ROTATE_Y;
            case "lookat_xyz" -> FaceCameraMode.LOOKAT_XYZ;
            case "lookat_y" -> FaceCameraMode.LOOKAT_Y;
            case "lookat_direction" -> FaceCameraMode.LOOKAT_DIRECTION;
            case "direction_x" -> FaceCameraMode.DIRECTION_X;
            case "direction_y" -> FaceCameraMode.DIRECTION_Y;
            case "direction_z" -> FaceCameraMode.DIRECTION_Z;
            case "emitter_transform_xy" -> FaceCameraMode.EMITTER_TRANSFORM_XY;
            case "emitter_transform_xz" -> FaceCameraMode.EMITTER_TRANSFORM_XZ;
            case "emitter_transform_yz" -> FaceCameraMode.EMITTER_TRANSFORM_YZ;
            default -> FaceCameraMode.ROTATE_XYZ;
        };
    }

    private static UVConfig parseUVConfig(@Nullable JsonObject uvObj, ParticleMolangEnvironment molang) {
        if (uvObj == null) {
            return new UVConfig(MolangExpression.zero(), MolangExpression.zero(),
                    MolangExpression.constant(1), MolangExpression.constant(1), 1, 1);
        }
        int texW = uvObj.has("texture_width") ? uvObj.get("texture_width").getAsInt() : 1;
        int texH = uvObj.has("texture_height") ? uvObj.get("texture_height").getAsInt() : 1;

        if (uvObj.has("uv")) {
            JsonElement uvElem = uvObj.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray arr = uvElem.getAsJsonArray();
                MolangExpression u = molang.compile(molangFromElement(arr.get(0), "0"));
                MolangExpression v = molang.compile(molangFromElement(arr.get(1), "0"));
                MolangExpression w = MolangExpression.constant(1);
                MolangExpression h = MolangExpression.constant(1);
                if (uvObj.has("uv_size")) {
                    JsonArray sizeArr = uvObj.getAsJsonArray("uv_size");
                    w = molang.compile(molangFromElement(sizeArr.get(0), "1"));
                    h = molang.compile(molangFromElement(sizeArr.get(1), "1"));
                }
                return new UVConfig(u, v, w, h, texW, texH);
            }
        }
        return new UVConfig(MolangExpression.zero(), MolangExpression.zero(),
                MolangExpression.constant(texW), MolangExpression.constant(texH), texW, texH);
    }

    private static FlipbookConfig parseFlipbookConfig(JsonObject uvObj, ParticleMolangEnvironment molang) {
        int texW = uvObj.has("texture_width") ? uvObj.get("texture_width").getAsInt() : 1;
        int texH = uvObj.has("texture_height") ? uvObj.get("texture_height").getAsInt() : 1;
        JsonObject fb = uvObj.getAsJsonObject("flipbook");
        String[] baseStr = getMolangArray(fb, "base_UV", 2, "0", "0");
        String[] sizeStr = getMolangArray(fb, "size_UV", 2, "1", "1");
        String[] stepStr = getMolangArray(fb, "step_UV", 2, "0", "0");
        MolangExpression[] baseUV = new MolangExpression[]{molang.compile(baseStr[0]), molang.compile(baseStr[1])};
        MolangExpression[] sizeUV = new MolangExpression[]{molang.compile(sizeStr[0]), molang.compile(sizeStr[1])};
        MolangExpression[] stepUV = new MolangExpression[]{molang.compile(stepStr[0]), molang.compile(stepStr[1])};
        float fps = fb.has("frames_per_second") ? fb.get("frames_per_second").getAsFloat() : 1;
        MolangExpression maxFrame = molang.compile(getMolang(fb, "max_frame", "1"));
        boolean stretch = getBoolean(fb, "stretch_to_lifetime", false);
        boolean loop = getBoolean(fb, "loop", false);
        return new FlipbookConfig(baseUV, sizeUV, stepUV, fps, maxFrame, stretch, loop, texW, texH);
    }

    /**
     * 检查字符串是否为纯数字字面量（整数或浮点数）。
     */
    private static boolean isNumericLiteral(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
