package sk.gothmur.mod.block;

import net.minecraft.util.StringRepresentable;

public enum RockBase implements StringRepresentable {
    STONE("stone"),
    ANDESITE("andesite"),
    DIORITE("diorite"),
    GRANITE("granite"),
    DEEPSLATE("deepslate"),
    COPPER_ORE("copper_ore"),
    DEEPSLATE_COPPER_ORE("deepslate_copper_ore");

    private final String name;
    RockBase(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
