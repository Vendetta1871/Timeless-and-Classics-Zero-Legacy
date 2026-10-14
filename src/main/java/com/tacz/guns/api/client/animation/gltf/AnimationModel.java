package com.tacz.guns.api.client.animation.gltf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnimationModel {
    /**
     * The {@link Channel} instances
     * of this animation
     */
    private final List<Channel> channels = new ArrayList<>();
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add the given {@link Channel}
     *
     * @param channel The {@link Channel}
     */
    public void addChannel(Channel channel) {
        Objects.requireNonNull(channel, "The channel may not be null");
        this.channels.add(channel);
    }

    public List<Channel> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    public enum Interpolation {
        /**
         * Stepwise interpolation
         */
        STEP,

        /**
         * Linear interpolation
         */
        LINEAR,

        /**
         * Spline interpolation
         */
        SPLINE
    }


    /**
     * @param input         The input data
     * @param interpolation The interpolation method
     * @param output        The output data
     */
    public static final class Sampler {
        private final AccessorModel input;
        private final Interpolation interpolation;
        private final AccessorModel output;

        public AccessorModel input() {
            return input;
        }

        public Interpolation interpolation() {
            return interpolation;
        }

        public AccessorModel output() {
            return output;
        }

        /**
         * Default constructor
         *
         * @param input         The input
         * @param interpolation The interpolation
         * @param output        The output
         */
        public Sampler(
                AccessorModel input,
                Interpolation interpolation,
                AccessorModel output) {
            this.input = Objects.requireNonNull(
                    input, "The input may not be null");
            this.interpolation = Objects.requireNonNull(
                    interpolation, "The interpolation may not be null");
            this.output = Objects.requireNonNull(
                    output, "The output may not be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sampler that = (Sampler) o;
            return java.util.Objects.equals(input, that.input) && java.util.Objects.equals(interpolation, that.interpolation) && java.util.Objects.equals(output, that.output);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(input, interpolation, output);
        }

        @Override
        public String toString() {
            return "Sampler[" + "input=" + input + ", " + "interpolation=" + interpolation + ", " + "output=" + output + "]";
        }
    }

    /**
     * @param sampler   The sampler
     * @param nodeModel The node model
     * @param path      The path
     */
    public static final class Channel {
        private final Sampler sampler;
        private final NodeModel nodeModel;
        private final String path;

        public Sampler sampler() {
            return sampler;
        }

        public NodeModel nodeModel() {
            return nodeModel;
        }

        public String path() {
            return path;
        }

        /**
         * Default constructor
         *
         * @param sampler   The sampler
         * @param nodeModel The node model
         * @param path      The path
         */
        public Channel(
                Sampler sampler,
                NodeModel nodeModel,
                String path) {
            this.sampler = Objects.requireNonNull(
                    sampler, "The sampler may not be null");
            this.nodeModel = nodeModel;
            this.path = Objects.requireNonNull(
                    path, "The path may not be null");

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Channel that = (Channel) o;
            return java.util.Objects.equals(sampler, that.sampler) && java.util.Objects.equals(nodeModel, that.nodeModel) && java.util.Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(sampler, nodeModel, path);
        }

        @Override
        public String toString() {
            return "Channel[" + "sampler=" + sampler + ", " + "nodeModel=" + nodeModel + ", " + "path=" + path + "]";
        }
    }
}
