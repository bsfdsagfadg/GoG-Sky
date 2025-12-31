package com.github.bsfdsagfadg.gogsky.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.Set;

public class GogSkyConfig {
    public static final Set<ResourceKey<Level>> ENABLED_DIMENSIONS = new HashSet<>();

    static {
        // 默认启用主世界
        ENABLED_DIMENSIONS.add(Level.OVERWORLD);
    }

    public static boolean isEnabled(Level level) {
        return level != null && ENABLED_DIMENSIONS.contains(level.dimension());
    }
}