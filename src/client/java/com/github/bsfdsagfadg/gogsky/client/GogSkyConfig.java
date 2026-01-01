package com.github.bsfdsagfadg.gogsky.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.Set;

/**
 * 模组配置类
 */
public class GogSkyConfig {
    /**
     * 启用水晶花园天空的世界列表
     */
    public static final Set<ResourceKey<Level>> ENABLED_DIMENSIONS = new HashSet<>();

    static {
        // 默认启用主世界 (Overworld)
        ENABLED_DIMENSIONS.add(Level.OVERWORLD);
    }

    /**
     * 检查当前世界是否应渲染水晶花园天空
     */
    public static boolean isEnabled(Level level) {
        return level != null && ENABLED_DIMENSIONS.contains(level.dimension());
    }
}