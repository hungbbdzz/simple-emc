package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MatterburstTntBlock extends TntBlock {
    public static final IntegerProperty CORE_LEVEL = IntegerProperty.create("core_level", 1, 4);

    public MatterburstTntBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CORE_LEVEL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE, CORE_LEVEL);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face,
                             @Nullable LivingEntity igniter) {
        prime(level, pos, state.getValue(CORE_LEVEL), igniter, 80);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) return;
        BlockState state = level.getBlockState(pos);
        int core = state.hasProperty(CORE_LEVEL) ? state.getValue(CORE_LEVEL) : 1;
        prime(level, pos, core, explosion.getIndirectSourceEntity(), level.random.nextInt(20) + 10);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack drop = new ItemStack(VeloriseEMC.MATTERBURST_TNT_ITEM.get());
        CoreTierData.set(drop, com.velorise.emc.common.WirelessCoreTier.byLevel(state.getValue(CORE_LEVEL)));
        return List.of(drop);
    }

    private static void prime(Level level, BlockPos pos, int core, @Nullable LivingEntity owner, int fuse) {
        if (level.isClientSide) return;
        PrimedTnt primed = new PrimedTnt(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, owner);
        primed.setFuse(fuse);
        primed.getPersistentData().putInt(MatterburstExplosionManager.ENTITY_CORE_TAG, Math.max(1, Math.min(4, core)));
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(),
            SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static BlockState stateForCore(int core) {
        return VeloriseEMC.MATTERBURST_TNT.get().defaultBlockState()
            .setValue(CORE_LEVEL, Math.max(1, Math.min(4, core)));
    }
}
