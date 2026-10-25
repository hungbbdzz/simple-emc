package com.velorise.emc;

import com.mojang.datafixers.util.Pair;
import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/** Survival locator and gated summoning item for a promotion boss. */
public final class PromotionBossEyeItem extends Item {
    public enum Kind { AETHERIC_BULWARK, DEEPBOUND_WARDEN }
    private static final int SUMMON_DISTANCE = 72;
    private final Kind kind;

    public PromotionBossEyeItem(Properties properties, Kind kind) {
        super(properties.stacksTo(16).fireResistant());
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer sp) || !correctDimension(level)) return InteractionResult.PASS;

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos clicked = context.getClickedPos();
        BlockPos center = kind == Kind.AETHERIC_BULWARK
            ? PromotionBossArenaService.findBulwarkAltarCenter(serverLevel, clicked)
            : PromotionBossArenaService.findDeepboundAltarCenter(serverLevel, clicked);
        if (center == null) return InteractionResult.PASS;

        if (!sp.getAbilities().instabuild) {
            WorldProgressionState state = WorldProgressionData.get(sp.server).snapshot();
            WorldTierStage stage = kind == Kind.AETHERIC_BULWARK ? WorldTierStage.END_CONQUEST : WorldTierStage.DEEP_WORLD_CONQUEST;
            WorldProgressionObjective bossObjective = kind == Kind.AETHERIC_BULWARK ? WorldProgressionObjective.AETHERIC_BULWARK : WorldProgressionObjective.DEEPBOUND_WARDEN;
            for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
                if (objective.stage() == stage && objective != bossObjective && !state.objectiveComplete(objective)) {
                    sp.displayClientMessage(Component.literal("The eye remains dormant. Missing: " + objective.displayName() + " (" + state.count(objective) + "/" + state.required(objective) + ")."), true);
                    return InteractionResult.FAIL;
                }
            }
        }

        ArenaSummonService.BossKind bossKind = kind == Kind.AETHERIC_BULWARK
            ? ArenaSummonService.BossKind.AETHERIC_BULWARK : ArenaSummonService.BossKind.DEEPBOUND_WARDEN;
        if (!ArenaSummonService.start(serverLevel, center, bossKind)) {
            sp.displayClientMessage(Component.literal("The altar is already channeling power or the boss is already present."), true);
            return InteractionResult.FAIL;
        }
        if (!sp.getAbilities().instabuild) context.getItemInHand().shrink(1);
        level.playSound(null, clicked, kind == Kind.AETHERIC_BULWARK ? SoundEvents.END_PORTAL_FRAME_FILL : SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.0F, kind == Kind.AETHERIC_BULWARK ? 1.25F : 0.7F);
        sp.displayClientMessage(Component.literal(kind == Kind.AETHERIC_BULWARK
            ? "The altar begins to condense aetheric energy..."
            : "The altar begins to gather deepbound energy..."), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        ServerPlayer sp = (ServerPlayer) player;
        if (!correctDimension(level)) {
            sp.displayClientMessage(Component.literal(kind == Kind.AETHERIC_BULWARK ? "The Aetheric Eye only responds in The End." : "The Deepbound Eye only responds in the Overworld."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) {
            WorldProgressionState state = WorldProgressionData.get(sp.server).snapshot();
            WorldTierStage stage = kind == Kind.AETHERIC_BULWARK ? WorldTierStage.END_CONQUEST : WorldTierStage.DEEP_WORLD_CONQUEST;
            WorldProgressionObjective bossObjective = kind == Kind.AETHERIC_BULWARK ? WorldProgressionObjective.AETHERIC_BULWARK : WorldProgressionObjective.DEEPBOUND_WARDEN;
            for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
                if (objective.stage() == stage && objective != bossObjective && !state.objectiveComplete(objective)) {
                    sp.displayClientMessage(Component.literal("The eye remains dormant. Missing: " + objective.displayName() + " (" + state.count(objective) + "/" + state.required(objective) + ")."), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BossArenaRegistryData.Kind registryKind = kind == Kind.AETHERIC_BULWARK
            ? BossArenaRegistryData.Kind.AETHERIC : BossArenaRegistryData.Kind.DEEPBOUND;
        BlockPos target = BossArenaRegistryData.get(sp.server).nearest(registryKind, player.blockPosition());
        boolean targetIsArena = target != null;
        if (target == null) target = locate(serverLevel, player.blockPosition());
        if (target == null) {
            sp.displayClientMessage(Component.literal("The eye cannot sense a suitable arena landmark nearby."), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos arena = targetIsArena ? target
            : (kind == Kind.AETHERIC_BULWARK
                ? new BlockPos(target.getX() + 48, Math.max(58, target.getY()), target.getZ())
                : new BlockPos(target.getX() + 48, -49, target.getZ()));
        double horizontal = Math.sqrt(player.blockPosition().distSqr(new BlockPos(arena.getX(), player.blockPosition().getY(), arena.getZ())));
        if (horizontal > SUMMON_DISTANCE) {
            spawnGuideLine(serverLevel, sp, arena, kind);
            player.getCooldowns().addCooldown(this, 30);
            return InteractionResultHolder.success(stack);
        }
        if (kind == Kind.AETHERIC_BULWARK ? !PromotionBossArenaService.hasBulwarkArena(serverLevel, arena)
            : !PromotionBossArenaService.hasDeepboundArena(serverLevel, arena)) {
            if (kind == Kind.AETHERIC_BULWARK) PromotionBossArenaService.buildBulwarkArena(serverLevel, arena);
            else PromotionBossArenaService.buildDeepboundArena(serverLevel, arena);
            level.playSound(null, player.blockPosition(), kind == Kind.AETHERIC_BULWARK ? SoundEvents.END_PORTAL_SPAWN : SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.5F, kind == Kind.AETHERIC_BULWARK ? 1.3F : 0.55F);
            player.getCooldowns().addCooldown(this, 80);
            sp.displayClientMessage(Component.literal(kind == Kind.AETHERIC_BULWARK
                ? "The Aetheric arena has formed. Use the eye on the central altar to summon the boss."
                : "The Deepbound arena has formed. Use the eye on the central altar to summon the boss."), true);
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, 40);
        sp.displayClientMessage(Component.literal("The arena is ready. Use the eye on the altar to begin the summoning."), true);
        return InteractionResultHolder.success(stack);
    }

    private static void spawnGuideLine(ServerLevel serverLevel, ServerPlayer sp, BlockPos target, Kind kind) {
        Vec3 start = sp.getEyePosition();
        Vec3 dest = Vec3.atCenterOf(target);
        Vec3 delta = dest.subtract(start);
        Vec3 dir = delta.normalize();

        for (int i = 2; i <= 60; i++) {
            double d = i * 0.5D;
            double px = start.x + dir.x * d;
            double py = start.y + dir.y * d;
            double pz = start.z + dir.z * d;

            if (kind == Kind.AETHERIC_BULWARK) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 2, 0.05, 0.05, 0.05, 0.01);
                }
            } else {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, px, py, pz, 2, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        if (kind == Kind.AETHERIC_BULWARK) {
            serverLevel.playSound(null, sp.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.2F, 1.25F);
        } else {
            serverLevel.playSound(null, sp.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8F, 0.7F);
        }

        long dist = Math.round(Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        String name = kind == Kind.AETHERIC_BULWARK ? "Aetheric Bulwark Arena" : "Deepbound Warden Arena";
        sp.displayClientMessage(Component.literal("The eye points toward the " + name + " (" + dist + " blocks away)."), true);
    }

    private boolean correctDimension(Level level) {
        return kind == Kind.AETHERIC_BULWARK ? level.dimension() == Level.END : level.dimension() == Level.OVERWORLD;
    }

    private BlockPos locate(ServerLevel level, BlockPos origin) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:" + (kind == Kind.AETHERIC_BULWARK ? "end_city" : "ancient_city"));
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        java.util.Optional<Holder.Reference<Structure>> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), origin, 160, false);
        return found == null ? null : found.getFirst();
    }
}
