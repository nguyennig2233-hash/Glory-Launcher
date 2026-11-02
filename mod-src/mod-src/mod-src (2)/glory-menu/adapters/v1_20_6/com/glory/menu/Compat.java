package com.glory.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.ByteArrayInputStream;

/** Minecraft 1.20.5 and 1.20.6. Everything that differs between Minecraft versions lives here. */
final class Compat {
    private Compat() { }
    static final boolean PANORAMA_EVERYWHERE = false;

    static Identifier id(String ns, String path) { return new Identifier(ns, path); }

    static void drawTex(DrawContext c, Identifier id, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        c.drawTexture(id, x, y, w, h, u, v, rw, rh, tw, th);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    static Identifier makeTex(String key, byte[] png) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        Identifier id = new Identifier("glorymenu", key);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        return id;
    }

    static void connect(Screen s, String address) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(s, mc, ServerAddress.parse(address), info, false, null);
    }

    static void free(Identifier id) { MinecraftClient.getInstance().getTextureManager().destroyTexture(id); }
}
