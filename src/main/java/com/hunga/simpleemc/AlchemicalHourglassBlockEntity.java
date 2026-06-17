package com.hunga.simpleemc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class AlchemicalHourglassBlockEntity extends BlockEntity {
    private long emc = 0L;
    public static final long MAX_EMC = Long.MAX_VALUE;

    // Reflection cache for furnace fields
    private static Field furnaceCookingProgressField;
    private static Field furnaceCookingTotalTimeField;
    private static Field furnaceLitTimeField;
    
    // Reflection cache for brewing stand fields
    private static Field brewingStandBrewTimeField;

    // Non-stacking mechanism: Keep track of positions accelerated during the current game tick
    private static long lastTickTime = -1L;
    private static final Set<String> ACCELERATED_POSITIONS = new HashSet<>();

    static {
        try {
            furnaceCookingProgressField = AbstractFurnaceBlockEntity.class.getDeclaredField("cookingProgress");
            furnaceCookingProgressField.setAccessible(true);
            
            furnaceCookingTotalTimeField = AbstractFurnaceBlockEntity.class.getDeclaredField("cookingTotalTime");
            furnaceCookingTotalTimeField.setAccessible(true);
            
            furnaceLitTimeField = AbstractFurnaceBlockEntity.class.getDeclaredField("litTime");
            furnaceLitTimeField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[SimpleEMC] Failed to cache furnace fields for hourglass: " + e.getMessage());
        }

        try {
            brewingStandBrewTimeField = BrewingStandBlockEntity.class.getDeclaredField("brewTime");
            brewingStandBrewTimeField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[SimpleEMC] Failed to cache brewing stand fields for hourglass: " + e.getMessage());
        }
    }

    public AlchemicalHourglassBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleEMC.ALCHEMICAL_HOURGLASS_BE.get(), pos, state);
    }

    public long getEMC() {
        return emc;
    }

    public void setEMC(long emc) {
        this.emc = emc;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.emc = tag.getLong("emc");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("emc", this.emc);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlchemicalHourglassBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        // Auto-recharging mechanism: if EMC is low, scan for players within a 4-block radius
        if (blockEntity.emc < 2) {
            Player nearestPlayer = null;
            double nearestDistSq = Double.MAX_VALUE;
            
            for (Player p : level.players()) {
                double dSq = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (dSq <= 25.0) { // 5 blocks radius (5^2 = 25) to easily cover the 4-block zone
                    if (dSq < nearestDistSq) {
                        nearestDistSq = dSq;
                        nearestPlayer = p;
                    }
                }
            }

            if (nearestPlayer != null) {
                long added = EMCRegistry.consumeFuelFromInventory(nearestPlayer);
                if (added > 0) {
                    blockEntity.emc += added;
                    blockEntity.setChanged();
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
                    nearestPlayer.displayClientMessage(Component.literal("Hourglass auto-refilled! (+§e" + added + " EMC§f, Total: §e" + blockEntity.emc + " EMC§f)"), true);
                }
            }
        }

        boolean isActive = blockEntity.emc > 0;
        if (state.hasProperty(AlchemicalHourglassBlock.ACTIVE) && state.getValue(AlchemicalHourglassBlock.ACTIVE) != isActive) {
            level.setBlock(pos, state.setValue(AlchemicalHourglassBlock.ACTIVE, isActive), 3);
        }

        if (blockEntity.emc > 0) {
            // Consumes 2 EMC per game tick
            blockEntity.emc = Math.max(0L, blockEntity.emc - 2L);
            blockEntity.setChanged();

            // Clear the tracking set at the start of a new game tick
            long gameTime = level.getGameTime();
            if (gameTime != lastTickTime) {
                lastTickTime = gameTime;
                ACCELERATED_POSITIONS.clear();
            }

            // Loop in a 4-block radius (9x9x9 cube centered on hourglass)
            int radius = 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Skip the hourglass itself
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos adjPos = pos.offset(dx, dy, dz);
                        
                        // Check if this position has already been accelerated in this tick
                        String posKey = level.dimension().location().toString() + "@" + adjPos.asLong();
                        if (ACCELERATED_POSITIONS.contains(posKey)) {
                            continue;
                        }

                        BlockState adjState = level.getBlockState(adjPos);
                        if (adjState.isAir()) {
                            continue;
                        }

                        boolean handled = false;

                        // 1. Accelerate Block Entities
                        BlockEntity adjBE = level.getBlockEntity(adjPos);
                        if (adjBE != null) {
                            // Furnaces (Furnace, Smoker, Blast Furnace)
                            if (adjBE instanceof AbstractFurnaceBlockEntity furnace) {
                                handled = true;
                                try {
                                    if (furnaceLitTimeField != null && furnaceCookingProgressField != null && furnaceCookingTotalTimeField != null) {
                                        int litTime = furnaceLitTimeField.getInt(furnace);
                                        if (litTime > 0) {
                                            int progress = furnaceCookingProgressField.getInt(furnace);
                                            int total = furnaceCookingTotalTimeField.getInt(furnace);
                                            if (progress > 0 && progress < total) {
                                                // Speed up cooking by adding 3 ticks (total 4x speed)
                                                furnaceCookingProgressField.setInt(furnace, Math.min(total - 1, progress + 3));
                                                furnace.setChanged();

                                                // Spawn clock particles above the furnace to indicate acceleration
                                                if (level instanceof ServerLevel serverLevel && level.random.nextFloat() < 0.2F) {
                                                    serverLevel.sendParticles(
                                                        new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new ItemStack(Items.CLOCK)),
                                                        adjPos.getX() + 0.5, adjPos.getY() + 1.1, adjPos.getZ() + 0.5, 
                                                        1, 0.0, 0.0, 0.0, 0.0
                                                    );
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            }
                            // Brewing Stands
                            else if (adjBE instanceof BrewingStandBlockEntity brewingStand) {
                                handled = true;
                                try {
                                    if (brewingStandBrewTimeField != null) {
                                        int brewTime = brewingStandBrewTimeField.getInt(brewingStand);
                                        if (brewTime > 0) {
                                            // Speed up brewing by reducing brew time by 3 ticks (total 4x speed)
                                            brewingStandBrewTimeField.setInt(brewingStand, Math.max(0, brewTime - 3));
                                            brewingStand.setChanged();

                                            // Spawn clock particles above the brewing stand to indicate acceleration
                                            if (level instanceof ServerLevel serverLevel && level.random.nextFloat() < 0.2F) {
                                                serverLevel.sendParticles(
                                                    new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new ItemStack(Items.CLOCK)),
                                                    adjPos.getX() + 0.5, adjPos.getY() + 1.1, adjPos.getZ() + 0.5, 
                                                    1, 0.0, 0.0, 0.0, 0.0
                                                );
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            }
                            // Mob Spawners
                            else if (adjBE instanceof SpawnerBlockEntity spawner) {
                                handled = true;
                                try {
                                    // Run the spawner tick logic 3 extra times
                                    for (int i = 0; i < 3; i++) {
                                        spawner.getSpawner().serverTick((ServerLevel) level, adjPos);
                                    }

                                    // Spawn clock particles above the spawner to indicate acceleration
                                    if (level instanceof ServerLevel serverLevel && level.random.nextFloat() < 0.2F) {
                                        serverLevel.sendParticles(
                                            new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new ItemStack(Items.CLOCK)),
                                            adjPos.getX() + 0.5, adjPos.getY() + 1.1, adjPos.getZ() + 0.5, 
                                            1, 0.0, 0.0, 0.0, 0.0
                                        );
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            }
                        }

                        // 2. Accelerate Random-Ticking Blocks (Crops, Saplings, Copper Oxidation, Cocoa, Leaf Decay etc.)
                        if (!handled && adjState.isRandomlyTicking()) {
                            handled = true;
                            // 15% chance per game tick to trigger a random tick
                            if (level.random.nextFloat() < 0.15F) {
                                adjState.randomTick((ServerLevel) level, adjPos, level.random);
                            }
                        }

                        // If it is an acceleratable block/entity, mark it as handled to prevent other hourglasses from stacking on it
                        if (handled) {
                            ACCELERATED_POSITIONS.add(posKey);
                        }
                    }
                }
            }
        }
    }
}
