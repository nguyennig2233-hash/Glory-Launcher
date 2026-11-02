package com.glory.menu;

import com.glory.menu.mixin.ScreenAccessor;
import com.glory.menu.mixin.WidgetAccessor;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Util;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Glory Menu v3 (nhiều phiên bản Minecraft, phần khác nhau nằm trong Compat.java của từng adapter): đọc layout do launcher thiết kế và áp lên màn hình chính (TitleScreen).
 *  - Nút gốc: dời theo độ lệch / đổi cỡ / đổi chữ / ẩn, tìm theo khoá dịch (menu.singleplayer, ...).
 *  - Nút thêm: ButtonWidget thật (đúng texture của gói đang dùng, hover, âm thanh), có tooltip, hiện dần.
 *  - Chữ (có cỡ, căn lề) và ảnh (nhiều khung = ảnh động, phóng khi rê chuột, bấm được): vẽ ở afterRender.
 * Layout đọc lại mỗi lần mở màn hình chính nên sửa trong launcher rồi vào game lại là thấy.
 *  - Hình nền (ảnh hoặc ảnh động) do launcher ghi ở config/glory-menu/bg: vẽ trước mọi thứ; panorama gốc được gói GloryWall.zip làm trong suốt nên hình nền lộ ra.
 */
public final class GloryMenuClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("glory-menu");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("glory-menu").resolve("layout.json");
    private static final Path BG = FabricLoader.getInstance().getConfigDir().resolve("glory-menu").resolve("bg");
    private static Layout layout = new Layout();
    private static Wall wall = new Wall();
    private static long startMs;
    private static final List<Object[]> widgets = new ArrayList<>();   // {ButtonWidget, Layout.Element}

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            reloadWall();
            if (wall.on && (screen instanceof TitleScreen || (Compat.PANORAMA_EVERYWHERE && client.world == null)))
                ScreenEvents.beforeRender(screen).register((s, ctx, mx, my, delta) -> drawWall(s, ctx));
            if (!(screen instanceof TitleScreen)) return;
            layout = Layout.load(FILE);
            if (startMs == 0) startMs = System.currentTimeMillis();   // hiện dần tính từ lần mở menu đầu tiên
            widgets.clear();
            applyVanilla(screen);
            addButtons(screen);
            if (layout.elements.isEmpty()) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> drawElements(s, ctx, mx, my));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, btn) -> !clickImages(s, mx, my, btn));
        });
    }

    // ---------- hình nền ----------
    private static void reloadWall() {
        if (wall.sameAs(BG)) return;
        for (Object o : wall.tex) if (o != null) { try { Compat.free((Identifier) o); } catch (Exception ignored) { } }
        wall = Wall.load(BG);
        if (wall.on) LOG.info("Wallpaper: {} frames {}x{}", wall.delays.length, wall.w, wall.h);
    }

    private static void drawWall(Screen s, DrawContext ctx) {
        Wall k = wall;
        if (!k.on) return;
        int i = k.frameAt(System.currentTimeMillis() - k.t0);
        if (k.tex[i] == null) {
            byte[] png = k.frameBytes(i);
            if (png == null) { k.on = false; return; }
            try { k.tex[i] = Compat.makeTex("wall/f" + i, png); } catch (Exception ex) { LOG.warn("Wallpaper frame {} failed: {}", i, ex.toString()); k.on = false; return; }
        }
        ctx.fill(0, 0, s.width, s.height, 0xFF000000);
        int[] r = k.rect(s.width, s.height);
        Compat.drawTex(ctx, (Identifier) k.tex[i], r[0], r[1], r[2], r[3], 0f, 0f, k.w, k.h, k.w, k.h, 0xFFFFFFFF);
        if (k.dim > 0) ctx.fill(0, 0, s.width, s.height, ((k.dim * 255 / 100) << 24));
    }

    // ---------- nút gốc ----------
    private static String keyOf(ClickableWidget w) {
        TextContent c = w.getMessage().getContent();
        return c instanceof TranslatableTextContent t ? t.getKey() : "";
    }

    private static void applyVanilla(Screen screen) {
        if (layout.vanilla.isEmpty()) return;
        for (ClickableWidget w : Screens.getButtons(screen)) {
            Layout.Vanilla v = layout.vanilla.get(keyOf(w));
            if (v == null) continue;
            if (v.hide) { w.visible = false; w.active = false; continue; }
            w.setX(w.getX() + v.dx); w.setY(w.getY() + v.dy);
            if (v.w > 0) w.setWidth(v.w);
            if (v.h > 0) ((WidgetAccessor) w).glory$setHeight(v.h);
            if (!v.label.isEmpty()) w.setMessage(Text.literal(v.label));
        }
    }

    // ---------- nút thêm (nút thật) ----------
    private static void addButtons(Screen screen) {
        for (Layout.Element e : layout.elements) {
            if (!e.type.equals("button")) continue;
            ButtonWidget.Builder b = ButtonWidget.builder(Text.literal(e.label), btn -> run(e.action, e.value, screen))
                    .dimensions(e.left(screen.width), e.top(screen.height), e.w, e.h);
            if (!e.tooltip.isEmpty()) b.tooltip(Tooltip.of(Text.literal(e.tooltip)));
            ButtonWidget w = ((ScreenAccessor) screen).glory$addDrawableChild(b.build());
            w.setAlpha(alpha(e));
            widgets.add(new Object[]{w, e});
        }
    }

    /** 0..1 gồm cả độ đậm, độ trễ và hiện dần. */
    private static float alpha(Layout.Element e) {
        long t = System.currentTimeMillis() - startMs - e.delay;
        float f = t < 0 ? 0f : e.fade <= 0 ? 1f : Math.min(1f, t / (float) e.fade);
        return f * (e.opacity / 100f);
    }

    // ---------- chữ + ảnh ----------
    private static int rgb(String hex) {
        try { return 0xFF000000 | Integer.parseInt(hex.substring(1), 16); } catch (Exception e) { return 0xFFFFFFFF; }
    }

    private static boolean inside(Layout.Element e, Screen s, double mx, double my) {
        int l = e.left(s.width), t = e.top(s.height);
        return mx >= l && mx < l + e.w && my >= t && my < t + e.h;
    }

    private static void drawElements(Screen s, DrawContext ctx, int mx, int my) {
        for (Object[] p : widgets) ((ButtonWidget) p[0]).setAlpha(alpha((Layout.Element) p[1]));
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        String tip = null;
        for (Layout.Element e : layout.elements) {
            float a = alpha(e);
            if (a <= 0.003f || e.type.equals("button")) continue;
            int l = e.left(s.width), t = e.top(s.height), argb = ((int) (a * 255) << 24) | 0xFFFFFF;
            if (e.type.equals("image")) {
                boolean hot = inside(e, s, mx, my);
                if (hot && !e.tooltip.isEmpty()) tip = e.tooltip;
                try {
                    Identifier id = Compat.id("glory", "textures/menu/" + e.file);
                    int fh = Math.max(1, e.ih / Math.max(1, e.frames)), iw = Math.max(1, e.iw), ih = Math.max(1, e.ih);
                    int frame = e.frames > 1 ? (int) ((System.currentTimeMillis() * e.fps / 1000L) % e.frames) : 0;
                    float k = hot ? e.hover / 100f : 1f;
                    int w = Math.round(e.w * k), h = Math.round(e.h * k), x = l - (w - e.w) / 2, y = t - (h - e.h) / 2;
                    Compat.drawTex(ctx, id, x, y, w, h, 0f, (float) (frame * fh), iw, fh, iw, ih, argb);
                } catch (Exception ex) { /* ảnh lỗi thì bỏ qua, không làm sập menu */ }
            } else if (e.type.equals("text")) {
                int c = (rgb(e.color) & 0x00FFFFFF) | ((int) (a * 255) << 24);
                float sc = e.size;
                int tw = Math.round(font.getWidth(e.text) * sc);
                int ox = e.align.equals("center") ? (e.w - tw) / 2 : e.align.equals("right") ? e.w - tw : 0;
                var m = ctx.getMatrices();
                m.push(); m.translate(l + ox, t + Math.max(0f, (e.h - 8 * sc) / 2f), 0f); m.scale(sc, sc, 1f);
                ctx.drawText(font, Text.literal(e.text), 0, 0, c, e.shadow);
                m.pop();
            }
        }
        if (tip != null) ctx.drawTooltip(font, Text.literal(tip), mx, my);
    }

    /** true nếu đã xử lý cú bấm trên một ảnh có hành động (khi đó game không chuyển cú bấm cho nút gốc nữa). */
    private static boolean clickImages(Screen s, double mx, double my, int btn) {
        if (btn != 0) return false;
        for (int i = layout.elements.size() - 1; i >= 0; i--) {
            Layout.Element e = layout.elements.get(i);
            if (!e.type.equals("image") || e.action.equals("none") || alpha(e) <= 0.05f || !inside(e, s, mx, my)) continue;
            run(e.action, e.value, s);
            return true;
        }
        return false;
    }

    private static void run(String action, String value, Screen s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            switch (action) {
                case "singleplayer" -> mc.setScreen(new SelectWorldScreen(s));
                case "multiplayer" -> mc.setScreen(new MultiplayerScreen(s));
                case "options" -> mc.setScreen(new OptionsScreen(s, mc.options));
                case "quit" -> mc.scheduleStop();
                case "url" -> {
                    if (value.startsWith("https://") || value.startsWith("http://"))
                        mc.setScreen(new ConfirmLinkScreen((BooleanConsumer) ok -> { if (ok) Util.getOperatingSystem().open(URI.create(value)); mc.setScreen(s); }, value, true));
                }
                case "server" -> { if (!value.isEmpty()) Compat.connect(s, value); }
                default -> { }
            }
        } catch (Exception ex) { LOG.warn("Menu action {} failed: {}", action, ex.toString()); }
    }
}
