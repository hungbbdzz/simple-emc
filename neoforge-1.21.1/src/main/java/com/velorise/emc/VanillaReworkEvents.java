package com.velorise.emc;

import com.velorise.emc.common.UtilityItemProfile;
import com.velorise.emc.common.FixedUtilityProfile;
import com.velorise.emc.common.WirelessCoreProfile;
import com.velorise.emc.common.WirelessCoreTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;


@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VanillaReworkEvents {
    private static final ThreadLocal<Boolean> CORE_HOE_HARVEST = ThreadLocal.withInitial(() -> false);

    private VanillaReworkEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        clearStaleAnvilState(player);
        CondensedCoreGeneratorService.tick(player);
        if (player.tickCount % 20 == 0 && player.isInLava()
            && (player.getMainHandItem().is(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get())
                || player.getOffhandItem().is(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get())
                || player.getMainHandItem().is(VeloriseEMC.HEART_OF_THE_LAST_ECHO.get())
                || player.getOffhandItem().is(VeloriseEMC.HEART_OF_THE_LAST_ECHO.get()))) {
            SimpleAdvancements.award(player, "last_echo/return_policy");
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) MatterburstExplosionManager.tick(level);
    }

    @SubscribeEvent
    public static void onMatterburstExplosion(ExplosionEvent.Start event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        net.minecraft.world.entity.Entity source = MatterburstExplosionManager.findTaggedExplosionSource(event.getExplosion());
        if (source == null) return;
        int core = source.getPersistentData().getInt(MatterburstExplosionManager.ENTITY_CORE_TAG);
        if (core < 1) return;
        event.setCanceled(true);
        MatterburstExplosionManager.start(level, source.position(), core);
        source.discard();
    }



    @SubscribeEvent
    public static void onHourglassTargetPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (isHourglassTargetState(event.getPlacedBlock())) {
            HourglassAccelerationManager.markDirtyAround(serverLevel, event.getPos());
        }
    }


    private static boolean isHourglassTargetState(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.FURNACE || block == Blocks.SMOKER || block == Blocks.BLAST_FURNACE
            || block == Blocks.BREWING_STAND;
    }



    private static void disableStoredActiveItems(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        for (ItemStack stack : player.getInventory().items) {
            if (stack == main) continue;
            if (ImbuedEquipmentUtil.isActive(stack)) ImbuedEquipmentUtil.setActive(stack, false);
            if (ImbuedEquipmentUtil.usesMiningActivation(stack)) {
                ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
            }
        }
        ItemStack off = player.getOffhandItem();
        if (ImbuedEquipmentUtil.usesMiningActivation(off)) {
            ImbuedEquipmentUtil.setActive(off, false);
            ImbuedEquipmentUtil.setMiningActiveUntil(off, 0L);
        }
    }


    private static void disableAllActiveItems(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) clearTransientActiveState(stack);
        for (ItemStack stack : player.getInventory().offhand) clearTransientActiveState(stack);
        for (ItemStack stack : player.getInventory().armor) clearTransientActiveState(stack);
    }

    private static void clearTransientActiveState(ItemStack stack) {
        if (ImbuedEquipmentUtil.isActive(stack)) ImbuedEquipmentUtil.setActive(stack, false);
        if (ImbuedEquipmentUtil.usesMiningActivation(stack)) {
            ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            disableAllActiveItems(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) disableAllActiveItems(player);
    }

    private static void drainActiveHeldItems(ServerPlayer player) {
        long now = player.level().getGameTime();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!ImbuedEquipmentUtil.isActive(stack) || !ImbuedEquipmentUtil.drainsWhileHeld(stack)) continue;
            long cost = ImbuedEquipmentUtil.activeCostPerSecond(stack);
            if (cost <= 0L || now < ImbuedEquipmentUtil.getNextChargeTick(stack)) continue;
            if (EmcTransactionService.spend(player, cost)) {
                ImbuedEquipmentUtil.setNextChargeTick(stack, now + 20L);
            } else {
                ImbuedEquipmentUtil.setActive(stack, false);
                player.displayClientMessage(Component.literal("§cImbued power disabled: insufficient EMC"), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 0.8F);
            }
        }
    }

    private static void drainMiningEquipment(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!ImbuedEquipmentUtil.usesMiningActivation(stack) || !ImbuedEquipmentUtil.isActive(stack)) return;

        long now = player.level().getGameTime();
        if (now > ImbuedEquipmentUtil.getMiningActiveUntil(stack)) {
            ImbuedEquipmentUtil.setActive(stack, false);
            ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
            return;
        }

        long cost = ImbuedEquipmentUtil.miningCostPerSecond(stack);
        if (cost <= 0L || now < ImbuedEquipmentUtil.getNextChargeTick(stack)) return;
        if (EmcTransactionService.spend(player, cost)) {
            ImbuedEquipmentUtil.setNextChargeTick(stack, now + 20L);
        } else {
            ImbuedEquipmentUtil.setActive(stack, false);
            ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
            ImbuedEquipmentUtil.setActionCooldownTick(stack, now + 20L);
            player.displayClientMessage(Component.literal("§cMining boost paused: insufficient EMC"), true);
        }
    }

    @SubscribeEvent
    public static void onCoreBoundItemDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        float bonus = coreDamageBonus(event.getSource(), player);
        if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
    }

    private static float coreDamageBonus(DamageSource source, ServerPlayer player) {
        return source.getDirectEntity() == player
            ? ImbuedEquipmentUtil.damageBonus(player.getMainHandItem())
            : 0.0F;
    }

    private static boolean hasAquaAffinity(Player player) {
        var aquaAffinity = player.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolder(Enchantments.AQUA_AFFINITY)
            .orElse(null);
        return aquaAffinity != null
            && EnchantmentHelper.getEnchantmentLevel(aquaAffinity, player) > 0;
    }

    private static void clearStaleAnvilState(ServerPlayer player) {
        if (!(player.containerMenu instanceof AlchemicalAnvilMenu)) {
            player.getPersistentData().remove(VanillaMachineRegistry.ANVIL_FLAG);
        }
    }

    @SubscribeEvent
    public static void onImbuedRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (!ImbuedEquipmentUtil.usesActiveMode(stack)) return;
        boolean active = !ImbuedEquipmentUtil.isActive(stack);
        if (active && ImbuedEquipmentUtil.drainsWhileHeld(stack)) {
            long cost = ImbuedEquipmentUtil.activeCostPerSecond(stack);
            if (!EmcTransactionService.spend(player, cost)) {
                player.displayClientMessage(Component.literal("§cNot enough EMC to activate this item"), true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            ImbuedEquipmentUtil.setNextChargeTick(stack, player.level().getGameTime() + 20L);
        } else if (active) {
            // Equipped utilities pay only when their actual worn condition starts running.
            ImbuedEquipmentUtil.setNextChargeTick(stack, 0L);
        }
        ImbuedEquipmentUtil.setActive(stack, active);
        int mode = ImbuedEquipmentUtil.getMode(stack);
        player.displayClientMessage(Component.literal(active
            ? "§aActive §7| §eMode " + mode
            : "§7Inactive §7| §eMode " + mode), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            active ? net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE : net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, active ? 1.25F : 0.85F);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!left.isEmpty() && CoreTierData.isCoreItem(right)) {
            WirelessCoreTier installedTier = CoreTierData.get(right);
            if (!canInstallCore(event.getPlayer(), installedTier)) return;
            if (!canUseAnvilTier(event.getPlayer(), left, installedTier)) return;
            ItemStack output = createCoreUpgrade(left, installedTier);
            if (!output.isEmpty()) {
                event.setOutput(output);
                event.setCost(Math.max(1, installedTier.level()));
                event.setMaterialCost(1);
                return;
            }
        }

}


    private static boolean canInstallCore(Player player, WirelessCoreTier tier) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        return serverPlayer.getAbilities().instabuild
            || serverPlayer.hasPermissions(2)
            || WirelessCoreProfile.isUnlocked(WorldProgressionData.get(serverPlayer.server).snapshot(), tier);
    }

    /**
     * Core installation is exclusive to this mod's Alchemical Anvil of the exact
     * matching tier. Vanilla Anvils and lower/higher Alchemical tiers cannot
     * substitute for the required station.
     */
    private static boolean canUseAnvilTier(Player player, ItemStack target, WirelessCoreTier tier) {
        if (target.isEmpty()) return false;
        int stationLevel = player != null && player.containerMenu instanceof AlchemicalAnvilMenu menu
            ? menu.anvilLevel() : 0;

        if (stationLevel == 0) return false;
        if (target.is(Items.ANVIL) || target.getItem() instanceof AlchemicalAnvilItem) return false;
        return stationLevel >= tier.level();
    }

    public static ItemStack createCoreUpgradePreview(ItemStack left, WirelessCoreTier installedTier) {
        return createCoreUpgrade(left, installedTier);
    }

    private static ItemStack createCoreUpgrade(ItemStack left, WirelessCoreTier installedTier) {
        if (left.isEmpty() || CoreTierData.isCoreItem(left)) return ItemStack.EMPTY;
        if (installedTier == null) installedTier = WirelessCoreTier.I;

        if (PortableWorkstationItem.isPortableWorkstation(left)) return ItemStack.EMPTY;

        // The Hourglass recipe consumes Core I. Untagged legacy/crafted stacks are therefore
        // treated as Core I and upgrade directly to Core II instead of consuming Core I twice.
        // Navigation and time utilities are intentionally fixed at Core I.
        if (isFixedCoreOneUtility(left)) return ItemStack.EMPTY;
        if (isDirectCoreUtility(left)) {
            int currentLevel = Math.max(1, CoreTierData.installedLevel(left));
            if (installedTier.level() != currentLevel + 1) return ItemStack.EMPTY;
            ItemStack upgraded = left.copy();
            upgraded.setCount(1);
            CoreTierData.set(upgraded, installedTier);
            return upgraded;
        }
        if (isArcaneWeapon(left)) {
            // Crafted arcane weapons are Core I even before an explicit tier tag exists.
            int currentLevel = CoreTierData.hasTier(left) ? CoreTierData.get(left).level() : 1;
            if (installedTier.level() != currentLevel + 1) return ItemStack.EMPTY;
            ItemStack upgraded = left.copy();
            upgraded.setCount(1);
            CoreTierData.set(upgraded, installedTier);
            return upgraded;
        }
        if (isAlreadyCoreUpgraded(left)) {
            WirelessCoreTier current = CoreTierData.get(left);
            if (installedTier.level() != current.level() + 1) return ItemStack.EMPTY;
            ItemStack upgraded = left.copy();
            upgraded.setCount(1);
            // Older builds could leave normal equipment with only the tier marker.
            // Restore the imbued marker while preserving enchantments, durability and name.
            if (ImbuedCompatibility.canImbue(upgraded)) ImbuedEquipmentUtil.setImbued(upgraded, true);
            CoreTierData.set(upgraded, installedTier);
            if (left.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())) {
                upgraded.set(DataComponents.CUSTOM_NAME, Component.literal("Purification Grindstone " + AlchemicalAnvilItem.roman(installedTier.level())));
            }
            return upgraded;
        }

        // Matterburst explosives are born at Core I from vanilla TNT, then upgrade
        // sequentially I -> II -> III -> IV like the other integrated utilities.

        if (installedTier != WirelessCoreTier.I) return ItemStack.EMPTY;

        ItemStack output;
        if (left.is(VeloriseEMC.TRANSMUTATION_TABLE_ITEM.get()) || left.is(VeloriseEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get())) {
            output = left.copy(); output.setCount(1);
        }
        else if (left.is(Items.FURNACE)) output = new ItemStack(VeloriseEMC.RESONANT_FURNACE_ITEM.get());
        else if (left.is(Items.SMOKER)) output = new ItemStack(VeloriseEMC.RESONANT_SMOKER_ITEM.get());
        else if (left.is(Items.BLAST_FURNACE)) output = new ItemStack(VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get());
        else if (left.is(Items.JUKEBOX)) output = new ItemStack(VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get());
        else if (left.is(Items.ENDER_CHEST)) output = new ItemStack(VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get());
        else if (left.is(Items.TNT)) output = new ItemStack(VeloriseEMC.MATTERBURST_TNT_ITEM.get());
        else if (left.is(Items.TNT_MINECART)) output = new ItemStack(VeloriseEMC.MATTERBURST_MINECART_ITEM.get());
        else if (left.is(Items.GRINDSTONE)) {
            output = new ItemStack(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get());
            output.set(DataComponents.CUSTOM_NAME, Component.literal("Purification Grindstone " + AlchemicalAnvilItem.roman(installedTier.level())));
        }
        else if (left.is(Items.CRAFTING_TABLE)) output = new ItemStack(VeloriseEMC.PORTABLE_CRAFTING_TABLE_ITEM.get());
        else if (left.is(Items.STONECUTTER)) output = new ItemStack(VeloriseEMC.PORTABLE_STONECUTTER_ITEM.get());
        else if (left.is(Items.LOOM)) output = new ItemStack(VeloriseEMC.PORTABLE_LOOM_ITEM.get());
        else if (left.is(Items.CARTOGRAPHY_TABLE)) output = new ItemStack(VeloriseEMC.PORTABLE_CARTOGRAPHY_TABLE_ITEM.get());
        else if (left.is(Items.MILK_BUCKET)) output = new ItemStack(VeloriseEMC.PURIFYING_MILK.get());
        else if (left.is(Items.SPYGLASS) && !SpecialUpgradeUtil.is(left, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS)) {
            output = left.copy(); output.setCount(1); SpecialUpgradeUtil.set(output, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS);
        }
        else if (left.is(Items.COMPASS)) output = new ItemStack(VeloriseEMC.BOUND_COMPASS.get());
        else if (left.is(Items.RECOVERY_COMPASS)) output = new ItemStack(VeloriseEMC.SOULBOUND_RECOVERY_COMPASS.get());
        else if (left.is(Items.FLINT_AND_STEEL)) output = new ItemStack(VeloriseEMC.CURSED_IGNITER.get());
        else if (left.is(Items.TURTLE_HELMET)) output = new ItemStack(VeloriseEMC.TIDAL_TURTLE_HELMET.get());
        else if (ImbuedCompatibility.canImbue(left) && !ImbuedEquipmentUtil.isImbued(left)) {
            output = left.copy(); output.setCount(1); ImbuedEquipmentUtil.setImbued(output, true);
        } else return ItemStack.EMPTY;

        // Portable workstations and fixed one-stage utilities intentionally carry no tier data.
        if (!PortableWorkstationItem.isPortableWorkstation(output) && !isFixedCoreOneUtility(output)) {
            CoreTierData.set(output, installedTier);
        }
        Component customName = left.get(DataComponents.CUSTOM_NAME);
        if (customName != null) output.set(DataComponents.CUSTOM_NAME, customName);
        else {
            String nameKey = defaultUpgradeNameKey(left);
            if (nameKey != null) output.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey));
        }
        return output;
    }


    private static boolean isDirectCoreUtility(ItemStack stack) {
        return stack.is(VeloriseEMC.MAGIC_MIRROR.get())
            || stack.is(VeloriseEMC.VOID_TRANSLOCATOR.get())
            || stack.is(VeloriseEMC.ATTRACTION_CATALYST.get())
            || stack.is(VeloriseEMC.CHALICE_OF_TRANSMUTATION.get());
    }

    private static boolean isFixedCoreOneUtility(ItemStack stack) {
        return SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS)
            || SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.BOUND_COMPASS)
            || SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.SOULBOUND_RECOVERY_COMPASS)
            || (stack.is(Items.TURTLE_HELMET) && ImbuedEquipmentUtil.isImbued(stack));
    }




    private static boolean isAlreadyCoreUpgraded(ItemStack stack) {
        if (stack.isEmpty() || PortableWorkstationItem.isPortableWorkstation(stack)
            || AlchemicalAnvilItem.isAnvil(stack)) return false;
        return (isDirectCoreUtility(stack) && CoreTierData.hasTier(stack))
            || ((stack.is(VeloriseEMC.TRANSMUTATION_TABLE_ITEM.get()) || stack.is(VeloriseEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get())) && CoreTierData.hasTier(stack))
            || stack.is(VeloriseEMC.ALCHEMICAL_HOURGLASS_ITEM.get())
            || stack.is(VeloriseEMC.RESONANT_FURNACE_ITEM.get())
            || stack.is(VeloriseEMC.RESONANT_SMOKER_ITEM.get())
            || stack.is(VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get())
            || stack.is(VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get())
            || stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())
            || stack.is(VeloriseEMC.CURSED_IGNITER.get())
            || stack.is(VeloriseEMC.PURIFYING_MILK.get())
            || stack.is(VeloriseEMC.DRAGONFIRE_SCEPTER.get())
            || stack.is(VeloriseEMC.RESONANT_ECHO.get())
            || stack.is(VeloriseEMC.INFERNO_ROD.get())
            || stack.is(VeloriseEMC.GHASTFIRE_STAFF.get())
            || stack.is(VeloriseEMC.WITHER_SCEPTER.get())
            || stack.is(VeloriseEMC.SHULKER_SEEKER.get())
            || stack.is(VeloriseEMC.GUARDIAN_LENS.get())
            || stack.is(VeloriseEMC.EVOKER_FANG_STAFF.get())
            || stack.is(VeloriseEMC.STORMCALLER.get())
            || stack.is(VeloriseEMC.FROSTBOLT_WAND.get())
            || stack.is(VeloriseEMC.WEBCASTER.get())
            || stack.is(VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get())
            || stack.is(VeloriseEMC.MATTERBURST_TNT_ITEM.get())
            || stack.is(VeloriseEMC.MATTERBURST_MINECART_ITEM.get())
            || ImbuedEquipmentUtil.isImbued(stack)
            || (CoreTierData.hasTier(stack) && ImbuedCompatibility.canImbue(stack));
    }

    private static boolean isArcaneWeapon(ItemStack stack) {
        return stack.is(VeloriseEMC.DRAGONFIRE_SCEPTER.get())
            || stack.is(VeloriseEMC.RESONANT_ECHO.get())
            || stack.is(VeloriseEMC.INFERNO_ROD.get())
            || stack.is(VeloriseEMC.GHASTFIRE_STAFF.get())
            || stack.is(VeloriseEMC.WITHER_SCEPTER.get())
            || stack.is(VeloriseEMC.SHULKER_SEEKER.get())
            || stack.is(VeloriseEMC.GUARDIAN_LENS.get())
            || stack.is(VeloriseEMC.EVOKER_FANG_STAFF.get())
            || stack.is(VeloriseEMC.STORMCALLER.get())
            || stack.is(VeloriseEMC.FROSTBOLT_WAND.get())
            || stack.is(VeloriseEMC.WEBCASTER.get());
    }


    private static String defaultUpgradeNameKey(ItemStack base) {
        if (base.is(Items.COMPASS)) return "item.velorise_emc.bound_compass";
        if (base.is(Items.RECOVERY_COMPASS)) return "item.velorise_emc.soulbound_recovery_compass";
        if (base.is(Items.SPYGLASS)) return "item.velorise_emc.far_sight_spyglass";
        if (base.is(Items.ENDER_CHEST)) return "block.velorise_emc.deepbound_ender_chest";
        if (base.is(Items.TNT)) return "block.velorise_emc.matterburst_tnt";
        if (base.is(Items.TURTLE_HELMET)) return "item.velorise_emc.tidal_turtle_helmet";
        if (base.is(Items.TNT_MINECART)) return "item.velorise_emc.matterburst_minecart";
        String imbuedName = ImbuedEquipmentUtil.specialNameKey(base);
        return imbuedName;
    }


    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementHooks.onCoreBoundResult(serverPlayer, event.getOutput());
        }
        if (player.getPersistentData().getBoolean(VanillaMachineRegistry.ANVIL_FLAG)) {
            event.setBreakChance(0.0F);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChanged(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdvancementHooks.checkFullCoreIvArmor(player);
        }
    }


    @SubscribeEvent
    public static void onLeafBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().getAbilities().instabuild && !canHarvestResonantBlock(event.getState(), event.getPlayer().getMainHandItem())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("§cYour pickaxe is not strong enough!"), true);
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel && isHourglassTargetState(event.getState())) {
            HourglassAccelerationManager.markDirtyAround(serverLevel, event.getPos());
        }
        if (event.getPlayer() instanceof ServerPlayer player) harvestNearbyMatureCrops(event, player);
        handleAreaMining(event);
    }

    private static final ThreadLocal<Boolean> IS_AREA_MINING = ThreadLocal.withInitial(() -> false);

    private static void handleAreaMining(BlockEvent.BreakEvent event) {
        if (IS_AREA_MINING.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack tool = player.getMainHandItem();
        if (!ImbuedEquipmentUtil.supportsMode(tool)) return;
        int mode = ImbuedEquipmentUtil.getMode(tool);
        if (mode <= 1) return;

        int radius = mode - 1;
        BlockPos origin = event.getPos();

        Direction facing = player.getDirection();
        float pitch = player.getXRot();

        boolean xPlane = false, yPlane = false, zPlane = false;
        if (pitch < -45.0F || pitch > 45.0F) {
            yPlane = true;
        } else if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            zPlane = true;
        } else {
            xPlane = true;
        }

        try {
            IS_AREA_MINING.set(true);
            for (int dx = (xPlane ? 0 : -radius); dx <= (xPlane ? 0 : radius); dx++) {
                for (int dy = (yPlane ? 0 : -radius); dy <= (yPlane ? 0 : radius); dy++) {
                    for (int dz = (zPlane ? 0 : -radius); dz <= (zPlane ? 0 : radius); dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos targetPos = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(targetPos);
                        if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0) continue;
                        if (!isSuitableForAreaMining(tool, event.getState(), state, level, targetPos)) continue;

                        player.gameMode.destroyBlock(targetPos);
                    }
                }
            }
        } finally {
            IS_AREA_MINING.set(false);
        }
    }

    private static boolean isSuitableForAreaMining(ItemStack tool, BlockState originState, BlockState targetState, Level level, BlockPos targetPos) {
        float targetSpeed = targetState.getDestroySpeed(level, targetPos);
        if (targetSpeed < 0.0F) return false;

        if (!canHarvestResonantBlock(targetState, tool)) return false;

        if (targetSpeed > 10.0F && !targetState.is(originState.getBlock()) && !tool.isCorrectToolForDrops(targetState)) {
            return false;
        }

        if (targetState.is(originState.getBlock())) return true;
        if (tool.isCorrectToolForDrops(targetState)) return true;
        if (tool.getDestroySpeed(targetState) > 1.0F) return true;
        return false;
    }

    private static boolean canHarvestResonantBlock(BlockState state, ItemStack tool) {
        if (state.is(VeloriseEMC.VERDANT_RESONANT_ORE.get())
            || state.is(VeloriseEMC.DEEPSLATE_VERDANT_RESONANT_ORE.get())
            || state.is(VeloriseEMC.VERDANT_RESONANT_BLOCK.get())) {
            return tool.is(Items.DIAMOND_PICKAXE) || tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 1;
        }
        if (state.is(VeloriseEMC.INFERNAL_RESONANT_ORE.get())
            || state.is(VeloriseEMC.INFERNAL_RESONANT_BLOCK.get())) {
            return tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 1;
        }
        if (state.is(VeloriseEMC.AETHERIC_RESONANT_ORE.get())
            || state.is(VeloriseEMC.AETHERIC_RESONANT_BLOCK.get())) {
            return resonantPickaxeTier(tool) >= 2;
        }
        if (state.is(VeloriseEMC.ECHOING_RESONANT_ORE.get())
            || state.is(VeloriseEMC.ECHOING_RESONANT_BLOCK.get())) {
            return resonantPickaxeTier(tool) >= 3;
        }

        if (state.is(VeloriseEMC.ALCHEMICAL_ANVIL_I.get())) {
            return tool.is(Items.DIAMOND_PICKAXE) || tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 1;
        }
        if (state.is(VeloriseEMC.ALCHEMICAL_ANVIL_II.get())) {
            return tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 2;
        }
        if (state.is(VeloriseEMC.ALCHEMICAL_ANVIL_III.get())) {
            return resonantPickaxeTier(tool) >= 3;
        }
        if (state.is(VeloriseEMC.ALCHEMICAL_ANVIL_IV.get())) {
            return resonantPickaxeTier(tool) >= 4;
        }

        if (state.is(VeloriseEMC.CONDENSED_CORE_BLOCK_I.get())) {
            return tool.is(Items.DIAMOND_PICKAXE) || tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 1;
        }
        if (state.is(VeloriseEMC.CONDENSED_CORE_BLOCK_II.get())) {
            return tool.is(Items.NETHERITE_PICKAXE) || resonantPickaxeTier(tool) >= 2;
        }
        if (state.is(VeloriseEMC.CONDENSED_CORE_BLOCK_III.get())) {
            return resonantPickaxeTier(tool) >= 3;
        }
        if (state.is(VeloriseEMC.CONDENSED_CORE_BLOCK_IV.get())) {
            return resonantPickaxeTier(tool) >= 4;
        }
        return true;
    }

    private static int resonantPickaxeTier(ItemStack tool) {
        if (tool.is(VeloriseEMC.ECHOBOUND_PICKAXE.get())) return 5;
        if (tool.is(VeloriseEMC.ECHOING_RESONANT_PICKAXE.get())) return 4;
        if (tool.is(VeloriseEMC.AETHERIC_RESONANT_PICKAXE.get())) return 3;
        if (tool.is(VeloriseEMC.INFERNAL_RESONANT_PICKAXE.get())) return 2;
        if (tool.is(VeloriseEMC.VERDANT_RESONANT_PICKAXE.get())) return 1;
        return 0;
    }



    private static void harvestNearbyMatureCrops(BlockEvent.BreakEvent event, ServerPlayer player) {
        if (CORE_HOE_HARVEST.get()) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof HoeItem) || !ImbuedEquipmentUtil.isImbued(tool)
            || !isMatureCrop(event.getState())) return;

        int radius = com.velorise.emc.common.CoreEquipmentProfile.hoeHarvestRadius(CoreTierData.get(tool).level());
        if (radius <= 0) return;
        ServerLevel level = player.serverLevel();
        BlockPos center = event.getPos();
        CORE_HOE_HARVEST.set(true);
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos target = center.offset(dx, 0, dz);
                    if (!level.hasChunkAt(target) || !isMatureCrop(level.getBlockState(target))) continue;
                    player.gameMode.destroyBlock(target);
                }
            }
        } finally {
            CORE_HOE_HARVEST.set(false);
        }
    }

    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= 2;
        return false;
    }



    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();

        // Clean up redundant loader/mod-name footers and pre-existing Shift hints to prevent duplicates
        event.getToolTip().removeIf(line -> "Velorise EMC".equals(line.getString()));
        event.getToolTip().removeIf(line -> "Hold [Shift] for details".equals(net.minecraft.ChatFormatting.stripFormatting(line.getString())));

        boolean farSightSpyglass = stack.is(Items.SPYGLASS)
            && SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS);
        String shortFlavor = shortFlavorTooltip(stack);
        if (farSightSpyglass) {
            event.getToolTip().add(Component.literal("§7Uncapped zoom while in use; scroll the mouse wheel to adjust."));
            event.getToolTip().add(Component.literal("§dFor seeing trouble long before it becomes your problem."));
        } else if (shortFlavor != null) {
            event.getToolTip().add(Component.literal(shortFlavor));
        }

        String setBonus = resonantSetBonus(stack);
        boolean isModItem = setBonus != null
            || stack.is(VeloriseEMC.ECHOBOUND_SWORD.get())
            || stack.getItem() instanceof ResonantSwordItem
            || CoreTierData.isCoreItem(stack)
            || AlchemicalAnvilItem.isAnvil(stack)
            || stack.is(VeloriseEMC.BOUND_COMPASS.get())
            || stack.is(VeloriseEMC.SOULBOUND_RECOVERY_COMPASS.get())
            || stack.is(VeloriseEMC.ATTRACTION_CATALYST.get())
            || stack.is(VeloriseEMC.CHALICE_OF_TRANSMUTATION.get())
            || stack.is(VeloriseEMC.MAGIC_MIRROR.get())
            || stack.is(VeloriseEMC.VOID_TRANSLOCATOR.get())
            || stack.is(VeloriseEMC.PURIFYING_MILK.get())
            || stack.is(VeloriseEMC.MATTERBURST_TNT_ITEM.get())
            || stack.is(VeloriseEMC.MATTERBURST_MINECART_ITEM.get())
            || stack.getItem() instanceof ArcaneWeaponItem
            || stack.is(VeloriseEMC.CURSED_IGNITER.get())
            || (stack.is(Items.FISHING_ROD) && ImbuedEquipmentUtil.isImbued(stack))
            || (stack.is(Items.TURTLE_HELMET) && ImbuedEquipmentUtil.isImbued(stack))
            || stack.is(VeloriseEMC.TIDAL_TURTLE_HELMET.get())
            || stack.is(VeloriseEMC.RESONANT_FURNACE_ITEM.get())
            || stack.is(VeloriseEMC.RESONANT_SMOKER_ITEM.get())
            || stack.is(VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get())
            || stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())
            || stack.is(VeloriseEMC.ALCHEMICAL_HOURGLASS_ITEM.get())
            || stack.is(VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get())
            || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_I_ITEM.get())
            || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_II_ITEM.get())
            || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_III_ITEM.get())
            || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_IV_ITEM.get())
            || CoreTierData.hasTier(stack)
            || ImbuedEquipmentUtil.isImbued(stack);

        // Far-Sight is deliberately a fixed, non-tiered upgrade with one inline tooltip.
        // All other supported mod items may keep a flavor line and still expose details on Shift.
        if (isModItem && !farSightSpyglass) {
            if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                event.getToolTip().add(Component.literal("§8Hold §e[Shift] §8for details"));
            } else {
                if (ImbuedEquipmentUtil.isImbued(stack) || CoreTierData.hasTier(stack)) {
                    int core = CoreTierData.get(stack).level();
                    String coreText = switch (core) {
                        case 2 -> "Infernal Core II";
                        case 3 -> "End Core III";
                        case 4 -> "Echoing Core IV";
                        default -> "Overworld Core I";
                    };
                    long repairCost = com.velorise.emc.common.CoreEquipmentProfile.repairEmcPerDurability(core);
                    String repairKey = VeloriseEMC.ClientModEvents.REPAIR_KEY.getTranslatedKeyMessage().getString();
                    event.getToolTip().add(Component.literal("§dCore: §f" + coreText));
                    if (stack.isDamageableItem()) {
                        event.getToolTip().add(Component.literal("§7EMC Repair: §fPress §e[" + repairKey + "] §7(§e" + repairCost + " EMC§7/durability)"));
                    }
                    if (ImbuedEquipmentUtil.supportsMode(stack)) {
                        int mode = ImbuedEquipmentUtil.getMode(stack);
                        String modeSize = switch (mode) { case 2 -> "3×3"; case 3 -> "5×5"; case 4 -> "7×7"; default -> "1×1"; };
                        String chargeKey = VeloriseEMC.ClientModEvents.CHARGE_KEY.getTranslatedKeyMessage().getString();
                        event.getToolTip().add(Component.literal("§bMining Mode: §e" + modeSize + " §7(Press §e[" + chargeKey + "] §7to switch)"));
                    }
                }

                if (setBonus != null) {
                    event.getToolTip().add(Component.literal("§6[Full Set]"));
                    event.getToolTip().add(Component.literal(setBonus));
                }

                if (stack.is(VeloriseEMC.ECHOBOUND_SWORD.get())) {
                    String chargeKey = VeloriseEMC.ClientModEvents.CHARGE_KEY.getTranslatedKeyMessage().getString();
                    event.getToolTip().add(Component.literal("§d[Skill] §fMelee: five opening echo pulses deal 100 damage."));
                    event.getToolTip().add(Component.literal("§d[Skill] §fHold use: Sonic Boom or Echo Wave."));
                    event.getToolTip().add(Component.literal("§7Press §e[" + chargeKey + "] §7to switch attack mode."));
                } else if (stack.is(VeloriseEMC.ECHOBOUND_PICKAXE.get())) {
                    event.getToolTip().add(Component.literal("§d[Skill] §fSeismic Slam: Hold use to charge and release a seismic shockwave."));
                    event.getToolTip().add(Component.literal("§b[Ability] §fNear-instant mining speed on all breakable blocks."));
                } else if (stack.getItem() instanceof ResonantSwordItem sword) {
                    event.getToolTip().add(Component.literal("§b[Skill] §fResonance Echo: every " + resonanceHits(stack) + " hits deals +5 damage."));
                }

                if (CoreTierData.isCoreItem(stack)) {
                    WirelessCoreTier tier = CoreTierData.get(stack);
                    event.getToolTip().add(Component.literal("§6Wireless Core " + AlchemicalAnvilItem.roman(tier.level())));
                    event.getToolTip().add(Component.literal("§7Requires Alchemical Anvil " + AlchemicalAnvilItem.roman(tier.level())));
                } else if (AlchemicalAnvilItem.isAnvil(stack)) {
                    event.getToolTip().add(Component.literal("§7Anvil Tier: §e" + AlchemicalAnvilItem.roman(AlchemicalAnvilItem.level(stack))));
                } else if (stack.is(VeloriseEMC.BOUND_COMPASS.get())) {
                    event.getToolTip().add(Component.literal("§7Right-click block to lock coordinates (256 EMC)"));
                } else if (stack.is(VeloriseEMC.SOULBOUND_RECOVERY_COMPASS.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7EMC/block: §e" + formatVehicleNumber(com.velorise.emc.common.CustomUtilityProfile.magicMirrorEmcPerBlock(tier))));
                    event.getToolTip().add(Component.literal("§7Right-click to channel death recall"));
                    event.getToolTip().add(Component.literal("§8It remembers where you died. Unfortunately, so do the mobs."));
                } else if (stack.is(VeloriseEMC.MATTERBURST_TNT_ITEM.get())
                    || stack.is(VeloriseEMC.MATTERBURST_MINECART_ITEM.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Excavation radius: §c" + com.velorise.emc.common.MatterburstProfile.radius(tier) + " blocks"));
                    event.getToolTip().add(Component.literal("§7Excavation depth: §c" + com.velorise.emc.common.MatterburstProfile.depth(tier) + " blocks"));
                    event.getToolTip().add(Component.literal("§7Maximum entity damage: §c" + formatVehicleNumber(com.velorise.emc.common.MatterburstProfile.maxDamage(tier))));
                } else if (stack.is(VeloriseEMC.PURIFYING_MILK.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal(tier >= 4
                        ? "§7Purifies: §aall harmful effect levels"
                        : "§7Purifies: §aharmful effects up to level " + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7EMC/use: §e" + com.velorise.emc.common.EffectConsumableProfile.purificationCost(tier)));
                    event.getToolTip().add(Component.literal("§7Reusable; beneficial effects are preserved."));
                } else if (stack.is(VeloriseEMC.TIDAL_TURTLE_HELMET.get())) {
                    event.getToolTip().add(Component.literal("§fActive ability: Aquatic Breathing"));
                    event.getToolTip().add(Component.literal("§fPress G while equipped to activate"));
                    event.getToolTip().add(Component.literal("§fGrants Water Breathing and Dolphin's Grace"));
                    event.getToolTip().add(Component.literal("§fConsumes "
                        + com.velorise.emc.common.FixedUtilityProfile.aquaticActivationCost() + " EMC per activation"));
                } else if (stack.is(VeloriseEMC.ATTRACTION_CATALYST.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Pull area: §b" + com.velorise.emc.common.CustomUtilityProfile.attractionDiameter(tier) + "×" + com.velorise.emc.common.CustomUtilityProfile.attractionDiameter(tier)));
                    event.getToolTip().add(Component.literal("§7EMC: §e" + com.velorise.emc.common.CustomUtilityProfile.attractionCostPerTick() + "/tick"));
                } else if (stack.is(VeloriseEMC.CHALICE_OF_TRANSMUTATION.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Area: §b" + com.velorise.emc.common.CustomUtilityProfile.chaliceDiameter(tier) + "×" + com.velorise.emc.common.CustomUtilityProfile.chaliceDiameter(tier)));
                    event.getToolTip().add(Component.literal("§7Growth EMC: §e" + com.velorise.emc.common.CustomUtilityProfile.chaliceGrowthCostPerBlock() + "/block"));
                    event.getToolTip().add(Component.literal("§7Void EMC: §evariable by fluid and blocks removed"));
                } else if (stack.is(VeloriseEMC.MAGIC_MIRROR.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Same-dimension range: §b" + formatVehicleNumber(com.velorise.emc.common.CustomUtilityProfile.magicMirrorRange(tier)) + " blocks"));
                    event.getToolTip().add(Component.literal("§7EMC: §e" + formatVehicleNumber(com.velorise.emc.common.CustomUtilityProfile.magicMirrorEmcPerBlock(tier)) + "/block"));
                    event.getToolTip().add(Component.literal("§7Cross-dimension surcharge: §e" + com.velorise.emc.common.CustomUtilityProfile.teleportCrossDimensionSurcharge()));
                } else if (stack.is(VeloriseEMC.VOID_TRANSLOCATOR.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Range: §b" + formatVehicleNumber(com.velorise.emc.common.CustomUtilityProfile.translocatorRange(tier)) + " blocks"));
                    event.getToolTip().add(Component.literal("§7EMC/use: §e" + com.velorise.emc.common.CustomUtilityProfile.translocatorCost()));
                } else if (stack.getItem() instanceof ArcaneWeaponItem weapon) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    com.velorise.emc.common.ArcaneWeaponProfile.Kind kind =
                        com.velorise.emc.common.ArcaneWeaponProfile.Kind.valueOf(weapon.kind().name());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Range: §b" + formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.range(kind, tier)) + " blocks"));
                    float damage = com.velorise.emc.common.ArcaneWeaponProfile.damage(kind, tier);
                    event.getToolTip().add(Component.literal(damage > 0.0F
                        ? (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.GUARDIAN ? "§7Damage/s: §c" : "§7Damage: §c") + formatVehicleNumber(damage)
                        : "§7Damage: §aUtility effect"));
                    event.getToolTip().add(Component.literal(kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.GUARDIAN
                        ? "§7EMC/s: §e" + com.velorise.emc.common.ArcaneWeaponProfile.emcCost(kind, tier)
                        : "§7EMC/use: §e" + com.velorise.emc.common.ArcaneWeaponProfile.emcCost(kind, tier)));
                    if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.GUARDIAN) {
                        event.getToolTip().add(Component.literal("§7Maximum channel: §e" +
                            formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.guardianBeamDurationTicks(tier) / 20.0D) + "s"));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.GHASTFIRE) {
                        event.getToolTip().add(Component.literal("§7Blast radius: §b" +
                            formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.ghastfireRadius(tier)) + " blocks"));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.WITHER) {
                        event.getToolTip().add(Component.literal("§7Skulls per cast: §b" +
                            com.velorise.emc.common.ArcaneWeaponProfile.witherSkullCount(tier)));
                        event.getToolTip().add(Component.literal("§7Casts before reload: §b" +
                            com.velorise.emc.common.ArcaneWeaponProfile.witherBurstShots()));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.SHULKER) {
                        event.getToolTip().add(Component.literal("§7Bolts per cast: §b" +
                            com.velorise.emc.common.ArcaneWeaponProfile.shulkerShotCount(tier)));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.EVOKER) {
                        event.getToolTip().add(Component.literal("§7Fang waves: §b" +
                            com.velorise.emc.common.ArcaneWeaponProfile.evokerWaveCount(tier)));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.LIGHTNING) {
                        event.getToolTip().add(Component.literal("§7Calls before reload: §b" +
                            com.velorise.emc.common.ArcaneWeaponProfile.lightningBurstUses(tier)));
                    } else if (kind == com.velorise.emc.common.ArcaneWeaponProfile.Kind.WEB) {
                        event.getToolTip().add(Component.literal("§7Web lifetime: §b" +
                            formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.webDurationTicks(tier) / 20.0D) + "s"));
                        event.getToolTip().add(Component.literal("§7Venom: §aPoison " +
                            AlchemicalAnvilItem.roman(com.velorise.emc.common.ArcaneWeaponProfile.webPoisonAmplifier(tier) + 1) + " for " +
                            formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.webVenomDurationTicks(tier) / 20.0D) + "s"));
                    }
                    event.getToolTip().add(Component.literal("§7Reload: §e" + formatVehicleNumber(com.velorise.emc.common.ArcaneWeaponProfile.cooldownTicks(kind) / 20.0D) + "s"));
                } else if (stack.is(VeloriseEMC.CURSED_IGNITER.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Damage: §c" + formatVehicleNumber(com.velorise.emc.common.UtilityItemProfile.cursedIgniterDamage(tier))));
                    event.getToolTip().add(Component.literal("§7EMC per ignition: §e" + com.velorise.emc.common.UtilityItemProfile.cursedIgniterCost(tier)));
                    event.getToolTip().add(Component.literal("§7Entity reload: §e" + formatVehicleNumber(com.velorise.emc.common.UtilityItemProfile.cursedIgniterCooldownTicks() / 20.0D) + "s"));
                } else if (stack.is(Items.FISHING_ROD) && ImbuedEquipmentUtil.isImbued(stack)) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    // The shared imbued-equipment tooltip already shows the installed Core tier.
                    // Only append fishing-specific statistics here to avoid a duplicate Tier line.
                    event.getToolTip().add(Component.literal("§7Catch count: §b×" + com.velorise.emc.common.UtilityItemProfile.fishingCatches(tier)));
                    event.getToolTip().add(Component.literal("§7EMC per cast: §e" + com.velorise.emc.common.UtilityItemProfile.fishingCastCost(tier)));
                } else if (stack.is(Items.TURTLE_HELMET) && ImbuedEquipmentUtil.isImbued(stack)) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§fActive ability: Aquatic Breathing"));
                    event.getToolTip().add(Component.literal("§fPress G while equipped to activate"));
                    event.getToolTip().add(Component.literal("§fGrants Water Breathing and Dolphin's Grace"));
                    event.getToolTip().add(Component.literal("§fConsumes "
                        + com.velorise.emc.common.FixedUtilityProfile.aquaticActivationCost() + " EMC per activation"));
                } else if (stack.is(VeloriseEMC.RESONANT_FURNACE_ITEM.get())
                    || stack.is(VeloriseEMC.RESONANT_SMOKER_ITEM.get())
                    || stack.is(VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Smelting Speed: §b×" + com.velorise.emc.common.ResonantMachineProfile.furnaceSpeedMultiplier(tier)));
                } else if (stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Extracts enchantments without destroying the item"));
                    event.getToolTip().add(Component.literal(tier >= 3
                        ? "§7Curse extraction slot: §aUnlocked"
                        : "§7Curse extraction slot: §cRequires Tier III"));
                } else if (stack.is(VeloriseEMC.ALCHEMICAL_HOURGLASS_ITEM.get())) {
                    int tier = Math.max(1, CoreTierData.installedLevel(stack));
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Range: §b" + com.velorise.emc.common.HourglassProfile.radius(tier) + " blocks"));
                    event.getToolTip().add(Component.literal("§7Bonus: §b+" + com.velorise.emc.common.HourglassProfile.bonusTicksPerPulse() + " ticks per pulse"));
                    event.getToolTip().add(Component.literal("§7EMC per successful tick: §e" + com.velorise.emc.common.HourglassProfile.emcPerSuccessfulTick()));
                } else if (stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_I_ITEM.get())
                    || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_II_ITEM.get())
                    || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_III_ITEM.get())
                    || stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_IV_ITEM.get())) {
                    int tier = stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_IV_ITEM.get()) ? 4
                        : stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_III_ITEM.get()) ? 3
                        : stack.is(VeloriseEMC.CONDENSED_CORE_BLOCK_II_ITEM.get()) ? 2 : 1;
                    event.getToolTip().add(Component.literal("§7Generator Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Output: §a" + String.format("%,d", com.velorise.emc.common.CondensedCoreGeneratorProfile.emcPerSecond(tier)) + " EMC/s"));
                    event.getToolTip().add(Component.literal("§7Collection radius: §b" + com.velorise.emc.common.CondensedCoreGeneratorProfile.radius(tier) + " blocks"));
                    event.getToolTip().add(Component.literal("§8Turns nearby condensed resonance into an EMC allowance."));
                } else if (stack.is(VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get())) {
                    int tier = Math.max(1, CoreTierData.get(stack).level());
                    event.getToolTip().add(Component.literal("§7Tier: §e" + AlchemicalAnvilItem.roman(tier)));
                    event.getToolTip().add(Component.literal("§7Accessible disc slots: §b" + com.velorise.emc.common.ResonanceJukeboxProfile.discSlots(tier)));
                    event.getToolTip().add(Component.literal("§7Supports stacked music discs and playlists"));
                    event.getToolTip().add(Component.literal("§7Playback EMC: §e" + com.velorise.emc.common.ResonanceJukeboxProfile.emcCost() + " every " + formatVehicleNumber(com.velorise.emc.common.ResonanceJukeboxProfile.costIntervalTicks() / 20.0D) + "s"));
                }
            }
        }

        // Always append EMC at the very bottom
        long emc = EMCRegistry.getUnitEMC(stack);
        if (emc > 0L) {
            event.getToolTip().add(Component.literal("§eEMC: §f" + String.format("%,d", emc)));
            if (stack.getCount() > 1) {
                long totalEmc;
                try {
                    totalEmc = Math.multiplyExact(emc, (long) stack.getCount());
                } catch (ArithmeticException overflow) {
                    totalEmc = Long.MAX_VALUE;
                }
                event.getToolTip().add(Component.literal("§eStack EMC: §f" + String.format("%,d", totalEmc)));
            }
        }
    }

    private static String shortFlavorTooltip(ItemStack stack) {
        if (stack.is(VeloriseEMC.TRANSMUTATION_TABLE_ITEM.get())) {
            return "Matter in, matter out. Economics left the server.";
        }
        if (stack.is(VeloriseEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get())) {
            return "Crafting, but the table remembers your homework.";
        }
        if (stack.is(VeloriseEMC.TRANSMUTATION_TABLET.get())) {
            return "Your personal EMC shop, now in travel size.";
        }
        if (stack.is(VeloriseEMC.ALCHEMICAL_HOURGLASS_ITEM.get())) {
            return "Time flies. Nearby machines do not get a choice.";
        }
        if (stack.is(VeloriseEMC.PHILOSOPHERS_STONE.get())) {
            return "Turns inconvenient matter into convenient matter.";
        }
        if (stack.is(VeloriseEMC.BOUND_COMPASS.get())) {
            return "Right-click a block. Coordinates acquired. Getting lost is now a choice.";
        }
        if (stack.is(VeloriseEMC.ATTRACTION_CATALYST.get())) {
            return "Dropped items suddenly remember they love you.";
        }
        if (stack.is(VeloriseEMC.CHALICE_OF_TRANSMUTATION.get())) {
            return "Makes crops grow and puddles regret existing.";
        }
        if (stack.is(VeloriseEMC.MAGIC_MIRROR.get())) {
            return "Emergency exit with excellent dramatic timing.";
        }
        if (stack.is(VeloriseEMC.EYE_OF_THE_CRUCIBLE.get())) {
            return "Stares directly into the Nether. The Nether stares back.";
        }
        if (stack.is(VeloriseEMC.AETHERIC_BULWARK_EYE.get())) {
            return "A suspiciously judgmental eye from the End.";
        }
        if (stack.is(VeloriseEMC.DEEPBOUND_EYE.get())) {
            return "It heard something underground. Probably you.";
        }
        if (stack.is(VeloriseEMC.EYE_OF_THE_LAST_ECHO.get())) {
            return "Points toward the final bad decision.";
        }
        if (stack.is(VeloriseEMC.HEART_OF_THE_LAST_ECHO.get())) {
            return "Still beating. Politely ignore that.";
        }
        if (stack.is(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get())) {
            return "Dragon-grade roofing material.";
        }
        return null;
    }

    private static int resonanceHits(ItemStack stack) {
        if (stack.is(VeloriseEMC.INFERNAL_RESONANT_SWORD.get())) return 5;
        if (stack.is(VeloriseEMC.AETHERIC_RESONANT_SWORD.get())) return 4;
        return 3;
    }

    private static String resonantSetBonus(ItemStack stack) {
        if (stack.is(VeloriseEMC.VERDANT_RESONANT_HELMET.get()) || stack.is(VeloriseEMC.VERDANT_RESONANT_CHESTPLATE.get()) || stack.is(VeloriseEMC.VERDANT_RESONANT_LEGGINGS.get()) || stack.is(VeloriseEMC.VERDANT_RESONANT_BOOTS.get())) return "§aFoundation armor for the Resonance path.";
        if (stack.is(VeloriseEMC.INFERNAL_RESONANT_HELMET.get()) || stack.is(VeloriseEMC.INFERNAL_RESONANT_CHESTPLATE.get()) || stack.is(VeloriseEMC.INFERNAL_RESONANT_LEGGINGS.get()) || stack.is(VeloriseEMC.INFERNAL_RESONANT_BOOTS.get())) return "§c-10% damage taken • +10% Nether staff power.";
        if (stack.is(VeloriseEMC.AETHERIC_RESONANT_HELMET.get()) || stack.is(VeloriseEMC.AETHERIC_RESONANT_CHESTPLATE.get()) || stack.is(VeloriseEMC.AETHERIC_RESONANT_LEGGINGS.get()) || stack.is(VeloriseEMC.AETHERIC_RESONANT_BOOTS.get())) return "§d-20% damage taken • +10% Ender staff power.";
        if (stack.is(VeloriseEMC.ECHOING_RESONANT_HELMET.get()) || stack.is(VeloriseEMC.ECHOING_RESONANT_CHESTPLATE.get()) || stack.is(VeloriseEMC.ECHOING_RESONANT_LEGGINGS.get()) || stack.is(VeloriseEMC.ECHOING_RESONANT_BOOTS.get())) return "§b-30% damage taken • +50% Resonant Echo, +10% other staff power.";
        if (stack.is(VeloriseEMC.ECHOBOUND_HELMET.get()) || stack.is(VeloriseEMC.ECHOBOUND_CHESTPLATE.get()) || stack.is(VeloriseEMC.ECHOBOUND_LEGGINGS.get()) || stack.is(VeloriseEMC.ECHOBOUND_BOOTS.get())) return "§5-80% damage taken • creative flight.";
        return null;
    }


    private static String formatVehicleNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) return Long.toString(Math.round(value));
        if (Math.abs(value * 10.0D - Math.rint(value * 10.0D)) < 0.000001D) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static long saturatingMultiply(long value, int count) {
        if (value <= 0L || count <= 0) return 0L;
        return value > Long.MAX_VALUE / count ? Long.MAX_VALUE : value * count;
    }

    private static long saturatingAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
}
