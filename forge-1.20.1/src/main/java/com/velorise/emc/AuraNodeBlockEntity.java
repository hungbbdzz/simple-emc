package com.velorise.emc;

import com.velorise.emc.common.AuraNodeProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class AuraNodeBlockEntity extends BlockEntity {
    private boolean activated;
    private boolean recharging;
    private long storedEmc;
    private long rechargeEndGameTime;

    public AuraNodeBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.RESONANCE_SPIRE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraNodeBlockEntity node) {
        if (!(level instanceof ServerLevel serverLevel) || !AuraNodeProfile.enabled()) return;
        int worldTier = WorldProgressionData.get(serverLevel.getServer()).snapshot().worldTier();
        node.syncWorldTier(worldTier >= node.getTier());
        if (!node.activated) return;
        long gameTime = level.getGameTime();
        if (Math.floorMod(gameTime + pos.asLong(), AuraNodeProfile.pulseIntervalTicks()) != 0L) return;
        node.pulse(serverLevel, gameTime);
    }

    public void activate() {
        if (activated) return;
        activated = true;
        recharging = false;
        storedEmc = getCapacity();
        rechargeEndGameTime = 0L;
        updateActiveState(true);
        setChanged();
    }

    private void syncWorldTier(boolean unlocked) {
        if (unlocked && !activated) {
            activate();
        } else if (!unlocked && activated) {
            activated = false;
            recharging = false;
            storedEmc = 0L;
            rechargeEndGameTime = 0L;
            updateActiveState(false);
            setChanged();
        }
    }

    private void pulse(ServerLevel level, long gameTime) {
        if (recharging) {
            if (gameTime >= rechargeEndGameTime) {
                storedEmc = getCapacity();
                recharging = false;
                rechargeEndGameTime = 0L;
                updateActiveState(true);
                level.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D,
                    worldPosition.getZ() + 0.5D, 24, 0.7D, 1.0D, 0.7D, 0.04D);
                setChanged();
                return;
            }
            siphonNearbyPlayers(level);
            level.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D,
                worldPosition.getZ() + 0.5D, 5, 1.0D, 0.6D, 1.0D, 0.01D);
            return;
        }

        if (storedEmc <= 0L) {
            beginRecharge(gameTime);
            return;
        }

        int radius = AuraNodeProfile.outputRadius(getTier());
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(radius),
            p -> !p.isCreative() && !p.isSpectator() && p.isAlive());
        if (players.isEmpty()) return;

        long output = AuraNodeProfile.outputPerPlayerPerPulse(getTier());
        long totalWanted = output > Long.MAX_VALUE / players.size() ? Long.MAX_VALUE : output * players.size();
        long distributed = Math.min(storedEmc, totalWanted);
        long perPlayer = distributed / players.size();
        long remainder = distributed % players.size();
        for (int index = 0; index < players.size(); index++) {
            long amount = perPlayer + (index < remainder ? 1L : 0L);
            if (amount <= 0L) continue;
            ServerPlayer player = players.get(index);
            EmcAccountUtil.add(player, amount);
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
                4, 0.35D, 0.5D, 0.35D, 0.02D);
        }
        storedEmc -= distributed;
        setChanged();
        if (storedEmc <= 0L) beginRecharge(gameTime);
    }

    private void siphonNearbyPlayers(ServerLevel level) {
        int radius = AuraNodeProfile.siphonRadius(getTier());
        float damage = AuraNodeProfile.siphonDamagePerPulse(getTier());
        if (damage <= 0.0F) return;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(radius),
            p -> !p.isCreative() && !p.isSpectator() && p.isAlive())) {
            player.hurt(level.damageSources().magic(), damage);
            AdvancementHooks.onSiphonPulse(player, level.getGameTime(), AuraNodeProfile.pulseIntervalTicks());
            level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(),
                6, 0.25D, 0.5D, 0.25D, 0.01D);
        }
    }

    private void beginRecharge(long gameTime) {
        storedEmc = 0L;
        recharging = true;
        rechargeEndGameTime = gameTime + AuraNodeProfile.rechargeTicks(getTier());
        updateActiveState(false);
        setChanged();
    }

    private void updateActiveState(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(AuraNodeBlock.ACTIVE) && state.getValue(AuraNodeBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(AuraNodeBlock.ACTIVE, active), 3);
        }
    }

    public int getTier() { return getBlockState().getValue(AuraNodeBlock.TIER); }
    public long getCapacity() { return AuraNodeProfile.reserve(getTier()); }
    public long getStoredEmc() { return storedEmc; }
    public boolean isActivated() { return activated; }
    public boolean isRecharging() { return recharging; }
    public long getRechargeEndGameTime() { return rechargeEndGameTime; }

    public Component statusLine() {
        if (!activated) return Component.literal("§8Resonance Spire " + roman(getTier())
            + " — Locked until World Tier " + roman(getTier()) + ".");
        if (recharging) {
            long remaining = level == null ? 0L : Math.max(0L, rechargeEndGameTime - level.getGameTime());
            return Component.literal("§cRecharging §7— " + (remaining / 20L) + "s remaining. Vital Siphon active.");
        }
        return Component.literal("§aActive §7— " + storedEmc + " / " + getCapacity() + " EMC, "
            + AuraNodeProfile.outputPerPlayer(getTier()) + " EMC/s per player. Activated automatically by World Tier.");
    }

    private static String roman(int tier) {
        return switch (AuraNodeProfile.clampTier(tier)) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "I"; };
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("Activated");
        recharging = tag.getBoolean("Recharging");
        storedEmc = Math.max(0L, tag.getLong("StoredEmc"));
        rechargeEndGameTime = Math.max(0L, tag.getLong("RechargeEnd"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Activated", activated);
        tag.putBoolean("Recharging", recharging);
        tag.putLong("StoredEmc", storedEmc);
        tag.putLong("RechargeEnd", rechargeEndGameTime);
    }
}
