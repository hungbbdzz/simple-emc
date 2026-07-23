package com.velorise.emc;

import com.velorise.emc.common.FixedUtilityProfile;
import com.velorise.emc.common.UtilityItemProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/** Item-focused Vanilla reworks that remain based on the original Vanilla stacks. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaItemUpgradeEvents {
    private static final String RECOVERY_START = "VeloriseEMCRecoveryStart";
    private static final String RECOVERY_HAND = "VeloriseEMCRecoveryHand";
    private static final String RECOVERY_X = "VeloriseEMCRecoveryX";
    private static final String RECOVERY_Y = "VeloriseEMCRecoveryY";
    private static final String RECOVERY_Z = "VeloriseEMCRecoveryZ";
    private static final String FAR_SIGHT_ACTIVE = "VeloriseEMCFarSightActive";
    private static final String FAR_SIGHT_OWNS_NIGHT_VISION = "VeloriseEMCFarSightOwnsNightVision";

    private VanillaItemUpgradeEvents() {}

    @SubscribeEvent
    public static void onBoundCompassUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(VeloriseEMC.BOUND_COMPASS.get())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!EmcTransactionService.spend(player, UtilityItemProfile.boundCompassLockCost())) {
            player.displayClientMessage(Component.literal("§cNot enough EMC. Need §e" + UtilityItemProfile.boundCompassLockCost() + " EMC"), true);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("BoundDim", event.getLevel().dimension().location().toString());
        tag.putInt("BoundX", event.getPos().getX());
        tag.putInt("BoundY", event.getPos().getY());
        tag.putInt("BoundZ", event.getPos().getZ());

        player.displayClientMessage(Component.literal("§aBound Compass locked to §e" + event.getPos().toShortString()
            + " §7(-" + UtilityItemProfile.boundCompassLockCost() + " EMC)"), true);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    @SubscribeEvent
    public static void onSpecialRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(VeloriseEMC.SOULBOUND_RECOVERY_COMPASS.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            if (event.getEntity() instanceof ServerPlayer player) toggleRecoveryChannel(player, event.getHand());
            return;
        }

        if (stack.is(Items.FISHING_ROD) && ImbuedEquipmentUtil.isImbued(stack)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            if (MultiFishingManager.hasActiveCast(player)) {
                MultiFishingManager.retrieve(player);
                return;
            }

            int coreLevel = CoreTierData.get(stack).level();
            int hooks = UtilityItemProfile.fishingCatches(coreLevel);
            long castCost = MultiFishingManager.castEmcCost(player, stack);
            if (!EmcTransactionService.canSpend(player, castCost)) {
                player.displayClientMessage(Component.literal(
                    "§cNot enough EMC for a Core-powered catch. Need §e" + castCost + " EMC."), true);
                return;
            }
            if (!MultiFishingManager.cast(player, event.getHand(), stack, hooks, castCost)) {
                player.displayClientMessage(Component.literal("§cUnable to create the fishing cast."), true);
                return;
            }
            player.displayClientMessage(Component.literal(
                "§bCore " + coreLevel + " Fishing: §e" + hooks + "× catch §7| Fixed successful-catch cost: §e"
                    + castCost + " EMC"), true);
            return;
        }
    }

    @SubscribeEvent
    public static void onCursedIgniter(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(VeloriseEMC.CURSED_IGNITER.get())
            || !(event.getTarget() instanceof LivingEntity target)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            player.displayClientMessage(Component.literal("§cCursed Igniter is reloading."), true);
            return;
        }

        int coreLevel = CoreTierData.get(stack).level();
        long cost = UtilityItemProfile.cursedIgniterCost(coreLevel);
        if (!EmcTransactionService.spend(player, cost)) {
            EmcFailureMessages.show(player, cost);
            return;
        }
        float damage = UtilityItemProfile.cursedIgniterDamage(coreLevel);
        int fireSeconds = UtilityItemProfile.cursedIgniterFireSeconds(coreLevel);
        spawnCursedFlameBolt(player, target);
        target.setSecondsOnFire(fireSeconds);
        float magicPart = damage * 0.5F;
        float physicalPart = damage - magicPart;
        target.hurt(player.damageSources().magic(), magicPart);
        target.hurt(player.damageSources().playerAttack(player), physicalPart);
        player.getCooldowns().addCooldown(stack.getItem(), UtilityItemProfile.cursedIgniterCooldownTicks());
        ImbuedEquipmentUtil.setActionCooldownTick(stack,
            player.level().getGameTime() + UtilityItemProfile.cursedIgniterCooldownTicks());
        player.displayClientMessage(Component.literal("§5Cursed Flame §7(-§e" + cost + " EMC§7)"), true);
    }


    private static void spawnCursedFlameBolt(ServerPlayer player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.35));
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int)Math.ceil(delta.length() * 4.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double)steps));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z,
                1, 0.015D, 0.015D, 0.015D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onFishingPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MultiFishingManager.discard(player);
        }
    }

    @SubscribeEvent
    public static void onFishingPlayerChangedDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MultiFishingManager.discard(player);
    }

    @SubscribeEvent
    public static void onFishingPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MultiFishingManager.discard(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MultiFishingManager.tick(player);
        processRecoveryChannel(player);
        processFarSightSpyglass(player);
    }

    public static void activateAquaticBreathing(ServerPlayer player) {
        int helmetCore = activeTidalHelmetCore(player);
        if (helmetCore <= 0) {
            player.displayClientMessage(Component.literal("§cEquip an Imbued Turtle Helmet first."), true);
            return;
        }
        long cost = FixedUtilityProfile.aquaticActivationCost();
        if (!EmcTransactionService.spend(player, cost)) {
            EmcFailureMessages.show(player, cost);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
            FixedUtilityProfile.aquaticWaterBreathingTicks(), 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,
            FixedUtilityProfile.aquaticDolphinsGraceTicks(), 0, true, false, true));
        player.serverLevel().sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0D, player.getZ(),
            24, 0.5D, 0.8D, 0.5D, 0.08D);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
            SoundSource.PLAYERS, 0.8F, 1.15F);
        player.displayClientMessage(Component.literal("§bTidal Turtle Helmet activated §7(-§e" + cost + " EMC§7)"), true);
    }



    public static int activeTidalHelmetCore(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(VeloriseEMC.TIDAL_TURTLE_HELMET.get()) ? 1 : 0;
    }

    private static void processFarSightSpyglass(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        boolean scoped = player.isUsingItem()
            && player.getUseItem().is(Items.SPYGLASS)
            && SpecialUpgradeUtil.is(player.getUseItem(), SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS);
        if (scoped) {
            if (!data.getBoolean(FAR_SIGHT_ACTIVE)) {
                data.putBoolean(FAR_SIGHT_ACTIVE, true);
                data.putBoolean(FAR_SIGHT_OWNS_NIGHT_VISION, !player.hasEffect(MobEffects.NIGHT_VISION));
            }
            if (data.getBoolean(FAR_SIGHT_OWNS_NIGHT_VISION)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, false));
            }
            return;
        }

        if (!data.getBoolean(FAR_SIGHT_ACTIVE)) return;
        if (data.getBoolean(FAR_SIGHT_OWNS_NIGHT_VISION)) player.removeEffect(MobEffects.NIGHT_VISION);
        data.remove(FAR_SIGHT_ACTIVE);
        data.remove(FAR_SIGHT_OWNS_NIGHT_VISION);
    }





    private static void toggleRecoveryChannel(ServerPlayer player, InteractionHand hand) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(RECOVERY_START)) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§7Recovery teleport canceled"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(Items.RECOVERY_COMPASS)) {
            player.displayClientMessage(Component.literal("§cSoulbound Recovery Compass is cooling down"), true);
            return;
        }
        Optional<GlobalPos> death = player.getLastDeathLocation();
        if (death.isEmpty()) {
            player.displayClientMessage(Component.literal("§cNo death location is stored"), true);
            return;
        }
        ServerLevel targetLevel = player.server.getLevel(death.get().dimension());
        if (targetLevel == null) {
            player.displayClientMessage(Component.literal("§cThe death dimension is unavailable"), true);
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(death.get().pos());
        long cost = TeleportCostUtil.cost(player, targetLevel, target);
        if (!EmcTransactionService.canSpend(player, cost)) {
            player.displayClientMessage(Component.literal("§cNot enough available EMC. Need §e" + cost + " EMC"), true);
            return;
        }

        data.putLong(RECOVERY_START, player.level().getGameTime());
        data.putInt(RECOVERY_HAND, hand == InteractionHand.MAIN_HAND ? 0 : 1);
        data.putDouble(RECOVERY_X, player.getX());
        data.putDouble(RECOVERY_Y, player.getY());
        data.putDouble(RECOVERY_Z, player.getZ());
        player.displayClientMessage(Component.literal("§dChanneling death recall... §7Remain still for "
            + formatTicksAsDuration(UtilityItemProfile.recoveryChannelTicks())), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    private static void processRecoveryChannel(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RECOVERY_START)) return;

        InteractionHand hand = data.getInt(RECOVERY_HAND) == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.RECOVERY_COMPASS)
            || !SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.SOULBOUND_RECOVERY_COMPASS)) {
            clearRecoveryChannel(player);
            return;
        }

        double dx = player.getX() - data.getDouble(RECOVERY_X);
        double dy = player.getY() - data.getDouble(RECOVERY_Y);
        double dz = player.getZ() - data.getDouble(RECOVERY_Z);
        double movementTolerance = UtilityItemProfile.recoveryMovementTolerance();
        if (dx * dx + dy * dy + dz * dz > movementTolerance * movementTolerance) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§cRecovery teleport interrupted by movement"), true);
            return;
        }

        long elapsed = player.level().getGameTime() - data.getLong(RECOVERY_START);
        if (elapsed % 5L == 0L) {
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.35, 0.55, 0.35, 0.05);
        }
        if (elapsed < UtilityItemProfile.recoveryChannelTicks()) return;

        Optional<GlobalPos> death = player.getLastDeathLocation();
        if (death.isEmpty()) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§cDeath location is no longer available"), true);
            return;
        }
        ServerLevel targetLevel = player.server.getLevel(death.get().dimension());
        if (targetLevel == null) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§cThe death dimension is unavailable"), true);
            return;
        }
        Vec3 target = findSafeTarget(targetLevel, death.get().pos());
        if (target == null) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§cNo safe space was found near the death point"), true);
            return;
        }
        long cost = TeleportCostUtil.cost(player, targetLevel, target);
        if (!EmcTransactionService.spend(player, cost)) {
            clearRecoveryChannel(player);
            player.displayClientMessage(Component.literal("§cNot enough EMC. Need §e" + cost + " EMC"), true);
            return;
        }

        ServerLevel departure = (ServerLevel)player.level();
        departure.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.6, 0.5, 0.35);
        player.teleportTo(targetLevel, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        targetLevel.sendParticles(ParticleTypes.PORTAL, target.x, target.y + 1.0, target.z, 30, 0.5, 0.6, 0.5, 0.35);
        player.getCooldowns().addCooldown(Items.RECOVERY_COMPASS, UtilityItemProfile.recoveryCooldownTicks());
        clearRecoveryChannel(player);
        player.displayClientMessage(Component.literal("§aReturned to death point §7(-" + cost + " EMC)"), true);
        targetLevel.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static Vec3 findSafeTarget(ServerLevel level, BlockPos origin) {
        int horizontalRadius = UtilityItemProfile.recoveryHorizontalSearchRadius();
        int verticalRadius = UtilityItemProfile.recoveryVerticalSearchRadius();
        int[] verticalOffsets = new int[verticalRadius * 2 + 1];
        for (int y = 1; y <= verticalRadius; y++) {
            verticalOffsets[y] = y;
            verticalOffsets[verticalRadius + y] = -y;
        }
        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int y : verticalOffsets) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos feet = origin.offset(x, y, z);
                        BlockPos head = feet.above();
                        BlockPos floor = feet.below();
                        if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            && level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                            && !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                            && level.getFluidState(feet).isEmpty()
                            && level.getFluidState(head).isEmpty()) {
                            return Vec3.atBottomCenterOf(feet);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void clearRecoveryChannel(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(RECOVERY_START);
        data.remove(RECOVERY_HAND);
        data.remove(RECOVERY_X);
        data.remove(RECOVERY_Y);
        data.remove(RECOVERY_Z);
    }


    private static String formatTicksAsDuration(int ticks) {
        double seconds = Math.max(0, ticks) / 20.0D;
        String value = seconds == Math.rint(seconds)
            ? Long.toString(Math.round(seconds))
            : String.format(java.util.Locale.ROOT, "%.1f", seconds);
        return value + ("1".equals(value) ? " second" : " seconds");
    }

    private static String roman(int mode) {
        return switch (mode) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    private static void damageItem(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (player.getAbilities().instabuild) return;
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            stack.shrink(1);
            player.broadcastBreakEvent(hand);
        } else {
            stack.setDamageValue(damage);
        }
    }
}
