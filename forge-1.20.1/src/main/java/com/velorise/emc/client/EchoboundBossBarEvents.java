package com.velorise.emc.client;

import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.EchoboundClientEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ender-Sculk frame for the Echobound encounter; atlas row communicates phase without text. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EchoboundBossBarEvents {
    private static final ResourceLocation PROTECTED = new ResourceLocation(
        VeloriseEMC.MODID, "textures/gui/echobound_bossbar_protected.png");
    private static final ResourceLocation VULNERABLE = new ResourceLocation(
        VeloriseEMC.MODID, "textures/gui/echobound_bossbar_vulnerable.png");
    private static final String NAME_KEY = "entity.velorise_emc.echobound_dragon";
    private static final String INFERNAL_KEY = "entity.velorise_emc.infernal_crucible";
    private static final String DEEPBOUND_KEY = "entity.velorise_emc.deepbound_warden";
    private static final String AETHERIC_KEY = "entity.velorise_emc.aetheric_bulwark";
    private static final int BAR_WIDTH = 182;
    private static final int FRAME_WIDTH = 200;
    private static final int FRAME_HEIGHT = 16;

    private EchoboundBossBarEvents() {}

    @SubscribeEvent
    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents contents)) return;
        String key = contents.getKey();
        if (INFERNAL_KEY.equals(key)) {
            // Blaze Boss: Fiery Magma Gold & Red (Vanilla 182x5 thin bar)
            renderVanillaStyleBar(event, boss, 0xFFFFB34A, 0xFF180600, 0xFFD63000, 0xFFFF8A18, 0xFF441000);
            return;
        }
        if (DEEPBOUND_KEY.equals(key)) {
            // Warden Boss: Sculk Cyan & Void Teal (Vanilla 182x5 thin bar)
            renderVanillaStyleBar(event, boss, 0xFF7DFFF0, 0xFF021015, 0xFF087F78, 0xFF35E1C3, 0xFF03483F);
            return;
        }
        if (AETHERIC_KEY.equals(key)) {
            // Shulker Boss: Aetheric Purpur & Amethyst Magenta (Vanilla 182x5 thin bar)
            renderVanillaStyleBar(event, boss, 0xFFFF99FF, 0xFF14051C, 0xFF9E1BCE, 0xFFFF77FF, 0xFF520B6D);
            return;
        }
        if (!NAME_KEY.equals(key)) return;

        boolean protectedState = boss.getColor() == BossEvent.BossBarColor.BLUE;
        int phase = phase(boss.getProgress());
        int x = event.getX();
        int y = event.getY();
        int fill = Math.max(0, Math.min(BAR_WIDTH, (int)(boss.getProgress() * (BAR_WIDTH + 1))));
        var graphics = event.getGuiGraphics();

        graphics.fill(x, y, x + BAR_WIDTH, y + 5, 0xFF100B18);
        if (fill > 0) {
            int color = protectedState ? 0xFF087F9B : 0xFF65219A;
            int highlight = protectedState ? 0xFF23D5DB : 0xFFB14BE0;
            graphics.fill(x, y, x + fill, y + 5, color);
            graphics.fill(x, y, x + fill, y + 1, highlight);
        }
        graphics.blit(protectedState ? PROTECTED : VULNERABLE,
            x - 9, y - 5, 0, phase * FRAME_HEIGHT,
            FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT * 4);

        var font = Minecraft.getInstance().font;
        int titleX = event.getWindow().getGuiScaledWidth() / 2 - font.width(boss.getName()) / 2;
        graphics.drawString(font, boss.getName(), titleX, y - 9, 0xFFFFFF, false);
        event.setIncrement(22);
        event.setCanceled(true);
    }

    private static void renderVanillaStyleBar(
        CustomizeGuiOverlayEvent.BossEventProgress event,
        LerpingBossEvent boss,
        int nameColor,
        int bgDark,
        int fillMain,
        int fillTop,
        int notchColor
    ) {
        int smallWidth = 136;
        int x = event.getWindow().getGuiScaledWidth() / 2 - smallWidth / 2;
        int y = event.getY();
        int fill = Math.max(0, Math.min(smallWidth, (int)(boss.getProgress() * smallWidth)));
        var graphics = event.getGuiGraphics();

        // Compact Boss Bar dimensions: 136px wide, 4px high
        // Outer 1px shadow frame
        graphics.fill(x - 1, y - 1, x + smallWidth + 1, y + 5, 0xFF000000);
        // Background void
        graphics.fill(x, y, x + smallWidth, y + 4, bgDark);

        // Progress Bar Fill
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 4, fillMain);
            graphics.fill(x, y, x + fill, y + 1, fillTop);
        }

        // 10-Notch Segment Dividers
        for (int i = 1; i < 10; i++) {
            int sx = x + (i * smallWidth) / 10;
            graphics.fill(sx, y, sx + 1, y + 4, notchColor);
        }

        // Boss Title centered cleanly above bar
        drawBossTitle(event, boss, y, nameColor);

        event.setIncrement(17);
        event.setCanceled(true);
    }

    private static void drawBossTitle(CustomizeGuiOverlayEvent.BossEventProgress event,
                                      LerpingBossEvent boss, int y, int color) {
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int titleX = event.getWindow().getGuiScaledWidth() / 2 - font.width(boss.getName()) / 2;
        graphics.drawString(font, boss.getName(), titleX, y - 10, color, true);
    }

    private static int phase(float progress) {
        if (progress > 0.75F) return 0;
        if (progress > 0.50F) return 1;
        if (progress > 0.25F) return 2;
        return 3;
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        float strength = EchoboundClientEffects.intensity(event.getCamera().getPosition());
        if (strength <= 0.0F) return;
        double wave = System.nanoTime() / 13_000_000.0D;
        event.setYaw(event.getYaw() + (float)Math.sin(wave) * 2.4F * strength);
        event.setPitch(event.getPitch() + (float)Math.cos(wave * 1.17D) * 2.0F * strength);
        event.setRoll(event.getRoll() + (float)Math.sin(wave * 0.73D) * 1.35F * strength);
    }
}
