package com.hunga.simpleemc;

import com.hunga.simpleemc.network.ClientPayloadHandler;
import com.hunga.simpleemc.network.RequestWithdrawPayload;
import com.hunga.simpleemc.network.ServerPayloadHandler;
import com.hunga.simpleemc.network.SyncPlayerEMCPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(SimpleEMC.MODID)
public class SimpleEMC {
    public static final String MODID = "simpleemc";

    // Registries
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);

    // Items - dùng register() thay vì registerSimpleItem() vì cần truyền supplier lambda
    public static final Supplier<Item> PHILOSOPHERS_STONE = ITEMS.register("philosophers_stone",
        () -> new PhilosophersStoneItem(new Item.Properties()));
    public static final Supplier<Item> TRANSMUTATION_TABLET = ITEMS.register("transmutation_tablet",
        () -> new TransmutationTabletItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> TOME_OF_KNOWLEDGE = ITEMS.register("tome_of_knowledge",
        () -> new TomeOfKnowledgeItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> KNOWLEDGE_SHARING_BOOK = ITEMS.register("knowledge_sharing_book",
        () -> new KnowledgeSharingBookItem(new Item.Properties().stacksTo(1)));
    
    // Alchemical Utility Items
    public static final Supplier<Item> ATTRACTION_CATALYST = ITEMS.register("attraction_catalyst",
        () -> new AttractionCatalystItem(new Item.Properties()));
    public static final Supplier<Item> CHALICE_OF_TRANSMUTATION = ITEMS.register("chalice_of_transmutation",
        () -> new ChaliceOfTransmutationItem(new Item.Properties()));



    // Alchemical Fuels
    public static final Supplier<Item> ALCHEMICAL_COAL = ITEMS.register("alchemical_coal",
        () -> new AlchemicalFuelItem(new Item.Properties(), 12800));
    public static final Supplier<Item> MOBIUS_FUEL = ITEMS.register("mobius_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 102400));
    public static final Supplier<Item> AETERNALIS_FUEL = ITEMS.register("aeternalis_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 819200));

    // Blocks - dùng BLOCKS.register() cho custom block class
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

    // Block Items - dùng ITEMS.register() cho custom block item
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
        () -> new MenuType<>(TransmutationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<ArcaneTransmutationMenu>> ARCANE_TRANSMUTATION_MENU_TYPE = MENUS.register("arcane_transmutation_menu",
        () -> new MenuType<>(ArcaneTransmutationMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // Data Attachments
    public static final Supplier<AttachmentType<PlayerEMC>> PLAYER_EMC = ATTACHMENT_TYPES.register("player_emc",
        () -> AttachmentType.builder(PlayerEMC::new)
            .serialize(PlayerEMC.CODEC)
            .copyOnDeath()
            .build());

    public SimpleEMC(IEventBus modEventBus, ModContainer container) {
        // Register the Deferred Registers to the mod event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        // Register mod bus listeners
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::buildContents);
        
        // Register Config screen factory for Client mods list button
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mc, parent) -> new EMCConfigScreen(parent));
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);

        // Client-bound custom EMC sync packet (Server -> Client)
        registrar.playToClient(
            com.hunga.simpleemc.network.SyncCustomEMCPayload.TYPE,
            com.hunga.simpleemc.network.SyncCustomEMCPayload.STREAM_CODEC,
            ClientPayloadHandler::handleCustomEMCSync
        );

        // Client-bound open config UI packet (Server -> Client)
        registrar.playToClient(
            com.hunga.simpleemc.network.OpenConfigScreenPayload.TYPE,
            com.hunga.simpleemc.network.OpenConfigScreenPayload.STREAM_CODEC,
            ClientPayloadHandler::handleOpenConfigScreen
        );

        // Server-bound custom EMC edit packet (Client -> Server)
        registrar.playToServer(
            com.hunga.simpleemc.network.RequestUpdateEMCPayload.TYPE,
            com.hunga.simpleemc.network.RequestUpdateEMCPayload.STREAM_CODEC,
            ServerPayloadHandler::handleRequestUpdateEMC
        );

        // Client-bound sync packet (Server -> Client)
        registrar.playToClient(
            SyncPlayerEMCPayload.TYPE,
            SyncPlayerEMCPayload.STREAM_CODEC,
            ClientPayloadHandler::handle
        );

        // Server-bound withdraw action packet (Client -> Server)
        registrar.playToServer(
            RequestWithdrawPayload.TYPE,
            RequestWithdrawPayload.STREAM_CODEC,
            ServerPayloadHandler::handle
        );

        // Server-bound JEI recipe transfer fill packet (Client -> Server)
        registrar.playToServer(
            com.hunga.simpleemc.network.FillCraftingFromEMCPayload.TYPE,
            com.hunga.simpleemc.network.FillCraftingFromEMCPayload.STREAM_CODEC,
            ServerPayloadHandler::handleFillCrafting
        );
    }

    private void buildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PHILOSOPHERS_STONE.get());
            event.accept(TRANSMUTATION_TABLET.get());
            event.accept(TOME_OF_KNOWLEDGE.get());
            event.accept(KNOWLEDGE_SHARING_BOOK.get());
            event.accept(ALCHEMICAL_COAL.get());
            event.accept(MOBIUS_FUEL.get());
            event.accept(AETERNALIS_FUEL.get());
            event.accept(ATTRACTION_CATALYST.get());
            event.accept(CHALICE_OF_TRANSMUTATION.get());
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
        PlayerEMC data = player.getData(PLAYER_EMC.get());
        PacketDistributor.sendToPlayer(player, new SyncPlayerEMCPayload(data.getEmc(), data.getLearnedItemsRL()));
    }

    public static void syncCustomEMCOverrides(ServerPlayer player) {
        java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
        PacketDistributor.sendToPlayer(player, new com.hunga.simpleemc.network.SyncCustomEMCPayload(fullOverrides));
    }

    // =======================================================================
    // Game Event Subscribers (NeoForge.EVENT_BUS)
    // @EventBusSubscriber auto-registers this class to the correct bus
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
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
        public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
            var server = event.getPlayerList().getServer();
            EMCRegistry.reloadAndRecalculate(server.getRecipeManager(), server.registryAccess());
            
            // Sync to all players on reload
            java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(player, new com.hunga.simpleemc.network.SyncCustomEMCPayload(fullOverrides));
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
            var dispatcher = event.getDispatcher();
            dispatcher.register(net.minecraft.commands.Commands.literal("simpleemc")
                .then(net.minecraft.commands.Commands.literal("dump")
                    .executes(context -> {
                        try {
                            java.io.File file = new java.io.File("simpleemc_missing_emc.txt");
                            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file));
                            int count = 0;
                            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                                if (!EMCRegistry.hasEMC(item)) {
                                    writer.println(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
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
                        PacketDistributor.sendToPlayer(player, new com.hunga.simpleemc.network.OpenConfigScreenPayload());
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
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(SimpleEMC.TRANSMUTATION_MENU_TYPE.get(), TransmutationScreen::new);
            event.register(SimpleEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get(), ArcaneTransmutationScreen::new);
        }

    }

    // =======================================================================
    // Client-only Game Event Subscribers (Game Bus, CLIENT side only)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
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
        public static void onRecipesUpdated(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            // Use level's registryAccess if in-world, or connection's registryAccess as fallback.
            // RecipesUpdatedEvent may fire before mc.level is set during the join sequence.
            net.minecraft.core.HolderLookup.Provider registries = null;
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

        /**
         * LevelEvent.Load fires on the client after the client world is fully loaded.
         * mc.level and mc.getConnection() are both guaranteed non-null at this point,
         * making this the most reliable place to trigger recipe EMC calculation + cache save.
         */
        @SubscribeEvent
        public static void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
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
