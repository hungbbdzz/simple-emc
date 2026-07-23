package com.velorise.emc;

import net.minecraft.core.component.DataComponents;

import com.velorise.emc.common.ResonanceJukeboxProfile;
import com.velorise.emc.network.JukeboxAudioPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Playlist-driven jukebox with a tiny local EMC operating cost. */
public class ResonanceJukeboxBlockEntity extends BaseContainerBlockEntity {
    public enum RepeatMode {
        NONE,
        ALL,
        ONE;

        public static RepeatMode byId(int id) {
            RepeatMode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(ResonanceJukeboxProfile.CONTAINER_SIZE, ItemStack.EMPTY);
    private long storedEmc;
    private int coreTier = 1;
    private int selectedSlot;
    private boolean playing;
    private RepeatMode repeatMode = RepeatMode.NONE;
    private boolean shuffle;
    private int elapsedTicks;
    private int chargeTicks;
    private int currentLengthTicks;
    private int volume = 70;
    private boolean audioStarted;
    private static int cachedAvailableDiscTypes = -1;

    public ResonanceJukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.RESONANCE_JUKEBOX_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonanceJukeboxBlockEntity jukebox) {
        if (level.isClientSide) return;
        long configuredMax = ResonanceJukeboxProfile.maxEmc();
        if (jukebox.storedEmc > configuredMax) {
            jukebox.storedEmc = configuredMax;
            jukebox.setChanged();
        }
        jukebox.chargeFromFuelSlot();
        if (level.getGameTime() % 20L == 0L) jukebox.checkFunnyAdvancements((ServerLevel) level);

        if (!jukebox.playing) {
            if (jukebox.audioStarted) jukebox.stopAudio();
            return;
        }

        ItemStack disc = jukebox.getSelectedDisc();
        if (!isPlayable(disc)) {
            if (!jukebox.selectFirstPlayable()) {
                jukebox.stopPlayback(false);
                return;
            }
            disc = jukebox.getSelectedDisc();
        }

        if (!jukebox.audioStarted) jukebox.startAudio(disc);
        jukebox.elapsedTicks++;
        jukebox.chargeTicks++;

        // Resolve the song boundary before buying another listening interval.
        if (jukebox.currentLengthTicks > 0 && jukebox.elapsedTicks >= jukebox.currentLengthTicks) {
            jukebox.onTrackFinished();
        }

        if (jukebox.playing && jukebox.chargeTicks >= ResonanceJukeboxProfile.costIntervalTicks()) {
            if (jukebox.storedEmc < ResonanceJukeboxProfile.emcCost()) {
                jukebox.stopPlayback(false);
                return;
            }
            jukebox.chargeTicks = 0;
            jukebox.storedEmc -= ResonanceJukeboxProfile.emcCost();
            jukebox.setChanged();
        }
    }

    private void chargeFromFuelSlot() {
        ItemStack fuel = items.get(ResonanceJukeboxProfile.FUEL_SLOT);
        long each = EMCRegistry.getAlchemicalFuelEMC(fuel);
        if (each <= 0L || storedEmc >= ResonanceJukeboxProfile.maxEmc()) return;
        long room = Math.max(0L, ResonanceJukeboxProfile.maxEmc() - storedEmc);
        long fits = room / each;
        if (fits <= 0L) return;
        int consume = (int) Math.min((long) fuel.getCount(), fits);
        storedEmc += each * consume;
        fuel.shrink(consume);
        if (fuel.isEmpty()) items.set(ResonanceJukeboxProfile.FUEL_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    public void togglePlayback() {
        if (playing) {
            stopPlayback(true);
            return;
        }
        if (storedEmc < ResonanceJukeboxProfile.emcCost()) return;
        if (!isPlayable(getSelectedDisc()) && !selectFirstPlayable()) return;
        // Charge the first listening interval up front so rapid play/stop cannot run for free.
        storedEmc -= ResonanceJukeboxProfile.emcCost();
        playing = true;
        elapsedTicks = 0;
        chargeTicks = 0;
        currentLengthTicks = getDiscLength(getSelectedDisc());
        audioStarted = false;
        setChanged();
    }

    public void stopPlayback(boolean keepSelection) {
        playing = false;
        elapsedTicks = 0;
        chargeTicks = 0;
        currentLengthTicks = 0;
        stopAudio();
        if (!keepSelection) selectedSlot = Math.max(0, Math.min(getAccessibleDiscSlots() - 1, selectedSlot));
        setChanged();
    }

    public void selectSlot(int slot) {
        int clamped = Math.max(0, Math.min(getAccessibleDiscSlots() - 1, slot));
        if (selectedSlot == clamped) return;
        selectedSlot = clamped;
        if (playing) restartSelected();
        setChanged();
        notifyComparator();
    }

    public void nextTrack() {
        int next = findNextPlayable(selectedSlot, 1, true);
        if (next >= 0) {
            selectedSlot = next;
            if (playing) restartSelected();
            setChanged();
            notifyComparator();
        }
    }

    public void previousTrack() {
        int previous = findNextPlayable(selectedSlot, -1, true);
        if (previous >= 0) {
            selectedSlot = previous;
            if (playing) restartSelected();
            setChanged();
            notifyComparator();
        }
    }

    public void cycleRepeatMode() {
        repeatMode = RepeatMode.byId(repeatMode.ordinal() + 1);
        setChanged();
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        setChanged();
    }

    public void changeVolume(int delta) {
        int next = ResonanceJukeboxProfile.cycleVolume(volume, delta);
        if (next == volume) return;
        volume = next;
        if (audioStarted) broadcastVolume();
        setChanged();
    }

    private void broadcastVolume() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        JukeboxAudioPayload payload = JukeboxAudioPayload.volume(worldPosition, volume / 100.0F);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private void onTrackFinished() {
        if (repeatMode == RepeatMode.ONE) {
            restartSelected();
            return;
        }
        int next = shuffle ? findRandomPlayable()
            : findNextPlayable(selectedSlot, 1, repeatMode == RepeatMode.ALL);
        if (next < 0) {
            stopPlayback(true);
            return;
        }
        selectedSlot = next;
        restartSelected();
        setChanged();
        notifyComparator();
    }

    private void restartSelected() {
        stopAudio();
        elapsedTicks = 0;
        currentLengthTicks = getDiscLength(getSelectedDisc());
        audioStarted = false;
        if (!isPlayable(getSelectedDisc())) {
            if (!selectFirstPlayable()) stopPlayback(true);
        }
    }

    private boolean selectFirstPlayable() {
        for (int i = 0; i < getAccessibleDiscSlots(); i++) {
            if (isPlayable(items.get(i))) {
                boolean changed = selectedSlot != i;
                selectedSlot = i;
                currentLengthTicks = getDiscLength(items.get(i));
                if (changed) {
                    setChanged();
                    notifyComparator();
                }
                return true;
            }
        }
        return false;
    }

    private int findNextPlayable(int from, int step, boolean wrap) {
        int size = getAccessibleDiscSlots();
        int index = from;
        for (int checked = 0; checked < size - 1; checked++) {
            index += step;
            if (index < 0 || index >= size) {
                if (!wrap) return -1;
                index = Math.floorMod(index, size);
            }
            if (isPlayable(items.get(index))) return index;
        }
        return isPlayable(items.get(from)) && wrap ? from : -1;
    }

    private int findRandomPlayable() {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < getAccessibleDiscSlots(); i++) {
            if (i != selectedSlot && isPlayable(items.get(i))) candidates.add(i);
        }
        if (candidates.isEmpty()) return repeatMode == RepeatMode.ALL && isPlayable(getSelectedDisc()) ? selectedSlot : -1;
        return candidates.get(level == null ? 0 : level.random.nextInt(candidates.size()));
    }

    private Optional<Holder<JukeboxSong>> getSong(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return JukeboxSong.fromStack(level.registryAccess(), stack);
    }

    private void startAudio(ItemStack disc) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Optional<Holder<JukeboxSong>> song = getSong(disc);
        if (song.isEmpty()) return;
        stopAudio();
        currentLengthTicks = Math.max(1, song.get().value().lengthInTicks());
        net.minecraft.resources.ResourceLocation soundId = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
            .getKey(song.get().value().soundEvent().value());
        if (soundId == null) return;
        JukeboxAudioPayload payload = JukeboxAudioPayload.play(worldPosition, soundId, volume / 100.0F);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
        audioStarted = true;
        setChanged();
    }

    private void stopAudio() {
        if (level instanceof ServerLevel serverLevel && audioStarted) {
            JukeboxAudioPayload payload = JukeboxAudioPayload.stop(worldPosition);
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
            }
        }
        audioStarted = false;
    }

    public static boolean isPlayable(ItemStack stack) {
        return !stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
    }

    private int getDiscLength(ItemStack stack) {
        return getSong(stack).map(holder -> Math.max(1, holder.value().lengthInTicks())).orElse(0);
    }

    public int getComparatorOutput() {
        return getSong(getSelectedDisc()).map(holder -> holder.value().comparatorOutput()).orElse(0);
    }

    public ItemStack getSelectedDisc() {
        return items.get(Math.max(0, Math.min(getAccessibleDiscSlots() - 1, selectedSlot)));
    }

    private void notifyComparator() {
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public ItemStack createDropStack() {
        ItemStack result = new ItemStack(VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get());
        CompoundTag tag = new CompoundTag();
        writeMachineData(tag);
        BlockItem.setBlockEntityData(result, VeloriseEMC.RESONANCE_JUKEBOX_BE.get(), tag);
        CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.byLevel(coreTier));
        if (hasCustomName()) result.set(DataComponents.CUSTOM_NAME, getCustomName());
        return result;
    }

    private void writeMachineData(CompoundTag tag) {
        tag.putLong("StoredEMC", storedEmc);
        tag.putInt("CoreTier", coreTier);
        tag.putInt("SelectedSlot", selectedSlot);
        tag.putInt("RepeatMode", repeatMode.ordinal());
        tag.putBoolean("Shuffle", shuffle);
        tag.putInt("Volume", volume);
    }

    private void readMachineData(CompoundTag tag) {
        storedEmc = Math.max(0L, Math.min(ResonanceJukeboxProfile.maxEmc(), tag.getLong("StoredEMC")));
        coreTier = Math.max(1, Math.min(4, tag.contains("CoreTier") ? tag.getInt("CoreTier") : 1));
        selectedSlot = Math.max(0, Math.min(getAccessibleDiscSlots() - 1, tag.getInt("SelectedSlot")));
        repeatMode = RepeatMode.byId(tag.getInt("RepeatMode"));
        shuffle = tag.getBoolean("Shuffle");
        volume = ResonanceJukeboxProfile.clampVolume(tag.contains("Volume") ? tag.getInt("Volume") : 70);
        playing = false;
        elapsedTicks = 0;
        chargeTicks = 0;
        currentLengthTicks = 0;
        audioStarted = false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(ResonanceJukeboxProfile.CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        readMachineData(tag);
        consolidateDiscStacks();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        writeMachineData(tag);
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("container.velorise_emc.resonance_jukebox").append(" " + getCoreTierLevel());
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ResonanceJukeboxMenu(id, inventory, this); }
    @Override public int getContainerSize() { return ResonanceJukeboxProfile.CONTAINER_SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == ResonanceJukeboxProfile.FUEL_SLOT) return EMCRegistry.getAlchemicalFuelEMC(stack) > 0L;
        return slot >= 0 && slot < getAccessibleDiscSlots() && isPlayable(stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot >= 0 && slot < ResonanceJukeboxProfile.DISC_SLOTS) consolidateDiscStacks();
        if (slot == selectedSlot) {
            if (playing) restartSelected();
            notifyComparator();
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = super.removeItem(slot, amount);
        if (slot == selectedSlot && !removed.isEmpty()) {
            if (playing) restartSelected();
            notifyComparator();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        if (slot == selectedSlot && !removed.isEmpty()) {
            if (playing) restartSelected();
            notifyComparator();
        }
        return removed;
    }

    private void consolidateDiscStacks() {
        boolean changed = false;
        int slots = getAccessibleDiscSlots();
        for (int firstSlot = 0; firstSlot < slots; firstSlot++) {
            ItemStack first = items.get(firstSlot);
            if (!isPlayable(first)) continue;
            for (int duplicateSlot = firstSlot + 1; duplicateSlot < slots && first.getCount() < 64; duplicateSlot++) {
                ItemStack duplicate = items.get(duplicateSlot);
                if (!isPlayable(duplicate) || !ItemStack.isSameItemSameComponents(first, duplicate)) continue;
                int moved = Math.min(64 - first.getCount(), duplicate.getCount());
                if (moved <= 0) continue;
                first.grow(moved);
                duplicate.shrink(moved);
                if (duplicate.isEmpty()) {
                    items.set(duplicateSlot, ItemStack.EMPTY);
                    if (selectedSlot == duplicateSlot) selectedSlot = firstSlot;
                }
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    public int getDistinctDiscTypeCount() {
        Set<net.minecraft.world.item.Item> types = new HashSet<>();
        for (int slot = 0; slot < getAccessibleDiscSlots(); slot++) {
            ItemStack stack = items.get(slot);
            if (isPlayable(stack)) types.add(stack.getItem());
        }
        return types.size();
    }

    public int getRequiredCollectionTypeCount() {
        if (cachedAvailableDiscTypes < 0) {
            cachedAvailableDiscTypes = (int) net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item.getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE))
                .count();
        }
        return Math.min(ResonanceJukeboxProfile.DISC_SLOTS, Math.max(0, cachedAvailableDiscTypes));
    }

    public void checkCollectionAdvancement(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || coreTier < 4) return;
        int required = getRequiredCollectionTypeCount();
        if (required > 0 && getDistinctDiscTypeCount() >= required) {
            SimpleAdvancements.award(player, "engineering/worlds_soundtrack");
        }
    }

    private void checkFunnyAdvancements(ServerLevel level) {
        if (!playing) return;
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
            new net.minecraft.world.phys.AABB(worldPosition).inflate(16.0D));
        if (nearby.isEmpty()) return;
        int slots = getAccessibleDiscSlots();
        ItemStack first = ItemStack.EMPTY;
        boolean allSame = slots > 1;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = items.get(i);
            if (!isPlayable(stack)) { allSame = false; break; }
            if (first.isEmpty()) first = stack;
            else if (!ItemStack.isSameItemSameComponents(first, stack)) { allSame = false; break; }
        }
        ItemStack selected = getSelectedDisc();
        boolean spooky = selected.is(net.minecraft.world.item.Items.MUSIC_DISC_11)
            || selected.is(net.minecraft.world.item.Items.MUSIC_DISC_13);
        long dayTime = level.getDayTime() % 24000L;
        int activeMachines = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-8,-4,-8), worldPosition.offset(8,4,8))) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(p);
            if (be instanceof AbstractResonantFurnaceBlockEntity furnace && furnace.isVisuallyLit()) activeMachines++;
            else if (be instanceof AlchemicalHourglassBlockEntity hourglass && hourglass.isActive()) activeMachines++;
            if (activeMachines >= 3) break;
        }
        for (ServerPlayer player : nearby) {
            if (allSame) SimpleAdvancements.award(player, "engineering/easter/on_repeat");
            if (spooky && dayTime >= 13000L && dayTime <= 23000L) SimpleAdvancements.award(player, "engineering/easter/do_not_turn_around");
            if (activeMachines >= 3) SimpleAdvancements.award(player, "engineering/easter/industrial_music");
        }
    }

    public long getStoredEmc() { return storedEmc; }
    public int getCoreTierLevel() { return coreTier; }
    public int getAccessibleDiscSlots() { return ResonanceJukeboxProfile.discSlots(coreTier); }
    public void setCoreTier(com.velorise.emc.common.WirelessCoreTier tier) {
        coreTier = tier == null ? 1 : tier.level();
        selectedSlot = Math.min(selectedSlot, getAccessibleDiscSlots() - 1);
        consolidateDiscStacks();
        setChanged();
    }
    public int getSelectedSlot() { return selectedSlot; }
    public boolean isPlaying() { return playing; }
    public RepeatMode getRepeatMode() { return repeatMode; }
    public boolean isShuffle() { return shuffle; }
    public int getElapsedTicks() { return elapsedTicks; }
    public int getCurrentLengthTicks() { return currentLengthTicks; }
    public int getVolume() { return volume; }
}
