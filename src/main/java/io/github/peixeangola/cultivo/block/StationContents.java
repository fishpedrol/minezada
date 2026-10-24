package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.StationVisualRules;
import net.minecraft.util.StringRepresentable;

/**
 * Valor da propriedade {@code contents} das estações. Espelha {@link StationVisualRules.Contents} (a regra pura) com
 * a interface que o EnumProperty do Minecraft exige; os nomes vêm da regra.
 */
public enum StationContents implements StringRepresentable {
    EMPTY(StationVisualRules.Contents.EMPTY),
    WORKING(StationVisualRules.Contents.WORKING),
    DONE(StationVisualRules.Contents.DONE);

    private final StationVisualRules.Contents rule;

    StationContents(StationVisualRules.Contents rule) {
        this.rule = rule;
    }

    public static StationContents of(StationVisualRules.Contents rule) {
        for (StationContents value : values()) {
            if (value.rule == rule) {
                return value;
            }
        }
        throw new IllegalArgumentException("sem valor de blockstate para " + rule);
    }

    @Override
    public String getSerializedName() {
        return rule.id();
    }
}
