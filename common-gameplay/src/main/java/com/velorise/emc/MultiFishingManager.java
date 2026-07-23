package com.velorise.emc;

import com.velorise.emc.common.FishingEconomyProfile;
import com.velorise.emc.common.UtilityItemProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned single-bobber casts with one shared Vanilla loot roll.
 *
 * <p>One real Vanilla bobber owns the bite timer and retrieval. A successful result is
 * duplicated according to Core tier after the normal catch resolves. The successful-catch
 * EMC price is fixed; Core progression increases output rather than operating cost.</p>
 */
public final class MultiFishingManager {
    private static final Map<UUID, CastGroup> GROUPS = new HashMap<>();
    private static final double LOOT_CAPTURE_RADIUS = 3.0D;

    private MultiFishingManager() {}

    public static boolean hasActiveCast(ServerPlayer player) {
        return GROUPS.containsKey(player.getUUID());
    }

    public static long castEmcCost(ServerPlayer player, ItemStack rod) {
        return isValidRod(rod) ? UtilityItemProfile.fishingCastEmcCost() : 0L;
    }

    public static long totalStake(int hookCount, long configuredCost) {
        return FishingEconomyProfile.totalStake(hookCount, configuredCost);
    }

    public static boolean cast(ServerPlayer player, InteractionHand hand, ItemStack rod,
                               int hookCount, long castEmcCost) {
        if (hasActiveCast(player) || !isValidRod(rod) || hookCount <= 0 || castEmcCost < 0L) return false;
        if (player.fishing != null && !player.fishing.isRemoved()) return false;

        ServerLevel level = player.serverLevel();
        int luck = fishingMetric(level, rod, player, false);
        int lure = fishingMetric(level, rod, player, true);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 origin = eye.add(look.scale(0.35D)).add(0.0D, -0.15D, 0.0D);
        HitResult hit = level.clip(new ClipContext(
            eye,
            eye.add(look.scale(24.0D)),
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.ANY,
            player
        ));
        Vec3 center = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(16.0D)) : hit.getLocation();

        // Use exactly one authoritative bobber. Core rewards are duplicated only
        // after a successful Vanilla catch; fake secondary hooks caused bite animation
        // and retrieval desynchronization.
        List<UUID> hookIds = new ArrayList<>(1);
        FishingHook primary = new FishingHook(player, level, luck, lure);
        primary.setPos(origin.x, origin.y, origin.z);

        Vec3 delta = center.subtract(origin);
        if (delta.lengthSqr() < 0.0001D) delta = look;
        Vec3 motion = delta.normalize().scale(0.65D);
        primary.setDeltaMovement(motion.x, motion.y + 0.10D, motion.z);

        if (level.addFreshEntity(primary)) hookIds.add(primary.getUUID());

        if (hookIds.size() != 1) {
            discardHooks(level, hookIds);
            player.fishing = null;
            return false;
        }

        long stake = totalStake(hookCount, castEmcCost);
        GROUPS.put(player.getUUID(), new CastGroup(level.dimension(), hand, primary.getUUID(), hookIds,
            hookCount, stake));
        player.fishing = primary;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
            SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        player.awardStat(Stats.ITEM_USED.get(rod.getItem()));
        player.swing(hand, true);
        return true;
    }

    public static boolean retrieve(ServerPlayer player) {
        CastGroup group = GROUPS.remove(player.getUUID());
        if (group == null) return false;

        ServerLevel level = player.getServer() == null ? null : player.getServer().getLevel(group.dimension);
        ItemStack rod = player.getItemInHand(group.hand);
        boolean resolvedInteraction = false;
        boolean paidCatch = false;

        if (level != null && isValidRod(rod)) {
            Entity entity = level.getEntity(group.primaryHookId);
            if (entity instanceof FishingHook primary && !primary.isRemoved()) {
                Vec3 captureCenter = primary.position();
                AABB captureBox = new AABB(captureCenter, captureCenter).inflate(LOOT_CAPTURE_RADIUS);
                AABB experienceBox = player.getBoundingBox().inflate(4.0D);
                Set<UUID> previousItems = entityIds(level.getEntitiesOfClass(ItemEntity.class, captureBox));
                Set<UUID> previousExperience = entityIds(level.getEntitiesOfClass(ExperienceOrb.class, experienceBox));

                // Only this hook owns the Vanilla wait/bite timer and retrieval attempt.
                player.fishing = primary;
                int retrievalResult = primary.retrieve(rod);
                List<ItemEntity> caughtItems = freshEntities(
                    level.getEntitiesOfClass(ItemEntity.class, captureBox), previousItems);
                List<ExperienceOrb> caughtExperience = freshEntities(
                    level.getEntitiesOfClass(ExperienceOrb.class, experienceBox), previousExperience);

                if (!caughtItems.isEmpty()) {
                    resolvedInteraction = true;
                    if (!EmcTransactionService.spend(player, group.totalStake)) {
                        discardEntities(caughtItems);
                        discardEntities(caughtExperience);
                        player.displayClientMessage(Component.literal(
                            "§cCore-powered catch canceled: need §e" + group.totalStake + " EMC."), true);
                    } else {
                        duplicateSharedLoot(level, player, caughtItems, group.hookCount);
                        paidCatch = true;
                        damageRodOnce(rod, player, group.hand);
                        displayCatchResult(player, caughtItems, group);
                    }
                } else if (retrievalResult > 0) {
                    // Hooked entities resolve normally and do not consume EMC bait.
                    resolvedInteraction = true;
                    damageRodOnce(rod, player, group.hand);
                }
            }

            // The authoritative hook is the only entity and loot roll.
            discardHooks(level, group.hookIds);
        } else if (level != null) {
            discardHooks(level, group.hookIds);
        }

        player.fishing = null;
        if (!resolvedInteraction && level != null) {
            player.displayClientMessage(Component.literal("§7No catch. No EMC bait was consumed."), true);
        }
        if ((resolvedInteraction || paidCatch) && level != null) {
            player.awardStat(Stats.ITEM_USED.get(rod.getItem()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1.0F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            player.swing(group.hand, true);
        }
        return true;
    }

    public static void tick(ServerPlayer player) {
        CastGroup group = GROUPS.get(player.getUUID());
        if (group == null) return;
        if (!player.isAlive() || player.isRemoved() || !player.level().dimension().equals(group.dimension)
            || !isValidRod(player.getItemInHand(group.hand))) {
            discard(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        Entity primaryEntity = level.getEntity(group.primaryHookId);
        if (!(primaryEntity instanceof FishingHook primary) || primary.isRemoved()) {
            // Do not promote a secondary hook: that would replace the authoritative timer.
            discard(player);
            return;
        }

        player.fishing = primary;
    }

    public static void discard(ServerPlayer player) {
        CastGroup group = GROUPS.remove(player.getUUID());
        if (group == null) {
            player.fishing = null;
            return;
        }
        if (player.getServer() != null) {
            ServerLevel level = player.getServer().getLevel(group.dimension);
            if (level != null) discardHooks(level, group.hookIds);
        }
        player.fishing = null;
    }

    private static boolean isValidRod(ItemStack stack) {
        return stack.is(Items.FISHING_ROD) && ImbuedEquipmentUtil.isImbued(stack);
    }

    private static void discardHooks(ServerLevel level, List<UUID> hookIds) {
        for (UUID id : hookIds) {
            Entity entity = level.getEntity(id);
            if (entity instanceof FishingHook hook && !hook.isRemoved()) hook.discard();
        }
    }

    private static int fishingMetric(ServerLevel level, ItemStack rod, ServerPlayer player, boolean lure) {
        String[] names = lure
            ? new String[] {"getFishingSpeedBonus", "getFishingTimeReduction"}
            : new String[] {"getFishingLuckBonus"};

        for (String name : names) {
            for (Method method : EnchantmentHelper.class.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(name)) continue;
                Object[] arguments = resolveArguments(method.getParameterTypes(), level, rod, player);
                if (arguments == null) continue;
                try {
                    Object result = method.invoke(null, arguments);
                    if (!(result instanceof Number number)) continue;
                    double value = number.doubleValue();
                    if (name.equals("getFishingTimeReduction")) value *= 20.0D;
                    return Math.max(0, (int)Math.round(value));
                } catch (ReflectiveOperationException ignored) {
                    // Try another mapped overload.
                }
            }
        }
        return 0;
    }

    private static Object[] resolveArguments(Class<?>[] parameterTypes, ServerLevel level,
                                             ItemStack rod, ServerPlayer player) {
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type.isInstance(level)) arguments[i] = level;
            else if (type.isInstance(rod)) arguments[i] = rod;
            else if (type.isInstance(player)) arguments[i] = player;
            else return null;
        }
        return arguments;
    }

    private static <T extends Entity> Set<UUID> entityIds(List<T> entities) {
        Set<UUID> ids = new HashSet<>(entities.size());
        for (Entity entity : entities) ids.add(entity.getUUID());
        return ids;
    }

    private static <T extends Entity> List<T> freshEntities(List<T> entities, Set<UUID> previousIds) {
        List<T> fresh = new ArrayList<>();
        for (T entity : entities) {
            if (!previousIds.contains(entity.getUUID())) fresh.add(entity);
        }
        return fresh;
    }

    private static void discardEntities(List<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (!entity.isRemoved()) entity.discard();
        }
    }

    private static void duplicateSharedLoot(ServerLevel level, ServerPlayer player,
                                            List<ItemEntity> caughtItems, int hookCount) {
        for (ItemEntity source : caughtItems) {
            ItemStack result = source.getItem();
            Vec3 baseMotion = source.getDeltaMovement();
            for (int copyIndex = 1; copyIndex < hookCount; copyIndex++) {
                ItemEntity duplicate = new ItemEntity(level, source.getX(), source.getY(), source.getZ(), result.copy());
                double spreadX = (player.getRandom().nextDouble() - 0.5D) * 0.06D;
                double spreadZ = (player.getRandom().nextDouble() - 0.5D) * 0.06D;
                duplicate.setDeltaMovement(baseMotion.add(spreadX, 0.0D, spreadZ));
                level.addFreshEntity(duplicate);
            }
        }
    }

    private static void displayCatchResult(ServerPlayer player, List<ItemEntity> caughtItems, CastGroup group) {
        long singleRollValue = 0L;
        for (ItemEntity entity : caughtItems) {
            singleRollValue = safeAdd(singleRollValue, EMCRegistry.getEMC(entity.getItem()));
        }
        long totalValue = safeMultiply(singleRollValue, group.hookCount);
        long net = totalValue >= group.totalStake
            ? totalValue - group.totalStake
            : -(group.totalStake - totalValue);
        String rewardName = caughtItems.size() == 1
            ? caughtItems.get(0).getItem().getHoverName().getString()
            : "shared loot";
        String netColor = net >= 0L ? "§a+" : "§c";
        player.displayClientMessage(Component.literal(
            "§bCore-powered catch: §e" + group.hookCount + "× " + rewardName
                + " §7| Fixed catch cost: §e" + group.totalStake + " EMC"
                + " §7| Loot EMC: §e" + totalValue
                + " §7| Net: " + netColor + net + " EMC"), true);
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) return left;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long safeMultiply(long value, int multiplier) {
        if (value <= 0L || multiplier <= 0) return 0L;
        return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
    }

    private static void damageRodOnce(ItemStack rod, ServerPlayer player, InteractionHand hand) {
        if (rod.isEmpty() || player.getAbilities().instabuild || !rod.isDamageableItem()) return;
        int damage = rod.getDamageValue() + 1;
        if (damage >= rod.getMaxDamage()) {
            net.minecraft.world.item.Item brokenItem = rod.getItem();
            rod.shrink(1);
            ItemBreakCompat.broadcast(player, hand, brokenItem);
        } else {
            rod.setDamageValue(damage);
        }
    }

    private static final class CastGroup {
        private final ResourceKey<Level> dimension;
        private final InteractionHand hand;
        private final UUID primaryHookId;
        private final List<UUID> hookIds;
        private final int hookCount;
        private final long totalStake;

        private CastGroup(ResourceKey<Level> dimension, InteractionHand hand, UUID primaryHookId,
                          List<UUID> hookIds, int hookCount, long totalStake) {
            this.dimension = dimension;
            this.hand = hand;
            this.primaryHookId = primaryHookId;
            this.hookIds = hookIds;
            this.hookCount = hookCount;
            this.totalStake = totalStake;
        }
    }
}
