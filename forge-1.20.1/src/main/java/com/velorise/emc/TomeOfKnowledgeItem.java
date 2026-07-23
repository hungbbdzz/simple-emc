package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class TomeOfKnowledgeItem extends Item {
    public TomeOfKnowledgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§7Learns every item permitted by the server EMC policy"));
        tooltipComponents.add(Component.literal("§7Consumed on use"));
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
    }
}
