package com.glory.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Layout (v2) đọc từ config/glory-menu/layout.json do launcher ghi. Mọi trường đều được kiểm tra; file hỏng thì bỏ qua. */
public final class Layout {
    /** Nút gốc của game: chỉ lưu độ lệch so với vị trí gốc nên đúng ở mọi cỡ cửa sổ. */
    public static final class Vanilla {
        public boolean hide;
        public int dx, dy, w, h;      // w / h = 0 nghĩa là giữ nguyên
        public String label = "";
    }

    public static final class Element {
        public String id = "", type = "", name = "";
        public boolean visible = true;
        public int opacity = 100, fade, delay;
        public String ax = "center", ay = "top";
        public int x, y, w = 200, h = 20;
        // nút / ảnh
        public String label = "", action = "none", value = "", tooltip = "";
        // chữ
        public String text = "", color = "#ffffff", align = "left";
        public boolean shadow = true;
        public int size = 1;
        // ảnh (nhiều khung xếp dọc trong một file)
        public String file = "";
        public int iw, ih, frames = 1, fps = 10, hover = 100;

        public int left(int sw) { return base(ax, sw) + x; }
        public int top(int sh) { return base(ay, sh) + y; }
        private static int base(String a, int size) {
            switch (a) {
                case "left": case "top": return 0;
                case "right": case "bottom": return size;
                default: return size / 2;   // center / middle: nửa kích thước, làm tròn xuống (khớp launcher)
            }
        }
    }

    public final Map<String, Vanilla> vanilla = new HashMap<>();
    public final List<Element> elements = new ArrayList<>();

    static String str(JsonObject o, String k, String d) { JsonElement e = o.get(k); return e != null && e.isJsonPrimitive() ? e.getAsString() : d; }
    static int num(JsonObject o, String k, int d) { JsonElement e = o.get(k); try { return e != null && e.isJsonPrimitive() ? e.getAsInt() : d; } catch (Exception ex) { return d; } }
    static boolean bool(JsonObject o, String k, boolean d) { JsonElement e = o.get(k); try { return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : d; } catch (Exception ex) { return d; } }

    public static Layout load(Path file) {
        Layout L = new Layout();
        if (!Files.isRegularFile(file)) return L;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject t = root.getAsJsonObject("title");
            if (t == null) return L;
            JsonObject v = t.getAsJsonObject("vanilla");
            if (v != null) for (String k : v.keySet()) {
                if (!v.get(k).isJsonObject()) continue;
                JsonObject o = v.getAsJsonObject(k); Vanilla n = new Vanilla();
                n.hide = bool(o, "hide", false); n.dx = num(o, "dx", 0); n.dy = num(o, "dy", 0); n.w = Math.max(0, num(o, "w", 0)); n.h = Math.max(0, num(o, "h", 0)); n.label = str(o, "label", "");
                L.vanilla.put(k, n);
            }
            JsonArray a = t.getAsJsonArray("elements");
            if (a != null) for (JsonElement je : a) {
                if (!je.isJsonObject()) continue;
                JsonObject o = je.getAsJsonObject(); Element e = new Element();
                e.type = str(o, "type", ""); e.id = str(o, "id", ""); e.name = str(o, "name", "");
                e.visible = bool(o, "visible", true); e.opacity = Math.max(0, Math.min(100, num(o, "opacity", 100))); e.fade = Math.max(0, num(o, "fade", 0)); e.delay = Math.max(0, num(o, "delay", 0));
                e.ax = str(o, "ax", "center"); e.ay = str(o, "ay", "top"); e.x = num(o, "x", 0); e.y = num(o, "y", 0); e.w = Math.max(1, num(o, "w", 200)); e.h = Math.max(1, num(o, "h", 20));
                e.label = str(o, "label", ""); e.action = str(o, "action", "none"); e.value = str(o, "value", ""); e.tooltip = str(o, "tooltip", "");
                e.text = str(o, "text", ""); e.color = str(o, "color", "#ffffff"); e.align = str(o, "align", "left"); e.shadow = bool(o, "shadow", true); e.size = Math.max(1, Math.min(8, num(o, "size", 1)));
                e.file = str(o, "file", ""); e.iw = num(o, "iw", 0); e.ih = num(o, "ih", 0); e.frames = Math.max(1, num(o, "frames", 1)); e.fps = Math.max(1, num(o, "fps", 10)); e.hover = Math.max(100, num(o, "hover", 100));
                if (!e.visible) continue;
                if (e.type.equals("button") || e.type.equals("text") || e.type.equals("image")) L.elements.add(e);
            }
        } catch (Exception ex) {
            GloryMenuClient.LOG.warn("Cannot read menu layout: {}", ex.toString());
            return new Layout();
        }
        return L;
    }
}
