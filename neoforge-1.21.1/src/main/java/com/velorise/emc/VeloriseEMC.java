package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
import com.velorise.emc.network.ClientPayloadHandler;
import com.velorise.emc.network.RequestWithdrawPayload;
import com.velorise.emc.network.ServerPayloadHandler;
import com.velorise.emc.network.SyncPlayerEMCPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.client.KeyMapping;
import java.util.function.Supplier;

@Mod(VeloriseEMC.MODID)
public class VeloriseEMC {
    public static final String MODID = "velorise_emc";

    // Registries
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    // Custom Sounds
    public static final Supplier<net.minecraft.sounds.SoundEvent> MAGIC_MIRROR_USE_SOUND = SOUND_EVENTS.register("magic_mirror_use",
        () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "magic_mirror_use")));

    // Items - dùng register() thay vì registerSimpleItem() vì cần truyền supplier lambda
    public static final Supplier<Item> PHILOSOPHERS_STONE = ITEMS.register("philosophers_stone",
        () -> new PhilosophersStoneItem(new Item.Properties()));
    public static final Supplier<Item> TRANSMUTATION_TABLET = ITEMS.register("transmutation_tablet",
        () -> new TransmutationTabletItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> TOME_OF_KNOWLEDGE = ITEMS.register("tome_of_knowledge",
        () -> new TomeOfKnowledgeItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> KNOWLEDGE_SHARING_BOOK = ITEMS.register("knowledge_sharing_book",
        () -> new KnowledgeSharingBookItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> WIRELESS_CORE = ITEMS.register("wireless_core_t1",
        () -> new WirelessCoreItem(new Item.Properties(), com.velorise.emc.common.WirelessCoreTier.I));
    public static final Supplier<Item> WIRELESS_CORE_T2 = ITEMS.register("wireless_core_t2",
        () -> new WirelessCoreItem(new Item.Properties().fireResistant(), com.velorise.emc.common.WirelessCoreTier.II));
    public static final Supplier<Item> WIRELESS_CORE_T3 = ITEMS.register("wireless_core_t3",
        () -> new WirelessCoreItem(new Item.Properties().fireResistant(), com.velorise.emc.common.WirelessCoreTier.III));
    public static final Supplier<Item> WIRELESS_CORE_T4 = ITEMS.register("wireless_core_t4",
        () -> new WirelessCoreItem(new Item.Properties().fireResistant(), com.velorise.emc.common.WirelessCoreTier.IV));
    public static final Supplier<Item> CONDENSED_CORE_I = ITEMS.register("condensed_core_i",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CONDENSED_CORE_II = ITEMS.register("condensed_core_ii",
        () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> CONDENSED_CORE_III = ITEMS.register("condensed_core_iii",
        () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> CONDENSED_CORE_IV = ITEMS.register("condensed_core_iv",
        () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> INFERNAL_ASCENSION_TEMPLATE = ITEMS.register("infernal_ascension_template",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> END_ASCENSION_TEMPLATE = ITEMS.register("end_ascension_template",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DEEP_CONVERGENCE_TEMPLATE = ITEMS.register("deep_convergence_template",
        () -> new Item(new Item.Properties()));

    // Alchemical Utility Items
    public static final Supplier<Item> ATTRACTION_CATALYST = ITEMS.register("attraction_catalyst",
        () -> new AttractionCatalystItem(new Item.Properties()));
    public static final Supplier<Item> CHALICE_OF_TRANSMUTATION = ITEMS.register("chalice_of_transmutation",
        () -> new ChaliceOfTransmutationItem(new Item.Properties()));
    public static final Supplier<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
        () -> new MagicMirrorItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> VOID_TRANSLOCATOR = ITEMS.register("void_translocator",
        () -> new VoidTranslocatorItem(new Item.Properties()));
    public static final Supplier<Item> CURSED_IGNITER = ITEMS.register("cursed_igniter",
        () -> new CursedIgniterItem(new Item.Properties()));
    public static final Supplier<Item> EYE_OF_THE_LAST_ECHO = ITEMS.register("eye_of_the_last_echo", () -> new EyeOfLastEchoItem(new Item.Properties()));
    public static final Supplier<Item> EYE_OF_THE_CRUCIBLE = ITEMS.register("eye_of_the_crucible", () -> new EyeOfTheCrucibleItem(new Item.Properties()));
    public static final Supplier<Item> AETHERIC_BULWARK_EYE = ITEMS.register("aetheric_bulwark_eye", () -> new PromotionBossEyeItem(new Item.Properties(), PromotionBossEyeItem.Kind.AETHERIC_BULWARK));
    public static final Supplier<Item> DEEPBOUND_EYE = ITEMS.register("deepbound_eye", () -> new PromotionBossEyeItem(new Item.Properties(), PromotionBossEyeItem.Kind.DEEPBOUND_WARDEN));
    public static final Supplier<Item> HEART_OF_THE_LAST_ECHO = ITEMS.register("heart_of_the_last_echo",
        () -> new Item(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final Supplier<Item> ECHOBOUND_DRAGON_SCALE = ITEMS.register("echobound_dragon_scale",
        () -> new Item(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final Supplier<Item> PURIFYING_MILK = ITEMS.register("purifying_milk",
        () -> new PurifyingMilkItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> DRAGONFIRE_SCEPTER = ITEMS.register("dragonfire_scepter",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.DRAGONFIRE));
    public static final Supplier<Item> RESONANT_ECHO = ITEMS.register("resonant_echo",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.ECHO));
    public static final Supplier<Item> INFERNO_ROD = ITEMS.register("inferno_rod",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.INFERNO));
    public static final Supplier<Item> GHASTFIRE_STAFF = ITEMS.register("ghastfire_staff",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.GHASTFIRE));
    public static final Supplier<Item> WITHER_SCEPTER = ITEMS.register("wither_scepter",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.WITHER));
    public static final Supplier<Item> SHULKER_SEEKER = ITEMS.register("shulker_seeker",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.SHULKER));
    public static final Supplier<Item> GUARDIAN_LENS = ITEMS.register("guardian_lens",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.GUARDIAN));
    public static final Supplier<Item> EVOKER_FANG_STAFF = ITEMS.register("evoker_fang_staff",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.EVOKER));
    public static final Supplier<Item> STORMCALLER = ITEMS.register("stormcaller",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.LIGHTNING));
    public static final Supplier<Item> FROSTBOLT_WAND = ITEMS.register("frostbolt_wand",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.FROST));
    public static final Supplier<Item> WEBCASTER = ITEMS.register("webcaster",
        () -> new ArcaneWeaponItem(new Item.Properties().stacksTo(1), ArcaneWeaponItem.Kind.WEB));


    // Alchemical Fuels
    public static final Supplier<Item> ALCHEMICAL_COAL = ITEMS.register("alchemical_coal",
        () -> new AlchemicalFuelItem(new Item.Properties(), 12800));
    public static final Supplier<Item> MOBIUS_FUEL = ITEMS.register("mobius_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 102400));
    public static final Supplier<Item> AETERNALIS_FUEL = ITEMS.register("aeternalis_fuel",
        () -> new AlchemicalFuelItem(new Item.Properties(), 819200));

    public static final Supplier<Item> RAW_VERDANT_RESONANT = ITEMS.register("raw_verdant_resonant",
        () -> new Item(resonantProperties(1)));
    public static final Supplier<Item> VERDANT_RESONANT_INGOT = ITEMS.register("verdant_resonant_ingot",
        () -> new Item(resonantProperties(1)));
    public static final Supplier<Item> RAW_INFERNAL_RESONANT = ITEMS.register("raw_infernal_resonant",
        () -> new Item(resonantProperties(2)));
    public static final Supplier<Item> INFERNAL_RESONANT_INGOT = ITEMS.register("infernal_resonant_ingot",
        () -> new Item(resonantProperties(2)));
    public static final Supplier<Item> RAW_AETHERIC_RESONANT = ITEMS.register("raw_aetheric_resonant",
        () -> new Item(resonantProperties(3)));
    public static final Supplier<Item> AETHERIC_RESONANT_INGOT = ITEMS.register("aetheric_resonant_ingot",
        () -> new Item(resonantProperties(3)));
    public static final Supplier<Item> RAW_ECHOING_RESONANT = ITEMS.register("raw_echoing_resonant",
        () -> new Item(resonantProperties(4)));
    public static final Supplier<Item> ECHOING_RESONANT_INGOT = ITEMS.register("echoing_resonant_ingot",
        () -> new Item(resonantProperties(4)));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VERDANT_ARMOR = ARMOR_MATERIALS.register("verdant_resonant",
        () -> ResonantMaterials.armor("verdant_resonant", 1, () -> VERDANT_RESONANT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INFERNAL_ARMOR = ARMOR_MATERIALS.register("infernal_resonant",
        () -> ResonantMaterials.armor("infernal_resonant", 2, () -> INFERNAL_RESONANT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> AETHERIC_ARMOR = ARMOR_MATERIALS.register("aetheric_resonant",
        () -> ResonantMaterials.armor("aetheric_resonant", 3, () -> AETHERIC_RESONANT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHOING_ARMOR = ARMOR_MATERIALS.register("echoing_resonant",
        () -> ResonantMaterials.armor("echoing_resonant", 4, () -> ECHOING_RESONANT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHOBOUND_ARMOR = ARMOR_MATERIALS.register("echobound",
        ResonantMaterials::echoboundArmor);

    public static final Supplier<Item> VERDANT_RESONANT_SWORD = ITEMS.register("verdant_resonant_sword",
        () -> new net.minecraft.world.item.SwordItem(ResonantMaterials.VERDANT_TOOLS, resonantProperties(1).attributes(net.minecraft.world.item.SwordItem.createAttributes(ResonantMaterials.VERDANT_TOOLS, 3.0F, -2.4F))));
    public static final Supplier<Item> VERDANT_RESONANT_PICKAXE = ITEMS.register("verdant_resonant_pickaxe",
        () -> new ResonantPickaxeItem(ResonantMaterials.VERDANT_TOOLS, resonantProperties(1).attributes(net.minecraft.world.item.PickaxeItem.createAttributes(ResonantMaterials.VERDANT_TOOLS, 1.0F, -2.8F))));
    public static final Supplier<Item> VERDANT_RESONANT_AXE = ITEMS.register("verdant_resonant_axe",
        () -> new net.minecraft.world.item.AxeItem(ResonantMaterials.VERDANT_TOOLS, resonantProperties(1).attributes(net.minecraft.world.item.AxeItem.createAttributes(ResonantMaterials.VERDANT_TOOLS, 5.0F, -3.0F))));
    public static final Supplier<Item> VERDANT_RESONANT_SHOVEL = ITEMS.register("verdant_resonant_shovel",
        () -> new net.minecraft.world.item.ShovelItem(ResonantMaterials.VERDANT_TOOLS, resonantProperties(1).attributes(net.minecraft.world.item.ShovelItem.createAttributes(ResonantMaterials.VERDANT_TOOLS, 1.5F, -3.0F))));
    public static final Supplier<Item> VERDANT_RESONANT_HOE = ITEMS.register("verdant_resonant_hoe",
        () -> new net.minecraft.world.item.HoeItem(ResonantMaterials.VERDANT_TOOLS, resonantProperties(1).attributes(net.minecraft.world.item.HoeItem.createAttributes(ResonantMaterials.VERDANT_TOOLS, -4.0F, 0.0F))));
    public static final Supplier<Item> VERDANT_RESONANT_HELMET = ITEMS.register("verdant_resonant_helmet",
        () -> new ArmorItem(VERDANT_ARMOR, ArmorItem.Type.HELMET, ResonantMaterials.armorProperties(1, ArmorItem.Type.HELMET)));
    public static final Supplier<Item> VERDANT_RESONANT_CHESTPLATE = ITEMS.register("verdant_resonant_chestplate",
        () -> new ArmorItem(VERDANT_ARMOR, ArmorItem.Type.CHESTPLATE, ResonantMaterials.armorProperties(1, ArmorItem.Type.CHESTPLATE)));
    public static final Supplier<Item> VERDANT_RESONANT_LEGGINGS = ITEMS.register("verdant_resonant_leggings",
        () -> new ArmorItem(VERDANT_ARMOR, ArmorItem.Type.LEGGINGS, ResonantMaterials.armorProperties(1, ArmorItem.Type.LEGGINGS)));
    public static final Supplier<Item> VERDANT_RESONANT_BOOTS = ITEMS.register("verdant_resonant_boots",
        () -> new ArmorItem(VERDANT_ARMOR, ArmorItem.Type.BOOTS, ResonantMaterials.armorProperties(1, ArmorItem.Type.BOOTS)));
    public static final Supplier<Item> INFERNAL_RESONANT_SWORD = ITEMS.register("infernal_resonant_sword",
        () -> new ResonantSwordItem(ResonantMaterials.INFERNAL_TOOLS, resonantProperties(2).attributes(net.minecraft.world.item.SwordItem.createAttributes(ResonantMaterials.INFERNAL_TOOLS, 3.0F, -2.4F)), 2));
    public static final Supplier<Item> INFERNAL_RESONANT_PICKAXE = ITEMS.register("infernal_resonant_pickaxe",
        () -> new ResonantPickaxeItem(ResonantMaterials.INFERNAL_TOOLS, resonantProperties(2).attributes(net.minecraft.world.item.PickaxeItem.createAttributes(ResonantMaterials.INFERNAL_TOOLS, 1.0F, -2.8F))));
    public static final Supplier<Item> INFERNAL_RESONANT_AXE = ITEMS.register("infernal_resonant_axe",
        () -> new net.minecraft.world.item.AxeItem(ResonantMaterials.INFERNAL_TOOLS, resonantProperties(2).attributes(net.minecraft.world.item.AxeItem.createAttributes(ResonantMaterials.INFERNAL_TOOLS, 5.0F, -3.0F))));
    public static final Supplier<Item> INFERNAL_RESONANT_SHOVEL = ITEMS.register("infernal_resonant_shovel",
        () -> new net.minecraft.world.item.ShovelItem(ResonantMaterials.INFERNAL_TOOLS, resonantProperties(2).attributes(net.minecraft.world.item.ShovelItem.createAttributes(ResonantMaterials.INFERNAL_TOOLS, 1.5F, -3.0F))));
    public static final Supplier<Item> INFERNAL_RESONANT_HOE = ITEMS.register("infernal_resonant_hoe",
        () -> new net.minecraft.world.item.HoeItem(ResonantMaterials.INFERNAL_TOOLS, resonantProperties(2).attributes(net.minecraft.world.item.HoeItem.createAttributes(ResonantMaterials.INFERNAL_TOOLS, -4.0F, 0.0F))));
    public static final Supplier<Item> INFERNAL_RESONANT_HELMET = ITEMS.register("infernal_resonant_helmet",
        () -> new ArmorItem(INFERNAL_ARMOR, ArmorItem.Type.HELMET, ResonantMaterials.armorProperties(2, ArmorItem.Type.HELMET)));
    public static final Supplier<Item> INFERNAL_RESONANT_CHESTPLATE = ITEMS.register("infernal_resonant_chestplate",
        () -> new ArmorItem(INFERNAL_ARMOR, ArmorItem.Type.CHESTPLATE, ResonantMaterials.armorProperties(2, ArmorItem.Type.CHESTPLATE)));
    public static final Supplier<Item> INFERNAL_RESONANT_LEGGINGS = ITEMS.register("infernal_resonant_leggings",
        () -> new ArmorItem(INFERNAL_ARMOR, ArmorItem.Type.LEGGINGS, ResonantMaterials.armorProperties(2, ArmorItem.Type.LEGGINGS)));
    public static final Supplier<Item> INFERNAL_RESONANT_BOOTS = ITEMS.register("infernal_resonant_boots",
        () -> new ArmorItem(INFERNAL_ARMOR, ArmorItem.Type.BOOTS, ResonantMaterials.armorProperties(2, ArmorItem.Type.BOOTS)));
    public static final Supplier<Item> AETHERIC_RESONANT_SWORD = ITEMS.register("aetheric_resonant_sword",
        () -> new ResonantSwordItem(ResonantMaterials.AETHERIC_TOOLS, resonantProperties(3).attributes(net.minecraft.world.item.SwordItem.createAttributes(ResonantMaterials.AETHERIC_TOOLS, 3.0F, -2.4F)), 3));
    public static final Supplier<Item> AETHERIC_RESONANT_PICKAXE = ITEMS.register("aetheric_resonant_pickaxe",
        () -> new ResonantPickaxeItem(ResonantMaterials.AETHERIC_TOOLS, resonantProperties(3).attributes(net.minecraft.world.item.PickaxeItem.createAttributes(ResonantMaterials.AETHERIC_TOOLS, 3.0F, -2.8F))));
    public static final Supplier<Item> AETHERIC_RESONANT_AXE = ITEMS.register("aetheric_resonant_axe",
        () -> new net.minecraft.world.item.AxeItem(ResonantMaterials.AETHERIC_TOOLS, resonantProperties(3).attributes(net.minecraft.world.item.AxeItem.createAttributes(ResonantMaterials.AETHERIC_TOOLS, 3.0F, -3.0F))));
    public static final Supplier<Item> AETHERIC_RESONANT_SHOVEL = ITEMS.register("aetheric_resonant_shovel",
        () -> new net.minecraft.world.item.ShovelItem(ResonantMaterials.AETHERIC_TOOLS, resonantProperties(3).attributes(net.minecraft.world.item.ShovelItem.createAttributes(ResonantMaterials.AETHERIC_TOOLS, 3.0F, -3.0F))));
    public static final Supplier<Item> AETHERIC_RESONANT_HOE = ITEMS.register("aetheric_resonant_hoe",
        () -> new net.minecraft.world.item.HoeItem(ResonantMaterials.AETHERIC_TOOLS, resonantProperties(3).attributes(net.minecraft.world.item.HoeItem.createAttributes(ResonantMaterials.AETHERIC_TOOLS, 3.0F, 0.0F))));
    public static final Supplier<Item> AETHERIC_RESONANT_HELMET = ITEMS.register("aetheric_resonant_helmet",
        () -> new ArmorItem(AETHERIC_ARMOR, ArmorItem.Type.HELMET, ResonantMaterials.armorProperties(3, ArmorItem.Type.HELMET)));
    public static final Supplier<Item> AETHERIC_RESONANT_CHESTPLATE = ITEMS.register("aetheric_resonant_chestplate",
        () -> new ArmorItem(AETHERIC_ARMOR, ArmorItem.Type.CHESTPLATE, ResonantMaterials.armorProperties(3, ArmorItem.Type.CHESTPLATE)));
    public static final Supplier<Item> AETHERIC_RESONANT_LEGGINGS = ITEMS.register("aetheric_resonant_leggings",
        () -> new ArmorItem(AETHERIC_ARMOR, ArmorItem.Type.LEGGINGS, ResonantMaterials.armorProperties(3, ArmorItem.Type.LEGGINGS)));
    public static final Supplier<Item> AETHERIC_RESONANT_BOOTS = ITEMS.register("aetheric_resonant_boots",
        () -> new ArmorItem(AETHERIC_ARMOR, ArmorItem.Type.BOOTS, ResonantMaterials.armorProperties(3, ArmorItem.Type.BOOTS)));
    public static final Supplier<Item> ECHOING_RESONANT_SWORD = ITEMS.register("echoing_resonant_sword",
        () -> new ResonantSwordItem(ResonantMaterials.ECHOING_TOOLS, resonantProperties(4).attributes(net.minecraft.world.item.SwordItem.createAttributes(ResonantMaterials.ECHOING_TOOLS, 3.0F, -2.4F)), 4));
    public static final Supplier<Item> ECHOING_RESONANT_PICKAXE = ITEMS.register("echoing_resonant_pickaxe",
        () -> new ResonantPickaxeItem(ResonantMaterials.ECHOING_TOOLS, resonantProperties(4).attributes(net.minecraft.world.item.PickaxeItem.createAttributes(ResonantMaterials.ECHOING_TOOLS, 3.0F, -2.8F))));
    public static final Supplier<Item> ECHOING_RESONANT_AXE = ITEMS.register("echoing_resonant_axe",
        () -> new net.minecraft.world.item.AxeItem(ResonantMaterials.ECHOING_TOOLS, resonantProperties(4).attributes(net.minecraft.world.item.AxeItem.createAttributes(ResonantMaterials.ECHOING_TOOLS, 3.0F, -3.0F))));
    public static final Supplier<Item> ECHOING_RESONANT_SHOVEL = ITEMS.register("echoing_resonant_shovel",
        () -> new net.minecraft.world.item.ShovelItem(ResonantMaterials.ECHOING_TOOLS, resonantProperties(4).attributes(net.minecraft.world.item.ShovelItem.createAttributes(ResonantMaterials.ECHOING_TOOLS, 3.0F, -3.0F))));
    public static final Supplier<Item> ECHOING_RESONANT_HOE = ITEMS.register("echoing_resonant_hoe",
        () -> new net.minecraft.world.item.HoeItem(ResonantMaterials.ECHOING_TOOLS, resonantProperties(4).attributes(net.minecraft.world.item.HoeItem.createAttributes(ResonantMaterials.ECHOING_TOOLS, 3.0F, 0.0F))));
    public static final Supplier<Item> ECHOING_RESONANT_HELMET = ITEMS.register("echoing_resonant_helmet",
        () -> new ArmorItem(ECHOING_ARMOR, ArmorItem.Type.HELMET, ResonantMaterials.armorProperties(4, ArmorItem.Type.HELMET)));
    public static final Supplier<Item> ECHOING_RESONANT_CHESTPLATE = ITEMS.register("echoing_resonant_chestplate",
        () -> new ArmorItem(ECHOING_ARMOR, ArmorItem.Type.CHESTPLATE, ResonantMaterials.armorProperties(4, ArmorItem.Type.CHESTPLATE)));
    public static final Supplier<Item> ECHOING_RESONANT_LEGGINGS = ITEMS.register("echoing_resonant_leggings",
        () -> new ArmorItem(ECHOING_ARMOR, ArmorItem.Type.LEGGINGS, ResonantMaterials.armorProperties(4, ArmorItem.Type.LEGGINGS)));
    public static final Supplier<Item> ECHOING_RESONANT_BOOTS = ITEMS.register("echoing_resonant_boots",
        () -> new ArmorItem(ECHOING_ARMOR, ArmorItem.Type.BOOTS, ResonantMaterials.armorProperties(4, ArmorItem.Type.BOOTS)));
    public static final Supplier<Item> ECHOBOUND_HELMET = ITEMS.register("echobound_helmet",
        () -> new ArmorItem(ECHOBOUND_ARMOR, ArmorItem.Type.HELMET, ResonantMaterials.echoboundArmorProperties(ArmorItem.Type.HELMET)));
    public static final Supplier<Item> ECHOBOUND_CHESTPLATE = ITEMS.register("echobound_chestplate",
        () -> new ArmorItem(ECHOBOUND_ARMOR, ArmorItem.Type.CHESTPLATE, ResonantMaterials.echoboundArmorProperties(ArmorItem.Type.CHESTPLATE)));
    public static final Supplier<Item> ECHOBOUND_LEGGINGS = ITEMS.register("echobound_leggings",
        () -> new ArmorItem(ECHOBOUND_ARMOR, ArmorItem.Type.LEGGINGS, ResonantMaterials.echoboundArmorProperties(ArmorItem.Type.LEGGINGS)));
    public static final Supplier<Item> ECHOBOUND_BOOTS = ITEMS.register("echobound_boots",
        () -> new ArmorItem(ECHOBOUND_ARMOR, ArmorItem.Type.BOOTS, ResonantMaterials.echoboundArmorProperties(ArmorItem.Type.BOOTS)));
    public static final Supplier<Item> ECHOBOUND_SWORD = ITEMS.register("echobound_sword",
        () -> new EchoboundSwordItem(ResonantMaterials.ECHOBOUND_TOOLS,
            new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC).durability(com.velorise.emc.common.LastEchoProfile.ECHOBOUND_TOOL_DURABILITY)
                .component(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                    new net.minecraft.world.item.component.Unbreakable(false))
                .attributes(net.minecraft.world.item.SwordItem.createAttributes(ResonantMaterials.ECHOBOUND_TOOLS, 3.0F, -2.4F))));
    public static final Supplier<Item> ECHOBOUND_PICKAXE = ITEMS.register("echobound_pickaxe",
        () -> new EchoboundPickaxeItem(ResonantMaterials.ECHOBOUND_TOOLS,
            new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC).durability(com.velorise.emc.common.LastEchoProfile.ECHOBOUND_TOOL_DURABILITY)
                .component(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                    new net.minecraft.world.item.component.Unbreakable(false))
                .attributes(net.minecraft.world.item.PickaxeItem.createAttributes(ResonantMaterials.ECHOBOUND_TOOLS, 3.0F, -2.8F))));

    // Blocks - dùng BLOCKS.register() cho custom block class
    // World-Tier Resonant ores and storage blocks
    public static final Supplier<Block> VERDANT_RESONANT_ORE = BLOCKS.register("verdant_resonant_ore",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIAMOND_ORE).requiresCorrectToolForDrops()));
    public static final Supplier<Block> DEEPSLATE_VERDANT_RESONANT_ORE = BLOCKS.register("deepslate_verdant_resonant_ore",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE).requiresCorrectToolForDrops()));
    public static final Supplier<Block> INFERNAL_RESONANT_ORE = BLOCKS.register("infernal_resonant_ore",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS).requiresCorrectToolForDrops()));
    public static final Supplier<Block> AETHERIC_RESONANT_ORE = BLOCKS.register("aetheric_resonant_ore",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.END_STONE).strength(5.0F, 9.0F).requiresCorrectToolForDrops()));
    public static final Supplier<Block> ECHOING_RESONANT_ORE = BLOCKS.register("echoing_resonant_ore",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE).strength(7.0F, 12.0F).requiresCorrectToolForDrops()));
    public static final Supplier<Block> VERDANT_RESONANT_BLOCK = BLOCKS.register("verdant_resonant_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)));
    public static final Supplier<Block> INFERNAL_RESONANT_BLOCK = BLOCKS.register("infernal_resonant_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)));
    public static final Supplier<Block> AETHERIC_RESONANT_BLOCK = BLOCKS.register("aetheric_resonant_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)));
    public static final Supplier<Block> ECHOING_RESONANT_BLOCK = BLOCKS.register("echoing_resonant_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)));
    public static final Supplier<Block> INFERNAL_ALTAR = BLOCKS.register("infernal_altar",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS)
            .strength(-1.0F, 3600000.0F).noLootTable()));
    public static final Supplier<Block> AETHERIC_ALTAR = BLOCKS.register("aetheric_altar",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.END_STONE_BRICKS)
            .strength(-1.0F, 3600000.0F).noLootTable()));
    public static final Supplier<Block> DEEPBOUND_ALTAR = BLOCKS.register("deepbound_altar",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
            .strength(-1.0F, 3600000.0F).noLootTable()));
    public static final Supplier<Block> LAST_ECHO_ALTAR = BLOCKS.register("last_echo_altar",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
            .strength(-1.0F, 3600000.0F).noLootTable()));

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
    public static final Supplier<Block> ALCHEMICAL_ANVIL_I = BLOCKS.register("alchemical_anvil_i",
        () -> new AlchemicalAnvilBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.METAL)
            .requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
            .sound(net.minecraft.world.level.block.SoundType.ANVIL), 1));
    public static final Supplier<Block> ALCHEMICAL_ANVIL_II = BLOCKS.register("alchemical_anvil_ii",
        () -> new AlchemicalAnvilBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.METAL)
            .requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
            .sound(net.minecraft.world.level.block.SoundType.ANVIL), 2));
    public static final Supplier<Block> ALCHEMICAL_ANVIL_III = BLOCKS.register("alchemical_anvil_iii",
        () -> new AlchemicalAnvilBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.METAL)
            .requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
            .sound(net.minecraft.world.level.block.SoundType.ANVIL), 3));
    public static final Supplier<Block> ALCHEMICAL_ANVIL_IV = BLOCKS.register("alchemical_anvil_iv",
        () -> new AlchemicalAnvilBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.METAL)
            .requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
            .sound(net.minecraft.world.level.block.SoundType.ANVIL), 4));
    public static final Supplier<Block> PURIFICATION_GRINDSTONE = BLOCKS.register("purification_grindstone",
        () -> new PurificationGrindstoneBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRINDSTONE)));
    public static final Supplier<Block> PORTABLE_CRAFTING_TABLE = BLOCKS.register("portable_crafting_table",
        () -> new ResonantCraftingTableBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE).noLootTable()));
    public static final Supplier<Block> PORTABLE_STONECUTTER = BLOCKS.register("portable_stonecutter",
        () -> new PortableStonecutterBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.STONECUTTER)));
    public static final Supplier<Block> PORTABLE_LOOM = BLOCKS.register("portable_loom",
        () -> new PortableLoomBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LOOM)));
    public static final Supplier<Block> PORTABLE_CARTOGRAPHY_TABLE = BLOCKS.register("portable_cartography_table",
        () -> new PortableCartographyTableBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE)));
    public static final Supplier<Block> WAYPOINT_ANCHOR = BLOCKS.register("waypoint_anchor",
        () -> new WaypointAnchorBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_PURPLE)
            .requiresCorrectToolForDrops().strength(3.5F, 12.0F)
            .sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final Supplier<Block> RESONANCE_SPIRE = BLOCKS.register("resonance_spire",
        () -> new AuraNodeBlock(BlockBehaviour.Properties.of()
            .strength(-1.0F, 3_600_000.0F)
            .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
            .lightLevel(state -> state.getValue(AuraNodeBlock.ACTIVE) ? 12 : 2)
            .noLootTable()));
    public static final Supplier<Block> CONDENSED_CORE_BLOCK_I = BLOCKS.register("condensed_core_block_i",
        () -> condensedCoreBlock(net.minecraft.world.level.material.MapColor.COLOR_CYAN, 5));
    public static final Supplier<Block> CONDENSED_CORE_BLOCK_II = BLOCKS.register("condensed_core_block_ii",
        () -> condensedCoreBlock(net.minecraft.world.level.material.MapColor.COLOR_ORANGE, 8));
    public static final Supplier<Block> CONDENSED_CORE_BLOCK_III = BLOCKS.register("condensed_core_block_iii",
        () -> condensedCoreBlock(net.minecraft.world.level.material.MapColor.COLOR_PURPLE, 11));
    public static final Supplier<Block> CONDENSED_CORE_BLOCK_IV = BLOCKS.register("condensed_core_block_iv",
        () -> condensedCoreBlock(net.minecraft.world.level.material.MapColor.COLOR_BLACK, 15));
    public static final Supplier<Block> DEEPBOUND_ENDER_CHEST = BLOCKS.register("deepbound_ender_chest",
        () -> new DeepboundEnderChestBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.ENDER_CHEST).noLootTable()));
    public static final Supplier<Block> MATTERBURST_TNT = BLOCKS.register("matterburst_tnt",
        () -> new MatterburstTntBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.TNT).noLootTable()));
    public static final Supplier<Block> CURSED_SOUL_FIRE = BLOCKS.register("cursed_soul_fire",
        () -> new CursedSoulFireBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SOUL_FIRE).noLootTable()));

    public static final Supplier<Block> RESONANT_FURNACE = BLOCKS.register("resonant_furnace",
        () -> new ResonantFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.FURNACE).noLootTable()));
    public static final Supplier<Block> RESONANT_SMOKER = BLOCKS.register("resonant_smoker",
        () -> new ResonantSmokerBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SMOKER).noLootTable()));
    public static final Supplier<Block> RESONANT_BLAST_FURNACE = BLOCKS.register("resonant_blast_furnace",
        () -> new ResonantBlastFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BLAST_FURNACE).noLootTable()));
    public static final Supplier<Block> RESONANCE_JUKEBOX = BLOCKS.register("resonance_jukebox",
        () -> new ResonanceJukeboxBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUKEBOX).noLootTable()));

    // Block Items - dùng ITEMS.register() cho custom block item
    public static final Supplier<Item> VERDANT_RESONANT_ORE_ITEM = ITEMS.register("verdant_resonant_ore", () -> new BlockItem(VERDANT_RESONANT_ORE.get(), new Item.Properties()));
    public static final Supplier<Item> DEEPSLATE_VERDANT_RESONANT_ORE_ITEM = ITEMS.register("deepslate_verdant_resonant_ore", () -> new BlockItem(DEEPSLATE_VERDANT_RESONANT_ORE.get(), new Item.Properties()));
    public static final Supplier<Item> INFERNAL_RESONANT_ORE_ITEM = ITEMS.register("infernal_resonant_ore", () -> new BlockItem(INFERNAL_RESONANT_ORE.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> AETHERIC_RESONANT_ORE_ITEM = ITEMS.register("aetheric_resonant_ore", () -> new BlockItem(AETHERIC_RESONANT_ORE.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> ECHOING_RESONANT_ORE_ITEM = ITEMS.register("echoing_resonant_ore", () -> new BlockItem(ECHOING_RESONANT_ORE.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> VERDANT_RESONANT_BLOCK_ITEM = ITEMS.register("verdant_resonant_block", () -> new BlockItem(VERDANT_RESONANT_BLOCK.get(), new Item.Properties()));
    public static final Supplier<Item> INFERNAL_RESONANT_BLOCK_ITEM = ITEMS.register("infernal_resonant_block", () -> new BlockItem(INFERNAL_RESONANT_BLOCK.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> AETHERIC_RESONANT_BLOCK_ITEM = ITEMS.register("aetheric_resonant_block", () -> new BlockItem(AETHERIC_RESONANT_BLOCK.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> ECHOING_RESONANT_BLOCK_ITEM = ITEMS.register("echoing_resonant_block", () -> new BlockItem(ECHOING_RESONANT_BLOCK.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> INFERNAL_ALTAR_ITEM = ITEMS.register("infernal_altar",
        () -> new BlockItem(INFERNAL_ALTAR.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> AETHERIC_ALTAR_ITEM = ITEMS.register("aetheric_altar",
        () -> new BlockItem(AETHERIC_ALTAR.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> DEEPBOUND_ALTAR_ITEM = ITEMS.register("deepbound_altar",
        () -> new BlockItem(DEEPBOUND_ALTAR.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> LAST_ECHO_ALTAR_ITEM = ITEMS.register("last_echo_altar",
        () -> new BlockItem(LAST_ECHO_ALTAR.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> CONDENSED_CORE_BLOCK_I_ITEM = ITEMS.register("condensed_core_block_i", () -> new BlockItem(CONDENSED_CORE_BLOCK_I.get(), new Item.Properties()));
    public static final Supplier<Item> CONDENSED_CORE_BLOCK_II_ITEM = ITEMS.register("condensed_core_block_ii", () -> new BlockItem(CONDENSED_CORE_BLOCK_II.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> CONDENSED_CORE_BLOCK_III_ITEM = ITEMS.register("condensed_core_block_iii", () -> new BlockItem(CONDENSED_CORE_BLOCK_III.get(), new Item.Properties().fireResistant()));
    public static final Supplier<Item> CONDENSED_CORE_BLOCK_IV_ITEM = ITEMS.register("condensed_core_block_iv", () -> new BlockItem(CONDENSED_CORE_BLOCK_IV.get(), new Item.Properties().fireResistant()));

    public static final Supplier<Item> TRANSMUTATION_TABLE_ITEM = ITEMS.register("transmutation_table",
        () -> new BlockItem(TRANSMUTATION_TABLE.get(), new Item.Properties()));
    public static final Supplier<Item> ARCANE_TRANSMUTATION_TABLE_ITEM = ITEMS.register("arcane_transmutation_table",
        () -> new GlintBlockItem(ARCANE_TRANSMUTATION_TABLE.get(), new Item.Properties()));

    // Alchemical Fuel Block Items
    public static final Supplier<Item> ALCHEMICAL_COAL_BLOCK_ITEM = ITEMS.register("alchemical_coal_block",
        () -> new AlchemicalFuelBlockItem(ALCHEMICAL_COAL_BLOCK.get(), new Item.Properties(), 128000));
    public static final Supplier<Item> MOBIUS_FUEL_BLOCK_ITEM = ITEMS.register("mobius_fuel_block",
        () -> new AlchemicalFuelBlockItem(MOBIUS_FUEL_BLOCK.get(), new Item.Properties(), 1024000));
    public static final Supplier<Item> AETERNALIS_FUEL_BLOCK_ITEM = ITEMS.register("aeternalis_fuel_block",
        () -> new AlchemicalFuelBlockItem(AETERNALIS_FUEL_BLOCK.get(), new Item.Properties(), 8192000));

    // Alchemical Hourglass Block Item
    public static final Supplier<Item> ALCHEMICAL_HOURGLASS_ITEM = ITEMS.register("alchemical_hourglass",
        () -> new AlchemicalHourglassItem(ALCHEMICAL_HOURGLASS.get(), new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ALCHEMICAL_ANVIL_I_ITEM = ITEMS.register("alchemical_anvil_i",
        () -> new AlchemicalAnvilItem(ALCHEMICAL_ANVIL_I.get(), new Item.Properties().stacksTo(1), 1));
    public static final Supplier<Item> ALCHEMICAL_ANVIL_II_ITEM = ITEMS.register("alchemical_anvil_ii",
        () -> new AlchemicalAnvilItem(ALCHEMICAL_ANVIL_II.get(), new Item.Properties().stacksTo(1), 2));
    public static final Supplier<Item> ALCHEMICAL_ANVIL_III_ITEM = ITEMS.register("alchemical_anvil_iii",
        () -> new AlchemicalAnvilItem(ALCHEMICAL_ANVIL_III.get(), new Item.Properties().stacksTo(1), 3));
    public static final Supplier<Item> ALCHEMICAL_ANVIL_IV_ITEM = ITEMS.register("alchemical_anvil_iv",
        () -> new AlchemicalAnvilItem(ALCHEMICAL_ANVIL_IV.get(), new Item.Properties().stacksTo(1), 4));
    public static final Supplier<Item> PURIFICATION_GRINDSTONE_ITEM = ITEMS.register("purification_grindstone",
        () -> new PurificationGrindstoneItem(PURIFICATION_GRINDSTONE.get(), new Item.Properties()));
    public static final Supplier<Item> PORTABLE_CRAFTING_TABLE_ITEM = ITEMS.register("portable_crafting_table",
        () -> new PortableWorkstationItem(PORTABLE_CRAFTING_TABLE.get(), new Item.Properties(), PortableWorkstationItem.Kind.CRAFTING));
    public static final Supplier<Item> PORTABLE_STONECUTTER_ITEM = ITEMS.register("portable_stonecutter",
        () -> new PortableWorkstationItem(PORTABLE_STONECUTTER.get(), new Item.Properties(), PortableWorkstationItem.Kind.STONECUTTER));
    public static final Supplier<Item> PORTABLE_LOOM_ITEM = ITEMS.register("portable_loom",
        () -> new PortableWorkstationItem(PORTABLE_LOOM.get(), new Item.Properties(), PortableWorkstationItem.Kind.LOOM));
    public static final Supplier<Item> PORTABLE_CARTOGRAPHY_TABLE_ITEM = ITEMS.register("portable_cartography_table",
        () -> new PortableWorkstationItem(PORTABLE_CARTOGRAPHY_TABLE.get(), new Item.Properties(), PortableWorkstationItem.Kind.CARTOGRAPHY));
    public static final Supplier<Item> WAYPOINT_ANCHOR_ITEM = ITEMS.register("waypoint_anchor",
        () -> new GlintBlockItem(WAYPOINT_ANCHOR.get(), new Item.Properties()));
    public static final Supplier<Item> DEEPBOUND_ENDER_CHEST_ITEM = ITEMS.register("deepbound_ender_chest", () -> new GlintBlockItem(DEEPBOUND_ENDER_CHEST.get(), new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> TIDAL_TURTLE_HELMET = ITEMS.register("tidal_turtle_helmet",
        () -> new net.minecraft.world.item.ArmorItem(net.minecraft.world.item.ArmorMaterials.TURTLE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).durability(275)));
    public static final Supplier<Item> BOUND_COMPASS = ITEMS.register("bound_compass", () -> new GlintItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> SOULBOUND_RECOVERY_COMPASS = ITEMS.register("soulbound_recovery_compass", () -> new GlintItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> MATTERBURST_TNT_ITEM = ITEMS.register("matterburst_tnt", () -> new MatterburstTntItem(MATTERBURST_TNT.get(), new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> MATTERBURST_MINECART_ITEM = ITEMS.register("matterburst_minecart", () -> new MatterburstMinecartItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> RESONANT_FURNACE_ITEM = ITEMS.register("resonant_furnace",
        () -> new ResonantBlockItem(RESONANT_FURNACE.get(), new Item.Properties()));
    public static final Supplier<Item> RESONANT_SMOKER_ITEM = ITEMS.register("resonant_smoker",
        () -> new ResonantBlockItem(RESONANT_SMOKER.get(), new Item.Properties()));
    public static final Supplier<Item> RESONANT_BLAST_FURNACE_ITEM = ITEMS.register("resonant_blast_furnace",
        () -> new ResonantBlockItem(RESONANT_BLAST_FURNACE.get(), new Item.Properties()));
    public static final Supplier<Item> RESONANCE_JUKEBOX_ITEM = ITEMS.register("resonance_jukebox",
        () -> new ResonanceJukeboxItem(RESONANCE_JUKEBOX.get(), new Item.Properties()));
    public static final Supplier<EntityType<InfernalCrucibleEntity>> INFERNAL_CRUCIBLE = ENTITY_TYPES.register("infernal_crucible",
        () -> EntityType.Builder.of(InfernalCrucibleEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
            .sized(2.4F, 4.2F).clientTrackingRange(12).updateInterval(2).build(MODID + ":infernal_crucible"));
    public static final Supplier<EntityType<EchoboundDragonEntity>> ECHOBOUND_DRAGON = ENTITY_TYPES.register("echobound_dragon",
        () -> EntityType.Builder.of(EchoboundDragonEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
            .sized(5.85F, 3.25F).clientTrackingRange(16).updateInterval(2)
            .build(MODID + ":echobound_dragon"));
    public static final Supplier<EntityType<AethericBulwarkEntity>> AETHERIC_BULWARK = ENTITY_TYPES.register("aetheric_bulwark",
        () -> EntityType.Builder.of(AethericBulwarkEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
            .sized(1.0F, 1.0F).clientTrackingRange(12).updateInterval(2).build(MODID + ":aetheric_bulwark"));
    public static final Supplier<EntityType<DeepboundWardenEntity>> DEEPBOUND_WARDEN = ENTITY_TYPES.register("deepbound_warden",
        () -> EntityType.Builder.of(DeepboundWardenEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
            .sized(1.5F, 3.5F).clientTrackingRange(12).updateInterval(2).build(MODID + ":deepbound_warden"));

    // Block Entity Types
    public static final Supplier<BlockEntityType<PassiveEmcTableBlockEntity>> PASSIVE_EMC_TABLE_BE = BLOCK_ENTITIES.register("passive_emc_table",
        () -> BlockEntityType.Builder.of(PassiveEmcTableBlockEntity::new,
            TRANSMUTATION_TABLE.get(), ARCANE_TRANSMUTATION_TABLE.get()).build(null));
    public static final Supplier<BlockEntityType<AlchemicalHourglassBlockEntity>> ALCHEMICAL_HOURGLASS_BE = BLOCK_ENTITIES.register("alchemical_hourglass",
        () -> BlockEntityType.Builder.of(AlchemicalHourglassBlockEntity::new, ALCHEMICAL_HOURGLASS.get()).build(null));
    public static final Supplier<BlockEntityType<AuraNodeBlockEntity>> RESONANCE_SPIRE_BE = BLOCK_ENTITIES.register("resonance_spire",
        () -> BlockEntityType.Builder.of(AuraNodeBlockEntity::new, RESONANCE_SPIRE.get()).build(null));
    public static final Supplier<BlockEntityType<DeepboundEnderChestBlockEntity>> DEEPBOUND_ENDER_CHEST_BE = BLOCK_ENTITIES.register("deepbound_ender_chest", () -> BlockEntityType.Builder.of(DeepboundEnderChestBlockEntity::new, DEEPBOUND_ENDER_CHEST.get()).build(null));

    public static final Supplier<BlockEntityType<ResonantFurnaceBlockEntity>> RESONANT_FURNACE_BE = BLOCK_ENTITIES.register("resonant_furnace",
        () -> BlockEntityType.Builder.of(ResonantFurnaceBlockEntity::new, RESONANT_FURNACE.get()).build(null));
    public static final Supplier<BlockEntityType<ResonantSmokerBlockEntity>> RESONANT_SMOKER_BE = BLOCK_ENTITIES.register("resonant_smoker",
        () -> BlockEntityType.Builder.of(ResonantSmokerBlockEntity::new, RESONANT_SMOKER.get()).build(null));
    public static final Supplier<BlockEntityType<ResonantBlastFurnaceBlockEntity>> RESONANT_BLAST_FURNACE_BE = BLOCK_ENTITIES.register("resonant_blast_furnace",
        () -> BlockEntityType.Builder.of(ResonantBlastFurnaceBlockEntity::new, RESONANT_BLAST_FURNACE.get()).build(null));
    public static final Supplier<BlockEntityType<ResonanceJukeboxBlockEntity>> RESONANCE_JUKEBOX_BE = BLOCK_ENTITIES.register("resonance_jukebox",
        () -> BlockEntityType.Builder.of(ResonanceJukeboxBlockEntity::new, RESONANCE_JUKEBOX.get()).build(null));

    // Menu Types
    public static final Supplier<MenuType<TransmutationMenu>> TRANSMUTATION_MENU_TYPE = MENUS.register("transmutation_menu",
        () -> new MenuType<>(TransmutationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<ArcaneTransmutationMenu>> ARCANE_TRANSMUTATION_MENU_TYPE = MENUS.register("arcane_transmutation_menu",
        () -> new MenuType<>(ArcaneTransmutationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<ResonantFurnaceMenu>> RESONANT_FURNACE_MENU_TYPE = MENUS.register("resonant_furnace_menu",
        () -> new MenuType<>(ResonantFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<ResonanceJukeboxMenu>> RESONANCE_JUKEBOX_MENU_TYPE = MENUS.register("resonance_jukebox_menu",
        () -> new MenuType<>(ResonanceJukeboxMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<PurificationGrindstoneMenu>> PURIFICATION_GRINDSTONE_MENU_TYPE = MENUS.register("purification_grindstone_menu",
        () -> new MenuType<>(PurificationGrindstoneMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<AlchemicalHourglassMenu>> ALCHEMICAL_HOURGLASS_MENU_TYPE = MENUS.register("alchemical_hourglass_menu",
        () -> new MenuType<>(AlchemicalHourglassMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<DeepboundStorageMenu>> DEEPBOUND_STORAGE_MENU_TYPE = MENUS.register("deepbound_storage_menu",
        () -> new MenuType<>(DeepboundStorageMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<CoreAuraMachineMenu>> CORE_AURA_MACHINE_MENU_TYPE = MENUS.register("core_aura_machine_menu", () -> new MenuType<>(CoreAuraMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));
    // Data Attachments
    public static final Supplier<AttachmentType<PlayerEMC>> PLAYER_EMC = ATTACHMENT_TYPES.register("player_emc",
        () -> AttachmentType.builder(PlayerEMC::new)
            .serialize(PlayerEMC.CODEC)
            .copyOnDeath()
            .build());

    // Creative Tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> VELORISE_EMC_TAB = CREATIVE_MODE_TABS.register("velorise_emc_tab",
        () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("creativetab.velorise_emc"))
            .icon(() -> new ItemStack(PHILOSOPHERS_STONE.get()))
            .displayItems((parameters, output) -> {
                output.accept(PHILOSOPHERS_STONE.get());
                output.accept(TRANSMUTATION_TABLET.get());
                output.accept(TOME_OF_KNOWLEDGE.get());
                output.accept(KNOWLEDGE_SHARING_BOOK.get());
                output.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.II));
                output.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.III));
                output.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.IV));
                output.accept(CONDENSED_CORE_I.get());
                output.accept(CONDENSED_CORE_II.get());
                output.accept(CONDENSED_CORE_III.get());
                output.accept(CONDENSED_CORE_IV.get());
                output.accept(CONDENSED_CORE_BLOCK_I_ITEM.get());
                output.accept(CONDENSED_CORE_BLOCK_II_ITEM.get());
                output.accept(CONDENSED_CORE_BLOCK_III_ITEM.get());
                output.accept(CONDENSED_CORE_BLOCK_IV_ITEM.get());
                output.accept(INFERNAL_ASCENSION_TEMPLATE.get());
                output.accept(END_ASCENSION_TEMPLATE.get());
                output.accept(DEEP_CONVERGENCE_TEMPLATE.get());
                output.accept(EYE_OF_THE_LAST_ECHO.get());
                output.accept(EYE_OF_THE_CRUCIBLE.get());
                output.accept(AETHERIC_BULWARK_EYE.get());
                output.accept(DEEPBOUND_EYE.get());
                output.accept(HEART_OF_THE_LAST_ECHO.get());
                output.accept(ECHOBOUND_DRAGON_SCALE.get());
                output.accept(ALCHEMICAL_COAL.get());
                output.accept(MOBIUS_FUEL.get());
                output.accept(AETERNALIS_FUEL.get());
                output.accept(RAW_VERDANT_RESONANT.get()); output.accept(VERDANT_RESONANT_INGOT.get());
                output.accept(RAW_INFERNAL_RESONANT.get()); output.accept(INFERNAL_RESONANT_INGOT.get());
                output.accept(RAW_AETHERIC_RESONANT.get()); output.accept(AETHERIC_RESONANT_INGOT.get());
                output.accept(RAW_ECHOING_RESONANT.get()); output.accept(ECHOING_RESONANT_INGOT.get());
                output.accept(VERDANT_RESONANT_ORE_ITEM.get()); output.accept(DEEPSLATE_VERDANT_RESONANT_ORE_ITEM.get()); output.accept(VERDANT_RESONANT_BLOCK_ITEM.get());
                output.accept(INFERNAL_RESONANT_ORE_ITEM.get()); output.accept(INFERNAL_RESONANT_BLOCK_ITEM.get());
                output.accept(AETHERIC_RESONANT_ORE_ITEM.get()); output.accept(AETHERIC_RESONANT_BLOCK_ITEM.get());
                output.accept(ECHOING_RESONANT_ORE_ITEM.get()); output.accept(ECHOING_RESONANT_BLOCK_ITEM.get());
                output.accept(VERDANT_RESONANT_SWORD.get()); output.accept(VERDANT_RESONANT_PICKAXE.get()); output.accept(VERDANT_RESONANT_AXE.get()); output.accept(VERDANT_RESONANT_SHOVEL.get()); output.accept(VERDANT_RESONANT_HOE.get());
                output.accept(VERDANT_RESONANT_HELMET.get()); output.accept(VERDANT_RESONANT_CHESTPLATE.get()); output.accept(VERDANT_RESONANT_LEGGINGS.get()); output.accept(VERDANT_RESONANT_BOOTS.get());
                output.accept(INFERNAL_RESONANT_SWORD.get()); output.accept(INFERNAL_RESONANT_PICKAXE.get()); output.accept(INFERNAL_RESONANT_AXE.get()); output.accept(INFERNAL_RESONANT_SHOVEL.get()); output.accept(INFERNAL_RESONANT_HOE.get());
                output.accept(INFERNAL_RESONANT_HELMET.get()); output.accept(INFERNAL_RESONANT_CHESTPLATE.get()); output.accept(INFERNAL_RESONANT_LEGGINGS.get()); output.accept(INFERNAL_RESONANT_BOOTS.get());
                output.accept(AETHERIC_RESONANT_SWORD.get()); output.accept(AETHERIC_RESONANT_PICKAXE.get()); output.accept(AETHERIC_RESONANT_AXE.get()); output.accept(AETHERIC_RESONANT_SHOVEL.get()); output.accept(AETHERIC_RESONANT_HOE.get());
                output.accept(AETHERIC_RESONANT_HELMET.get()); output.accept(AETHERIC_RESONANT_CHESTPLATE.get()); output.accept(AETHERIC_RESONANT_LEGGINGS.get()); output.accept(AETHERIC_RESONANT_BOOTS.get());
                output.accept(ECHOING_RESONANT_SWORD.get()); output.accept(ECHOING_RESONANT_PICKAXE.get()); output.accept(ECHOING_RESONANT_AXE.get()); output.accept(ECHOING_RESONANT_SHOVEL.get()); output.accept(ECHOING_RESONANT_HOE.get());
                output.accept(ECHOING_RESONANT_HELMET.get()); output.accept(ECHOING_RESONANT_CHESTPLATE.get()); output.accept(ECHOING_RESONANT_LEGGINGS.get()); output.accept(ECHOING_RESONANT_BOOTS.get());
                output.accept(ECHOBOUND_HELMET.get()); output.accept(ECHOBOUND_CHESTPLATE.get()); output.accept(ECHOBOUND_LEGGINGS.get()); output.accept(ECHOBOUND_BOOTS.get());
                output.accept(ECHOBOUND_SWORD.get()); output.accept(ECHOBOUND_PICKAXE.get());
                output.accept(createTieredBlockStack(ATTRACTION_CATALYST, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(CHALICE_OF_TRANSMUTATION, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(MAGIC_MIRROR, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(VOID_TRANSLOCATOR.get());
                // These are one registry item each. Higher Core tiers are installed through
                // the anvil; the creative tab should not show four visually identical copies.
                output.accept(createTieredBlockStack(PURIFYING_MILK, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(DRAGONFIRE_SCEPTER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(RESONANT_ECHO, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(INFERNO_ROD, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(GHASTFIRE_STAFF, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(WITHER_SCEPTER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(SHULKER_SEEKER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(GUARDIAN_LENS, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(EVOKER_FANG_STAFF, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(STORMCALLER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(FROSTBOLT_WAND, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(WEBCASTER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(TRANSMUTATION_TABLE_ITEM.get());
                output.accept(ARCANE_TRANSMUTATION_TABLE_ITEM.get());
                output.accept(ALCHEMICAL_COAL_BLOCK_ITEM.get());
                output.accept(MOBIUS_FUEL_BLOCK_ITEM.get());
                output.accept(AETERNALIS_FUEL_BLOCK_ITEM.get());
                output.accept(createTieredBlockStack(ALCHEMICAL_HOURGLASS_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(ALCHEMICAL_ANVIL_I_ITEM.get());
                output.accept(ALCHEMICAL_ANVIL_II_ITEM.get());
                output.accept(ALCHEMICAL_ANVIL_III_ITEM.get());
                output.accept(ALCHEMICAL_ANVIL_IV_ITEM.get());
                output.accept(PURIFICATION_GRINDSTONE_ITEM.get());
                output.accept(PORTABLE_CRAFTING_TABLE_ITEM.get());
                output.accept(WAYPOINT_ANCHOR_ITEM.get());
                output.accept(createTieredBlockStack(RESONANT_FURNACE_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(RESONANT_SMOKER_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(RESONANT_BLAST_FURNACE_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(MATTERBURST_TNT_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(createTieredBlockStack(MATTERBURST_MINECART_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(RESONANCE_JUKEBOX_ITEM.get());
                output.accept(BOUND_COMPASS.get());
                output.accept(createTieredBlockStack(SOULBOUND_RECOVERY_COMPASS, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(SpecialUpgradeUtil.createFixedVariant(
                    new ItemStack(net.minecraft.world.item.Items.SPYGLASS),
                    SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS,
                    "item.velorise_emc.far_sight_spyglass"));
                output.accept(ImbuedEquipmentUtil.createCreativeVariant(new ItemStack(net.minecraft.world.item.Items.FISHING_ROD)));
                output.accept(createTieredBlockStack(CURSED_IGNITER, com.velorise.emc.common.WirelessCoreTier.I));
                output.accept(TIDAL_TURTLE_HELMET.get());
            })
            .build()
    );

    private static Block condensedCoreBlock(net.minecraft.world.level.material.MapColor color, int light) {
        return new Block(BlockBehaviour.Properties.of()
            .mapColor(color).requiresCorrectToolForDrops().strength(5.0F, 12.0F)
            .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
            .lightLevel(state -> light));
    }

    private static Item.Properties resonantProperties(int tier) {
        Item.Properties properties = new Item.Properties();
        return com.velorise.emc.common.ResonantEquipmentProfile.fireproof(tier) ? properties.fireResistant() : properties;
    }

    private static ItemStack createTieredBlockStack(Supplier<Item> item, com.velorise.emc.common.WirelessCoreTier tier) {
        ItemStack stack = new ItemStack(item.get());
        CoreTierData.set(stack, tier);
        return stack;
    }

    public VeloriseEMC(IEventBus modEventBus, ModContainer container) {
        // Register the Deferred Registers to the mod event bus
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);

        // Register mod bus listeners
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::buildContents);
        modEventBus.addListener(this::registerEntityAttributes);
        registerPortableUiAdapters();

        // Register Config screen factory for Client mods list button
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mc, parent) -> new EMCConfigScreen(parent));
        }
    }

    private static void registerPortableUiAdapters() {
        InventoryUiFallback.register(
            PortableWorkstationItem::isPortableWorkstation,
            (player, stack) -> PortableWorkstationUi.open(
                player, ((PortableWorkstationItem) stack.getItem()).kind()));
    }

    private void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(INFERNAL_CRUCIBLE.get(), InfernalCrucibleEntity.createAttributes().build());
        event.put(ECHOBOUND_DRAGON.get(), EchoboundDragonEntity.createAttributes().build());
        event.put(AETHERIC_BULWARK.get(), AethericBulwarkEntity.createAttributes().build());
        event.put(DEEPBOUND_WARDEN.get(), DeepboundWardenEntity.createAttributes().build());
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);

        // Client-bound custom EMC sync packet (Server -> Client)
        registrar.playToClient(
            com.velorise.emc.network.SyncCustomEMCPayload.TYPE,
            com.velorise.emc.network.SyncCustomEMCPayload.STREAM_CODEC,
            ClientPayloadHandler::handleCustomEMCSync
        );

        registrar.playToClient(
            com.velorise.emc.network.SyncGameplayConfigPayload.TYPE,
            com.velorise.emc.network.SyncGameplayConfigPayload.STREAM_CODEC,
            ClientPayloadHandler::handleGameplayConfigSync
        );

        // Client-bound open config UI packet (Server -> Client)
        registrar.playToClient(
            com.velorise.emc.network.OpenConfigScreenPayload.TYPE,
            com.velorise.emc.network.OpenConfigScreenPayload.STREAM_CODEC,
            ClientPayloadHandler::handleOpenConfigScreen
        );

        registrar.playToClient(
            com.velorise.emc.network.JukeboxAudioPayload.TYPE,
            com.velorise.emc.network.JukeboxAudioPayload.STREAM_CODEC,
            ClientPayloadHandler::handleJukeboxAudio
        );

        // Server-bound custom EMC edit packet (Client -> Server)
        registrar.playToServer(
            com.velorise.emc.network.RequestUpdateEMCPayload.TYPE,
            com.velorise.emc.network.RequestUpdateEMCPayload.STREAM_CODEC,
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
            com.velorise.emc.network.FillCraftingFromEMCPayload.TYPE,
            com.velorise.emc.network.FillCraftingFromEMCPayload.STREAM_CODEC,
            ServerPayloadHandler::handleFillCrafting
        );

        // Server-bound alchemical tool change charge packet (Client -> Server)
        registrar.playToServer(
            com.velorise.emc.network.ChangeChargePayload.TYPE,
            com.velorise.emc.network.ChangeChargePayload.STREAM_CODEC,
            ServerPayloadHandler::handleChangeCharge
        );

        registrar.playToServer(
            com.velorise.emc.network.UpdateHubSettingsPayload.TYPE,
            com.velorise.emc.network.UpdateHubSettingsPayload.STREAM_CODEC,
            ServerPayloadHandler::handleUpdateHubSettings
        );

        registrar.playToServer(
            com.velorise.emc.network.ElytraBoostPayload.TYPE,
            com.velorise.emc.network.ElytraBoostPayload.STREAM_CODEC,
            ServerPayloadHandler::handleElytraBoost
        );

        registrar.playToServer(
            com.velorise.emc.network.RepairEquipmentPayload.TYPE,
            com.velorise.emc.network.RepairEquipmentPayload.STREAM_CODEC,
            ServerPayloadHandler::handleRepairEquipment
        );

        registrar.playToServer(
            com.velorise.emc.network.AquaticBreathPayload.TYPE,
            com.velorise.emc.network.AquaticBreathPayload.STREAM_CODEC,
            ServerPayloadHandler::handleAquaticBreath
        );

        registrar.playToServer(
            com.velorise.emc.network.OpenInventoryItemUiPayload.TYPE,
            com.velorise.emc.network.OpenInventoryItemUiPayload.STREAM_CODEC,
            ServerPayloadHandler::handleOpenInventoryItemUi
        );

        registrar.playToServer(
            com.velorise.emc.network.MiningUseStatePayload.TYPE,
            com.velorise.emc.network.MiningUseStatePayload.STREAM_CODEC,
            ServerPayloadHandler::handleMiningUseState
        );

    }

    private void buildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PHILOSOPHERS_STONE.get());
            event.accept(TRANSMUTATION_TABLET.get());
            event.accept(TOME_OF_KNOWLEDGE.get());
            event.accept(KNOWLEDGE_SHARING_BOOK.get());
            event.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.II));
            event.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.III));
            event.accept(CoreTierData.createCoreStack(com.velorise.emc.common.WirelessCoreTier.IV));
            event.accept(CONDENSED_CORE_I.get());
            event.accept(CONDENSED_CORE_II.get());
            event.accept(CONDENSED_CORE_III.get());
            event.accept(CONDENSED_CORE_IV.get());
            event.accept(CONDENSED_CORE_BLOCK_I_ITEM.get());
            event.accept(CONDENSED_CORE_BLOCK_II_ITEM.get());
            event.accept(CONDENSED_CORE_BLOCK_III_ITEM.get());
            event.accept(CONDENSED_CORE_BLOCK_IV_ITEM.get());
            event.accept(INFERNAL_ASCENSION_TEMPLATE.get());
            event.accept(END_ASCENSION_TEMPLATE.get());
            event.accept(DEEP_CONVERGENCE_TEMPLATE.get());
            event.accept(ALCHEMICAL_COAL.get());
            event.accept(MOBIUS_FUEL.get());
            event.accept(AETERNALIS_FUEL.get());
            event.accept(createTieredBlockStack(ATTRACTION_CATALYST, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(createTieredBlockStack(CHALICE_OF_TRANSMUTATION, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(createTieredBlockStack(MAGIC_MIRROR, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(VOID_TRANSLOCATOR.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRANSMUTATION_TABLE_ITEM.get());
            event.accept(ARCANE_TRANSMUTATION_TABLE_ITEM.get());
            event.accept(ALCHEMICAL_COAL_BLOCK_ITEM.get());
            event.accept(MOBIUS_FUEL_BLOCK_ITEM.get());
            event.accept(AETERNALIS_FUEL_BLOCK_ITEM.get());
            event.accept(createTieredBlockStack(ALCHEMICAL_HOURGLASS_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(createTieredBlockStack(MATTERBURST_TNT_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(createTieredBlockStack(MATTERBURST_MINECART_ITEM, com.velorise.emc.common.WirelessCoreTier.I));
            event.accept(ALCHEMICAL_ANVIL_I_ITEM.get());
            event.accept(ALCHEMICAL_ANVIL_II_ITEM.get());
            event.accept(ALCHEMICAL_ANVIL_III_ITEM.get());
            event.accept(ALCHEMICAL_ANVIL_IV_ITEM.get());
            event.accept(PURIFICATION_GRINDSTONE_ITEM.get());
            event.accept(PORTABLE_CRAFTING_TABLE_ITEM.get());
            event.accept(PORTABLE_STONECUTTER_ITEM.get());
            event.accept(PORTABLE_LOOM_ITEM.get());
            event.accept(PORTABLE_CARTOGRAPHY_TABLE_ITEM.get());
            event.accept(WAYPOINT_ANCHOR_ITEM.get());
            event.accept(RESONANT_FURNACE_ITEM.get());
            event.accept(RESONANT_SMOKER_ITEM.get());
            event.accept(RESONANT_BLAST_FURNACE_ITEM.get());
            event.accept(RESONANCE_JUKEBOX_ITEM.get());
        }
    }

    // Helper to send data sync packet to a specific ServerPlayer
    public static void syncPlayerData(ServerPlayer player) {
        PlayerEMC data = player.getData(PLAYER_EMC.get());
        TransmutationTaxService.applyDecay(data, TransmutationTaxService.nowMillis());
        com.velorise.emc.common.WorldProgressionState worldProgress =
            WorldProgressionData.get(player.server).snapshot();
        AdvancementHooks.syncWorldTierAdvancements(player, worldProgress.worldTier());
        PacketDistributor.sendToPlayer(player, new SyncPlayerEMCPayload(
            data.getEmc(), data.getLearnedItemsRL(), data.getSourceMode().ordinal(),
            data.getSafetyThreshold(), data.isHudEnabled(), data.getInventoryFuelBuffer(),
            data.getTransmutationUsage(), data.getTransmutationUsageDecayMillis(),
            data.getTransmutationTaxRemainder(), data.getTotalMaterializedItems(),
            data.getTotalMaterializationTaxPaid(), worldProgress.countsCopy(),
            worldProgress.contributorsCopy()
        ));
    }

    public static void syncCustomEMCOverrides(ServerPlayer player) {
        java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
        java.util.Map<ResourceLocation, com.velorise.emc.common.EmcPolicyOverride> fullPolicies = EMCRegistry.getFullPolicyOverrides();
        PacketDistributor.sendToPlayer(player, new com.velorise.emc.network.SyncCustomEMCPayload(
            fullOverrides, EMCRegistry.getConfiguredValueMode(), fullPolicies));
    }

    public static void syncGameplayConfig(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new com.velorise.emc.network.SyncGameplayConfigPayload(
            GameplayConfigManager.serverJson()));
    }

    // =======================================================================
    // Game Event Subscribers (NeoForge.EVENT_BUS)
    // @EventBusSubscriber auto-registers this class to the correct bus
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
            GameplayConfigManager.ReloadResult result = GameplayConfigManager.loadServer();
            System.out.println("[Velorise EMC] " + result.message());
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
                VanillaMachineRegistry.copyMachines(original, replacement);
                PersonalDeepboundEnderStorage.copyPlayerData(original, replacement);
                AdvancementHooks.copyPlayerData(original, replacement);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
                syncGameplayConfig(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
                syncGameplayConfig(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayerData(player);
                syncCustomEMCOverrides(player);
                syncGameplayConfig(player);
            }
        }

        @SubscribeEvent
        public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
            var server = event.getPlayerList().getServer();
            EMCRegistry.reloadAndRecalculate(server.getRecipeManager(), server.registryAccess());

            // Sync to all players on reload
            java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
            java.util.Map<ResourceLocation, com.velorise.emc.common.EmcPolicyOverride> fullPolicies = EMCRegistry.getFullPolicyOverrides();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(player, new com.velorise.emc.network.SyncCustomEMCPayload(
                    fullOverrides, EMCRegistry.getConfiguredValueMode(), fullPolicies));
                syncGameplayConfig(player);
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
            var dispatcher = event.getDispatcher();
            dispatcher.register(net.minecraft.commands.Commands.literal("velorise_emc")
                .then(net.minecraft.commands.Commands.literal("dump")
                    .executes(context -> {
                        try {
                            java.io.File file = new java.io.File("velorise_emc_missing_emc.txt");
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
                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Exported " + finalCount + " items without EMC to file " + file.getAbsolutePath()), true);
                        } catch (Exception e) {
                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Error exporting file: " + e.getMessage()));
                        }
                        return 1;
                    })
                )
                .then(net.minecraft.commands.Commands.literal("progress")
                    .executes(context -> WorldProgressCommands.show(context.getSource()))
                    .then(net.minecraft.commands.Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> WorldProgressCommands.reset(context.getSource()))
                    )
                    .then(net.minecraft.commands.Commands.literal("grant")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 4))
                            .executes(context -> WorldProgressCommands.grant(context.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier"))))
                    )
                )
                .then(net.minecraft.commands.Commands.literal("spawn")
                    .requires(source -> source.hasPermission(2))
                    .then(net.minecraft.commands.Commands.literal("structure")
                        .then(net.minecraft.commands.Commands.literal("tower_level")
                            .then(net.minecraft.commands.Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 4))
                                .executes(context -> {
                                    int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier");
                                    int forwardDistance = AuraNodeTowerStructure.radiusForTier(tier) + 6;
                                    net.minecraft.core.BlockPos feet = StructureCommandPlacement.ahead(
                                        context.getSource(), forwardDistance);
                                    AuraNodeTowerStructure.PlacementResult result = AuraNodeTowerStructure.place(
                                        context.getSource().getLevel(), feet.below(), tier, true);
                                    if (!result.success()) {
                                        context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(result.message()));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                        "Spawned tower_level " + tier + " (" + result.width() + "x" + result.width()
                                            + "x" + result.height() + ") with dormant Resonance Spire at "
                                            + result.nodePos().toShortString() + "."), true);
                                    return result.changedBlocks();
                                })
                            )
                        )
                        .then(net.minecraft.commands.Commands.literal("last_echo")
                            .executes(context -> LastEchoAdminCommands.placeCitadel(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("infernal_crucible")
                            .executes(context -> InfernalCrucibleArenaService.placeArena(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("aetheric_bulwark")
                            .executes(context -> PromotionBossArenaService.placeBulwark(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("deepbound_warden")
                            .executes(context -> PromotionBossArenaService.placeDeepbound(context.getSource()))
                        )
                    )
                    .then(net.minecraft.commands.Commands.literal("boss")
                        .then(net.minecraft.commands.Commands.literal("infernal_crucible")
                            .executes(context -> InfernalCrucibleArenaService.summon(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("last_echo")
                            .executes(context -> LastEchoAdminCommands.summonBoss(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("aetheric_bulwark")
                            .executes(context -> PromotionBossArenaService.summonBulwark(context.getSource()))
                        )
                        .then(net.minecraft.commands.Commands.literal("deepbound_warden")
                            .executes(context -> PromotionBossArenaService.summonDeepbound(context.getSource()))
                        )
                    )
                )
                .then(net.minecraft.commands.Commands.literal("locate")
                    .requires(source -> source.hasPermission(2))
                    .then(net.minecraft.commands.Commands.literal("infernal_crucible")
                        .executes(context -> LocateStructureCommand.locateInfernalCrucible(context.getSource())))
                    .then(net.minecraft.commands.Commands.literal("aetheric_bulwark")
                        .executes(context -> LocateStructureCommand.locateAethericBulwark(context.getSource())))
                    .then(net.minecraft.commands.Commands.literal("deepbound_warden")
                        .executes(context -> LocateStructureCommand.locateDeepboundWarden(context.getSource())))
                    .then(net.minecraft.commands.Commands.literal("last_echo")
                        .executes(context -> LocateStructureCommand.locateLastEcho(context.getSource())))
                )
                .then(net.minecraft.commands.Commands.literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PacketDistributor.sendToPlayer(player, new com.velorise.emc.network.OpenConfigScreenPayload());
                        return 1;
                    })
                    .then(net.minecraft.commands.Commands.literal("reload")
                        .executes(context -> {
                            GameplayConfigManager.ReloadResult result = GameplayConfigManager.loadServer();
                            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                syncGameplayConfig(player);
                            }
                            if (result.success()) {
                                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(result.message()), true);
                                return 1;
                            }
                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(result.message()));
                            return 0;
                        })
                    )
                )
            );
            LocateStructureCommand.register(dispatcher);
        }
    }

    // =======================================================================
    // Client-only Mod Event Subscribers (Mod Bus, CLIENT side only)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        public static final KeyMapping CHARGE_KEY = new KeyMapping(
            "key.velorise_emc.charge",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_V,
            "key.categories.velorise_emc"
        );
        public static final KeyMapping HUB_KEY = new KeyMapping(
            "key.velorise_emc.emc_hub",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_H,
            "key.categories.velorise_emc"
        );
        public static final KeyMapping REPAIR_KEY = new KeyMapping(
            "key.velorise_emc.repair_equipment",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_R,
            "key.categories.velorise_emc"
        );

        public static final KeyMapping AQUATIC_BREATH_KEY = new KeyMapping(
            "key.velorise_emc.aquatic_breath",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_G,
            "key.categories.velorise_emc"
        );

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(VeloriseEMC.TRANSMUTATION_MENU_TYPE.get(), TransmutationScreen::new);
            event.register(VeloriseEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get(), ArcaneTransmutationScreen::new);
            event.register(VeloriseEMC.RESONANT_FURNACE_MENU_TYPE.get(), ResonantFurnaceScreen::new);
            event.register(VeloriseEMC.RESONANCE_JUKEBOX_MENU_TYPE.get(), ResonanceJukeboxScreen::new);
            event.register(VeloriseEMC.PURIFICATION_GRINDSTONE_MENU_TYPE.get(), PurificationGrindstoneScreen::new);
            event.register(VeloriseEMC.ALCHEMICAL_HOURGLASS_MENU_TYPE.get(), AlchemicalHourglassScreen::new);
            event.register(VeloriseEMC.DEEPBOUND_STORAGE_MENU_TYPE.get(), DeepboundStorageScreen::new);
            event.register(VeloriseEMC.CORE_AURA_MACHINE_MENU_TYPE.get(), CoreAuraMachineScreen::new);
        }

        @SubscribeEvent
        public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(CHARGE_KEY);
            event.register(HUB_KEY);
            event.register(REPAIR_KEY);
            event.register(AQUATIC_BREATH_KEY);
        }

        @SubscribeEvent
        public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(VeloriseEMC.INFERNAL_CRUCIBLE.get(), com.velorise.emc.client.InfernalCrucibleRenderer::new);
            event.registerEntityRenderer(VeloriseEMC.ECHOBOUND_DRAGON.get(), com.velorise.emc.client.EchoboundDragonRenderer::new);
            event.registerEntityRenderer(VeloriseEMC.AETHERIC_BULWARK.get(), com.velorise.emc.client.AethericBulwarkRenderer::new);
            event.registerEntityRenderer(VeloriseEMC.DEEPBOUND_WARDEN.get(), net.minecraft.client.renderer.entity.WardenRenderer::new);
            event.registerBlockEntityRenderer(
                VeloriseEMC.ALCHEMICAL_HOURGLASS_BE.get(),
                com.velorise.emc.client.AlchemicalHourglassRenderer::new
            );
            event.registerBlockEntityRenderer(VeloriseEMC.DEEPBOUND_ENDER_CHEST_BE.get(), context -> new com.velorise.emc.client.CoreSocketRenderer<>(context));
            event.registerBlockEntityRenderer(VeloriseEMC.RESONANT_FURNACE_BE.get(), context -> new com.velorise.emc.client.CoreSocketRenderer<>(context));
            event.registerBlockEntityRenderer(VeloriseEMC.RESONANT_SMOKER_BE.get(), context -> new com.velorise.emc.client.CoreSocketRenderer<>(context));
            event.registerBlockEntityRenderer(VeloriseEMC.RESONANT_BLAST_FURNACE_BE.get(), context -> new com.velorise.emc.client.CoreSocketRenderer<>(context));
            event.registerBlockEntityRenderer(VeloriseEMC.RESONANCE_JUKEBOX_BE.get(), context -> new com.velorise.emc.client.CoreSocketRenderer<>(context));
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.velorise.emc.client.EchoboundDragonModel.LAYER,
                com.velorise.emc.client.EchoboundDragonModel::createBodyLayer);
        }
    }

    // =======================================================================
    // Client-only Game Event Subscribers (Game Bus, CLIENT side only)
    // =======================================================================
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {
        private static long lastHudEmc = Long.MIN_VALUE;
        private static double hudRate = 0.0D;
        private static boolean lastMiningUseState = false;
        private static int lastMiningHeartbeatTick = Integer.MIN_VALUE;
        private static double farSightZoom = 1.0D;
        private static boolean farSightWasUsing;

        @SubscribeEvent
        public static void onRenderMachineRange(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block event) {
            com.velorise.emc.client.RangeVisualizationRenderer.render(
                event.getPoseStack(), event.getMultiBufferSource(), event.getCamera(), event.getTarget().getBlockPos());
        }

        @SubscribeEvent
        public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                lastMiningUseState = false;
                lastMiningHeartbeatTick = Integer.MIN_VALUE;
                return;
            }
            if (mc.player.tickCount % 20 == 0) {
                PlayerEMC data = mc.player.getData(VeloriseEMC.PLAYER_EMC.get());
                long current = data.getEmc();
                if (lastHudEmc != Long.MIN_VALUE) hudRate = current - lastHudEmc;
                lastHudEmc = current;
            }
            syncMiningUseState(mc);
            if (mc.screen != null) return;

            while (ClientModEvents.HUB_KEY.consumeClick()) {
                mc.setScreen(new EmcHubScreen());
            }

            while (ClientModEvents.CHARGE_KEY.consumeClick()) {
                boolean decrease = net.minecraft.client.gui.screens.Screen.hasShiftDown();
                PacketDistributor.sendToServer(new com.velorise.emc.network.ChangeChargePayload(decrease));
            }

            while (ClientModEvents.REPAIR_KEY.consumeClick()) {
                boolean allEquipped = net.minecraft.client.gui.screens.Screen.hasShiftDown();
                PacketDistributor.sendToServer(new com.velorise.emc.network.RepairEquipmentPayload(allEquipped));
            }

            while (ClientModEvents.AQUATIC_BREATH_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new com.velorise.emc.network.AquaticBreathPayload());
            }

        }

        private static void syncMiningUseState(net.minecraft.client.Minecraft mc) {
            ItemStack held = mc.player.getMainHandItem();
            boolean active = mc.screen == null
                && mc.options.keyAttack.isDown()
                && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult
                && ImbuedEquipmentUtil.usesMiningActivation(held);
            boolean heartbeat = active && mc.player.tickCount - lastMiningHeartbeatTick >= 10;
            if (active == lastMiningUseState && !heartbeat) return;

            if (ImbuedEquipmentUtil.usesMiningActivation(held)) {
                ImbuedEquipmentUtil.setActive(held, active);
            }
            PacketDistributor.sendToServer(new com.velorise.emc.network.MiningUseStatePayload(active));
            lastMiningUseState = active;
            lastMiningHeartbeatTick = mc.player.tickCount;
        }

        @SubscribeEvent
        public static void onFarSightFov(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || !mc.player.isUsingItem()) {
                farSightWasUsing = false;
                return;
            }
            ItemStack use = mc.player.getUseItem();
            if (use.is(net.minecraft.world.item.Items.SPYGLASS)
                && SpecialUpgradeUtil.is(use, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS)) {
                if (!farSightWasUsing) farSightZoom = 1.0D;
                farSightWasUsing = true;
                event.setFOV(event.getFOV() * farSightZoom);
            } else {
                farSightWasUsing = false;
            }
        }

        @SubscribeEvent
        public static void onEchoboundPickaxeHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || !mc.player.isUsingItem()
                || !event.getItemStack().is(VeloriseEMC.ECHOBOUND_PICKAXE.get())) return;
            float elapsed = EchoboundPickaxeItem.SLAM_DURATION_TICKS
                - mc.player.getUseItemRemainingTicks() + event.getPartialTick();
            float progress = net.minecraft.util.Mth.clamp(
                elapsed / EchoboundPickaxeItem.SLAM_DURATION_TICKS, 0.0F, 1.0F);
            // First-person-only motion. Keep the existing spear use animation untouched so
            // the third-person pose remains unchanged.
            float windup = net.minecraft.util.Mth.clamp(progress / 0.66F, 0.0F, 1.0F);
            windup = windup * windup * (3.0F - 2.0F * windup);
            float strike = net.minecraft.util.Mth.clamp((progress - 0.76F) / 0.24F, 0.0F, 1.0F);
            strike = strike * strike * (3.0F - 2.0F * strike);

            net.minecraft.world.entity.HumanoidArm arm = mc.player.getMainArm();
            float side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            if (event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND) side = -side;

            // Pull the pickaxe inward and above the camera during charge, then make one
            // compact downward strike. The smaller offsets prevent the head and handle
            // from clipping through the camera in first person.
            event.getPoseStack().translate(
                -0.10D * side * windup + 0.05D * side * strike,
                -0.38D * windup + 0.70D * strike,
                -0.24D * windup + 0.12D * strike);
            event.getPoseStack().mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                -18.0F * side * windup + 10.0F * side * strike));
            event.getPoseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                -58.0F * windup + 112.0F * strike));
            event.getPoseStack().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(
                24.0F * side * windup - 36.0F * side * strike));
        }

        @SubscribeEvent
        public static void onFarSightScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || !mc.player.isUsingItem()) return;
            ItemStack use = mc.player.getUseItem();
            if (!use.is(net.minecraft.world.item.Items.SPYGLASS)
                || !SpecialUpgradeUtil.is(use, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS)) return;
            double delta = event.getScrollDeltaY();
            if (Math.abs(delta) < 1.0E-6D) return;
            double nextZoom = farSightZoom * (delta > 0.0D ? 0.90D : 1.10D);
            // 50%-500% zoom corresponds to an FOV multiplier from 2.0 down to 0.2.
            if (Double.isFinite(nextZoom)) farSightZoom = Math.max(0.20D, Math.min(2.0D, nextZoom));
            double zoomPercent = 100.0D / farSightZoom;
            String zoomText = zoomPercent < 1_000_000_000.0D
                ? String.format(java.util.Locale.ROOT, "%,.0f", zoomPercent)
                : String.format(java.util.Locale.ROOT, "%.3e", zoomPercent);
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§bFar-sight zoom: §f" + zoomText + "%"), true);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onInventoryRightClick(net.neoforged.neoforge.client.event.ScreenEvent.MouseButtonPressed.Pre event) {
            if (event.getButton() != org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
            if (!(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen screen)) return;
            net.minecraft.world.inventory.Slot slot = screen.getSlotUnderMouse();
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (slot == null || mc.player == null || slot.container != mc.player.getInventory()) return;
            ItemStack stack = slot.getItem();
            if (!InventoryUiFallback.canOpen(stack)) return;
            PacketDistributor.sendToServer(new com.velorise.emc.network.OpenInventoryItemUiPayload(slot.getContainerSlot()));
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            if (mc.screen instanceof EmcHudLayoutScreen) return;
            PlayerEMC data = mc.player.getData(VeloriseEMC.PLAYER_EMC.get());
            if (!data.isHudEnabled()) return;
            net.minecraft.client.gui.GuiGraphics graphics = event.getGuiGraphics();
            String balance = "EMC: " + String.format("%,d", data.getEmc());
            String rate = String.format("%+,.0f EMC/s", hudRate);
            String worldTierText = null;
            if (EmcHudPreferences.showWorldTier()) {
                int worldTier = ClientWorldProgress.current().worldTier();
                worldTierText = worldTier <= 0
                    ? "World Tier: 0"
                    : "World Tier: " + AlchemicalAnvilItem.roman(worldTier);
            }
            String targetLine = null;
            if (EmcHudPreferences.showLookedAtBlockEmc()
                && mc.level != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
                net.minecraft.world.level.block.state.BlockState lookedState = mc.level.getBlockState(hit.getBlockPos());
                ItemStack blockStack = new ItemStack(lookedState.getBlock().asItem());
                String blockName = net.minecraft.network.chat.Component
                    .translatable(lookedState.getBlock().getDescriptionId()).getString();
                long unitEmc = blockStack.isEmpty() ? 0L : EMCRegistry.getUnitEMC(blockStack);
                targetLine = unitEmc > 0L
                    ? blockName + ": " + String.format("%,d", unitEmc) + " EMC"
                    : blockName + ": No EMC";
            }
            EmcHudOverlay.render(graphics, mc.font,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                balance, rate, worldTierText, targetLine, hudRate >= 0.0D);
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
                System.out.println("[VeloriseEMC] RecipesUpdatedEvent – recalculating client EMC...");
                EMCRegistry.clientReloadAndRecalculate(event.getRecipeManager(), registries);
            } else {
                System.out.println("[VeloriseEMC] RecipesUpdatedEvent fired but no registries available – skipping.");
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
            com.velorise.emc.network.ClientPayloadHandler.clearJukeboxAudio();
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.getConnection() != null) {
                System.out.println("[VeloriseEMC] LevelEvent.Load (client) – recalculating client EMC...");
                EMCRegistry.clientReloadAndRecalculate(
                    mc.getConnection().getRecipeManager(),
                    mc.level.registryAccess()
                );
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            System.out.println("[VeloriseEMC] Logging out – clearing server-synced overrides.");
            com.velorise.emc.network.ClientPayloadHandler.clearJukeboxAudio();
            EMCRegistry.clearClientSyncedOverrides();
        }
    }
}
