package com.velorise.simpleemc;

import com.velorise.simpleemc.capability.PlayerEMCCapability;
import com.velorise.simpleemc.capability.PlayerEMCCapabilityProvider;
import com.velorise.simpleemc.network.ModMessages;
import com.velorise.simpleemc.network.SyncCustomEMCMessage;
import com.velorise.simpleemc.network.SyncPlayerEMCMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

@Mod(SimpleEMC.MODID)
public class SimpleEMC {
    public static final String MODID = "simpleemc";

    // Registries
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    // Custom Sounds
    public static final Supplier<SoundEvent> MAGIC_MIRROR_USE_SOUND = SOUND_EVENTS.register("magic_mirror_use",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "magic_mirror_use")));

    // Items
    public static final Supplier<Item> PHILOSOPHERS_STONE = ITEMS.register("philosophers_stone",
        () -> new PhilosophersStoneItem(new Item.Properties()));
    public static final Supplier<Item> TRANSMUTATION_TABLET = ITEMS.register("transmutation_tablet",
        () -> new TransmutationTabletItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> TOME_OF_KNOWLEDGE = ITEMS.register("tome_of_knowledge",
        () -> new TomeOfKnowledgeItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> KNOWLEDGE_SHARING_BOOK = ITEMS.register("knowledge_sharing_book",
        () -> new KnowledgeSharingBookItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> WIRELESS_CORE = ITEMS.register("wireless_core",
        () -> new Item(new Item.Properties()));
    
    // Alchemical Utility Items
    public static final Supplier<Item> ATTRACTION_CATALYST = ITEMS.register("attraction_catalyst",
        () -> new AttractionCatalystItem(new Item.Properties()));
    public static final Supplier<Item> CHALICE_OF_TRANSMUTATION = ITEMS.register("chalice_of_transmutation",
        () -> new ChaliceOfTransmutationItem(new Item.Properties()));
    public static final Supplier<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
        () -> new MagicMirrorItem(new Item.Properties().stacksTo(1)));

    // Alchemical Fuels
    public static final Supplier<Item> ALCHEMICAL_COAL = ITEMS.register("alchemical_coal",
        () -> new AlchemicalFuelItem(new Item.Properties(), 12800));
    public static final Supplier<Item> MOBIUS_FUEL = ITEMS.register("mobius_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 102400));
    public static final Supplier<Item> AETERNALIS_FUEL = ITEMS.register("aeternalis_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 819200));

    // Blocks
    public static final Supplier<Block> TRANSMUTATION_TABLE = BLOCKS.register("transmutation_table",
        () -> new TransmutationTableBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final Supplier<Block> ARCANE_TRANSMUTATION_TABLE = BLOCKS.register("arcane_transmutation_table",
        () -> new ArcaneTransmutationTableBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)));

    // Alchemical Fuel Blocks
    public static final Supplier<Block> ALCHEMICAL_COAL_BLOCK = BLOCKS.register("alchemical_coal_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final Supplier<Block> MOBIUS_FUEL_BLOCK = BLOCKS.register("mobius_fuel_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final Supplier<Block> AETERNALIS_FUEL_BLOCK = BLOCKS.register("aeternalis_fuel_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)));

    // Alchemical Hourglass Block
    public static final Supplier<Block> ALCHEMICAL_HOURGLASS = BLOCKS.register("alchemical_hourglass",
        () -> new AlchemicalHourglassBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(net.minecraft.world.level.block.SoundType.GLASS)
            .noOcclusion()));

    // Block Items
    public static final Supplier<Item> TRANSMUTATION_TABLE_ITEM = ITEMS.register("transmutation_table",
        () -> new BlockItem(TRANSMUTATION_TABLE.get(), new Item.Properties()));
    public static final Supplier<Item> ARCANE_TRANSMUTATION_TABLE_ITEM = ITEMS.register("arcane_transmutation_table",
        () -> new BlockItem(ARCANE_TRANSMUTATION_TABLE.get(), new Item.Properties()));

    // Alchemical Fuel Block Items
    public static final Supplier<Item> ALCHEMICAL_COAL_BLOCK_ITEM = ITEMS.register("alchemical_coal_block",
        () -> new AlchemicalFuelBlockItem(ALCHEMICAL_COAL_BLOCK.get(), new Item.Properties(), 128000));
    public static final Supplier<Item> MOBIUS_FUEL_BLOCK_ITEM = ITEMS.register("mobius_fuel_block",
        () -> new AlchemicalFuelBlockItem(MOBIUS_FUEL_BLOCK.get(), new Item.Properties(), 1024000));
    public static final Supplier<Item> AETERNALIS_FUEL_BLOCK_ITEM = ITEMS.register("aeternalis_fuel_block",
        () -> new AlchemicalFuelBlockItem(AETERNALIS_FUEL_BLOCK.get(), new Item.Properties(), 8192000));

    // Alchemical Hourglass Block Item
    public static final Supplier<Item> ALCHEMICAL_HOURGLASS_ITEM = ITEMS.register("alchemical_hourglass",
        () -> new BlockItem(ALCHEMICAL_HOURGLASS.get(), new Item.Properties()));

    // Block Entity Types
    public static final Supplier<BlockEntityType<AlchemicalHourglassBlockEntity>> ALCHEMICAL_HOURGLASS_BE = BLOCK_ENTITIES.register("alchemical_hourglass",
        () -> BlockEntityType.Builder.of(AlchemicalHourglassBlockEntity::new, ALCHEMICAL_HOURGLASS.get()).build(null));

    // Menu Types
    public static final Supplier<MenuType<TransmutationMenu>> TRANSMUTATION_MENU_TYPE = MENUS.register("transmutation_menu",
        () -> IForgeMenuType.create((windowId, inv, data) -> new TransmutationMenu(windowId, inv)));
    public static final Supplier<MenuType<ArcaneTransmutationMenu>> ARCANE_TRANSMUTATION_MENU_TYPE = MENUS.register("arcane_transmutation_menu",
        () -> IForgeMenuType.create((windowId, inv, data) -> new ArcaneTransmutationMenu(windowId, inv)));

    public SimpleEMC() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the Deferred Registers to the mod event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        // Register mod bus listeners
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::buildContents);

        // Register capabilities
        modEventBus.addListener(PlayerEMCCapability::register);
        
        // Register Config screen factory for Client mods list button
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new EMCConfigScreen(parent)));
        }

        // Register ourselves to the general Minecraft forge event bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
    }

    private void buildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PHILOSOPHERS_STONE.get());
            event.accept(TRANSMUTATION_TABLET.get());
            event.accept(TOME_OF_KNOWLEDGE.get());
            event.accept(KNOWLEDGE_SHARING_BOOK.get());
            event.accept(WIRELESS_CORE.get());
            event.accept(ALCHEMICAL_COAL.get());
            event.accept(MOBIUS_FUEL.get());
            event.accept(AETERNALIS_FUEL.get());
            event.accept(ATTRACTION_CATALYST.get());
            event.accept(CHALICE_OF_TRANSMUTATION.get());
            event.accept(MAGIC_MIRROR.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRANSMUTATION_TABLE_ITEM.get());
            event.accept(ARCANE_TRANSMUTATION_TABLE_ITEM.get());
            event.accept(ALCHEMICAL_COAL_BLOCK_ITEM.get());
            event.accept(MOBIUS_FUEL_BLOCK_ITEM.get());
            event.accept(AETERNALIS_FUEL_BLOCK_ITEM.get());
            event.accept(ALCHEMICAL_HOURGLASS_ITEM.get());
        }
    }

    // Helper to send data sync packet to a specific ServerPlayer
    public static void syncPlayerData(ServerPlayer player) {
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            ModMessages.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerEMCMessage(data.getEmc(), data.getLearnedItemsRL())
            );
        });
    }

    public static void syncCustomEMCOverrides(ServerPlayer player) {
        java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
        ModMessages.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncCustomEMCMessage(fullOverrides)
        );
    }

    // =======================================================================
    // Game Event Subscribers (MinecraftForge.EVENT_BUS)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(new ResourceLocation(MODID, "player_emc"), new PlayerEMCCapabilityProvider());
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
            event.getOriginal().invalidateCaps();
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
            }
        }

        @SubscribeEvent
        public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                syncCustomEMCOverrides(event.getPlayer());
            } else {
                var server = event.getPlayerList().getServer();
                EMCRegistry.reloadAndRecalculate(server.getRecipeManager(), server.registryAccess());
                
                // Sync to all players on reload
                java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ModMessages.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncCustomEMCMessage(fullOverrides)
                    );
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            var dispatcher = event.getDispatcher();
            dispatcher.register(net.minecraft.commands.Commands.literal("simpleemc")
                .then(net.minecraft.commands.Commands.literal("dump")
                    .executes(context -> {
                        try {
                            java.io.File file = new java.io.File("simpleemc_missing_emc.txt");
                            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file));
                            int count = 0;
                            for (var item : BuiltInRegistries.ITEM) {
                                if (!EMCRegistry.hasEMC(item)) {
                                    writer.println(BuiltInRegistries.ITEM.getKey(item).toString());
                                    count++;
                                }
                            }
                            writer.close();
                            final int finalCount = count;
                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Đã xuất " + finalCount + " items chưa có EMC ra file " + file.getAbsolutePath()), true);
                        } catch (Exception e) {
                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Lỗi khi xuất file: " + e.getMessage()));
                        }
                        return 1;
                    })
                )
                .then(net.minecraft.commands.Commands.literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ModMessages.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new com.velorise.simpleemc.network.OpenConfigScreenMessage()
                        );
                        return 1;
                    })
                )
            );
        }
    }

    // =======================================================================
    // Client-only Mod Event Subscribers (Mod Bus, CLIENT side only)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(SimpleEMC.TRANSMUTATION_MENU_TYPE.get(), TransmutationScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(SimpleEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get(), ArcaneTransmutationScreen::new);
            });
        }
    }

    // =======================================================================
    // Client-only Game Event Subscribers (Game Bus, CLIENT side only)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                
                // Add funny alchemical guide tooltips
                if (item == SimpleEMC.PHILOSOPHERS_STONE.get()) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.simpleemc.philosophers_stone.tooltip1"));
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.simpleemc.philosophers_stone.tooltip2"));
                } else if (item == SimpleEMC.TRANSMUTATION_TABLET.get()) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.simpleemc.transmutation_tablet.tooltip1"));
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.simpleemc.transmutation_tablet.tooltip2"));
                } else if (item == SimpleEMC.TRANSMUTATION_TABLE_ITEM.get()) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.transmutation_table.tooltip1"));
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.transmutation_table.tooltip2"));
                } else if (item == SimpleEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get()) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.arcane_transmutation_table.tooltip1"));
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.arcane_transmutation_table.tooltip2"));
                } else if (item == SimpleEMC.ALCHEMICAL_HOURGLASS_ITEM.get()) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.alchemical_hourglass.tooltip1"));
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("block.simpleemc.alchemical_hourglass.tooltip2"));
                }

                long emc = EMCRegistry.getEMC(item);
                if (emc > 0) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.literal("§eEMC: " + String.format("%,d", emc)));
                    if (stack.getCount() > 1) {
                        long totalEmc = emc * stack.getCount();
                        event.getToolTip().add(net.minecraft.network.chat.Component.literal("§eStack EMC: " + String.format("%,d", totalEmc)));
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRecipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.core.RegistryAccess registries = null;
            if (mc.level != null) {
                registries = mc.level.registryAccess();
            } else if (mc.getConnection() != null) {
                registries = mc.getConnection().registryAccess();
            }
            if (registries != null) {
                System.out.println("[SimpleEMC] RecipesUpdatedEvent – calculating recipe EMC...");
                EMCRegistry.calculateAllRecipeEMC(event.getRecipeManager(), registries);
                System.out.println("[SimpleEMC] Recipe EMC calculation done. Cache should be saved.");
            } else {
                System.out.println("[SimpleEMC] RecipesUpdatedEvent fired but no registries available – skipping.");
            }
        }

        @SubscribeEvent
        public static void onLevelLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
            if (!event.getLevel().isClientSide()) return;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.getConnection() != null) {
                System.out.println("[SimpleEMC] LevelEvent.Load (client) – ensuring recipe EMC is calculated...");
                EMCRegistry.calculateAllRecipeEMC(
                    mc.getConnection().getRecipeManager(),
                    mc.level.registryAccess()
                );
            }
        }
    }
}
