package com.glory.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The menu wallpaper (still or animated), written by the launcher into config/glory-menu/bg/: bg.json + f0.png, f1.png, ...
 * No Minecraft code here. bg.json: {"on":true,"w":854,"h":480,"fit":"cover|contain|stretch","dim":0-80,"speed":25-400,"delays":[ms,...]}
 */
public final class Wall {
    public boolean on;
    public int w, h, dim, speed = 100;
    public String fit = "cover";
    public int[] delays = new int[0];
    public Object[] tex = new Object[0];      // Identifier per frame, filled lazily by the client
    public long t0 = System.currentTimeMillis();
    private long total;
    private Path dir;
    private long stamp = -1;

    public static long stampOf(Path dir) {
        try { return Files.getLastModifiedTime(dir.resolve("bg.json")).toMillis(); } catch (IOException e) { return 0; }
    }

    public boolean sameAs(Path dir) { return this.dir != null && this.dir.equals(dir) && stamp == stampOf(dir); }

    public static Wall load(Path dir) {
        Wall k = new Wall(); k.dir = dir; k.stamp = stampOf(dir);
        try {
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(dir.resolve("bg.json")), StandardCharsets.UTF_8)).getAsJsonObject();
            k.on = o.has("on") && o.get("on").getAsBoolean();
            k.w = Math.max(1, Math.min(4096, o.get("w").getAsInt())); k.h = Math.max(1, Math.min(4096, o.get("h").getAsInt()));
            if (o.has("fit")) k.fit = o.get("fit").getAsString();
            if (o.has("dim")) k.dim = Math.max(0, Math.min(90, o.get("dim").getAsInt()));
            if (o.has("speed")) k.speed = Math.max(10, Math.min(500, o.get("speed").getAsInt()));
            JsonArray d = o.getAsJsonArray("delays");
            k.delays = new int[Math.min(240, d.size())];
            for (int i = 0; i < k.delays.length; i++) { k.delays[i] = Math.max(20, Math.min(10000, d.get(i).getAsInt())); k.total += k.delays[i]; }
            if (k.delays.length == 0) k.on = false;
            k.tex = new Object[k.delays.length];
        } catch (Exception e) { k.on = false; }
        return k;
    }

    /** Which frame shows after `ms` milliseconds of playing (loops). */
    public int frameAt(long ms) {
        if (delays.length <= 1 || total <= 0) return 0;
        long t = (ms * speed / 100L) % total;
        for (int i = 0; i < delays.length; i++) { if (t < delays[i]) return i; t -= delays[i]; }
        return delays.length - 1;
    }

    public byte[] frameBytes(int i) {
        try { return Files.readAllBytes(dir.resolve("f" + i + ".png")); } catch (IOException e) { return null; }
    }

    /** Destination rectangle {x, y, w, h} for a screen of sw x sh (GUI units). */
    public int[] rect(int sw, int sh) {
        if (fit.equals("stretch")) return new int[]{0, 0, sw, sh};
        double k = fit.equals("contain") ? Math.min(sw / (double) w, sh / (double) h) : Math.max(sw / (double) w, sh / (double) h);
        int dw = (int) Math.ceil(w * k), dh = (int) Math.ceil(h * k);
        return new int[]{(sw - dw) / 2, (sh - dh) / 2, dw, dh};
    }
}
