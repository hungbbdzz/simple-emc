package com.velorise.simpleemc;

import com.velorise.simpleemc.capability.PlayerEMCCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeSharingBookItem extends Item {
    public KnowledgeSharingBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isWritten(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (player.isSecondaryUseActive() && !isWritten(stack)) {
                writeKnowledge(player, stack);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (isWritten(stack)) {
            String owner = "Unknown";
            int count = 0;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                owner = tag.getString("owner_name");
                if (tag.contains("learned_items", Tag.TAG_LIST)) {
                    count = tag.getList("learned_items", Tag.TAG_STRING).size();
                }
            }
            tooltipComponents.add(Component.translatable("item.simpleemc.knowledge_sharing_book.written_by", owner));
            tooltipComponents.add(Component.translatable("item.simpleemc.knowledge_sharing_book.contains", count));
            tooltipComponents.add(Component.translatable("item.simpleemc.knowledge_sharing_book.burn_tooltip"));
        } else {
            tooltipComponents.add(Component.translatable("item.simpleemc.knowledge_sharing_book.empty_tooltip"));
        }
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
    }

    public static boolean isWritten(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("written");
    }

    public static void writeKnowledge(Player player, ItemStack stack) {
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            List<ResourceLocation> learnedRLs = data.getLearnedItemsRL();

            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("written", true);
            tag.putString("owner_name", player.getScoreboardName());

            ListTag itemsList = new ListTag();
            for (ResourceLocation rl : learnedRLs) {
                itemsList.add(net.minecraft.nbt.StringTag.valueOf(rl.toString()));
            }
            tag.put("learned_items", itemsList);

            player.sendSystemMessage(Component.translatable("item.simpleemc.knowledge_sharing_book.success", player.getScoreboardName(), learnedRLs.size()));
        });
    }

    public static List<ResourceLocation> getStoredItems(ItemStack stack) {
        List<ResourceLocation> list = new ArrayList<>();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.contains("learned_items", Tag.TAG_LIST)) {
                ListTag itemsList = tag.getList("learned_items", Tag.TAG_STRING);
                for (int i = 0; i < itemsList.size(); i++) {
                    String s = itemsList.getString(i);
                    ResourceLocation rl = ResourceLocation.tryParse(s);
                    if (rl != null) {
                        list.add(rl);
                    }
                }
            }
        }
        return list;
    }
}
