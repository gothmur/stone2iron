package sk.gothmur.mod.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;                 // <-- SPRÁVNY import
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FlintBifaceItem extends Item {
    private static final ResourceLocation ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("stone2steel", "flint_biface_damage");
    private static final ResourceLocation ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("stone2steel", "flint_biface_speed");

    private final double attackDamage;
    private final double attackSpeed;

    public FlintBifaceItem(Properties props, double damage, double speed) {
        super(props.durability(30)); // 30 použití
        this.attackDamage = damage;
        this.attackSpeed = speed;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // zníženie durability pri zásahu
        stack.hurtAndBreak(1, attacker, null);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Damage: " + attackDamage));
        tooltip.add(Component.literal("Speed: " + attackSpeed));
    }

    // Nové API (1.21.1): atribúty cez ItemAttributeModifiers + EquipmentSlotGroup
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }
}
