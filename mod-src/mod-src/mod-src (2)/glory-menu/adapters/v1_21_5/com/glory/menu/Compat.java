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
import net.minecraft.client.render.RenderLayer;
import java.io.ByteArrayInputStream;

/** Minecraft 1.21.5. Everything that differs between Minecraft versions lives here. */
final class Compat {
    private Compat() { }
    static final boolean PANORAMA_EVERYWHERE = true;

    static Identifier id(String ns, String path) { return Identifier.of(ns, path); }

    static void drawTex(DrawContext c, Identifier id, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th, int argb) {
        c.drawTexture(RenderLayer::getGuiTextured, id, x, y, u, v, w, h, rw, rh, tw, th, argb);
    }

    static Identifier makeTex(String key, byte[] png) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        Identifier id = Identifier.of("glorymenu", key);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "glorymenu/" + key, img));
        return id;
    }

    static void connect(Screen s, String address) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(s, mc, ServerAddress.parse(address), info, false, null);
    }

    static void free(Identifier id) { MinecraftClient.getInstance().getTextureManager().destroyTexture(id); }
}
