package com.github.mcmodderanchor.simplebedrockmodel;

import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2: SimpleBedrockModel is bundled into TACZ instead of being a separate mod, so this is no {@code @Mod}
 * class. Only the v1 subset TACZ needs (molang, bedrock particles, animation clocks, resource POJOs) is ported;
 * its event subscribers register under TACZ's mod id ({@link #EVENT_MOD_ID}).
 * <p>
 * TODO(port): left out of the v1 subset: the particle debug command, the network package, the client compat
 * layers (Sodium, Accelerated Rendering, Epic Fight) and the armor / camera handlers. The first-person item
 * handler lives in {@code com.tacz.guns.legacy.fp}.
 */
public class SimpleBedrockModel {
    public static final String MOD_ID = "simplebedrockmodel";
    /**
     * 1.12.2: {@code @Mod.EventBusSubscriber} only registers classes for an existing mod container.
     */
    public static final String EVENT_MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private SimpleBedrockModel() {
    }

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
