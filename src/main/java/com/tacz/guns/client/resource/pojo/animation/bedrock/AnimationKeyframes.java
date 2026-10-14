package com.tacz.guns.client.resource.pojo.animation.bedrock;


import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class AnimationKeyframes {
    private final Double2ObjectRBTreeMap<Keyframe> keyframes;

    public AnimationKeyframes(Double2ObjectRBTreeMap<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Keyframe> getKeyframes() {
        return keyframes;
    }

    public static final class Keyframe {
        private final @Nullable Vector3f pre;
        private final @Nullable Vector3f post;
        private final @Nullable Vector3f data;
        private final @Nullable String lerpMode;

        public Keyframe(@Nullable Vector3f pre, @Nullable Vector3f post, @Nullable Vector3f data, @Nullable String lerpMode) {
            this.pre = pre;
            this.post = post;
            this.data = data;
            this.lerpMode = lerpMode;
        }

        @Nullable public Vector3f pre() {
            return pre;
        }

        @Nullable public Vector3f post() {
            return post;
        }

        @Nullable public Vector3f data() {
            return data;
        }

        @Nullable public String lerpMode() {
            return lerpMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Keyframe that = (Keyframe) o;
            return java.util.Objects.equals(pre, that.pre) && java.util.Objects.equals(post, that.post) && java.util.Objects.equals(data, that.data) && java.util.Objects.equals(lerpMode, that.lerpMode);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(pre, post, data, lerpMode);
        }

        @Override
        public String toString() {
            return "Keyframe[" + "pre=" + pre + ", " + "post=" + post + ", " + "data=" + data + ", " + "lerpMode=" + lerpMode + "]";
        }
    }
}
