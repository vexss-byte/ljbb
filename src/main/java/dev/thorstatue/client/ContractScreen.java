package dev.thorstatue.client;

import dev.thorstatue.client.cutscene.CameraPath;
import dev.thorstatue.client.cutscene.CutsceneController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The "Patung Thor" contract panel that slides in from the right at ~5.6s (see reference clip), plus the
 * tabbed follow-up view (Home / Skills / Weapon / Offering / Followers) from your second screenshot.
 * The world keeps rendering (and the cutscene camera keeps running) behind it; Esc closes it and ends the cutscene.
 * Contract/weapon gameplay (forging, skills, Faith, seats) is NOT implemented - this is UI only.
 */
public class ContractScreen extends Screen {
    private static final int PANEL_W = 236;
    private static final String[] TABS = {"Home", "Skills", "Weapon", "Offering", "Followers"};

    private final long openedNs = System.nanoTime();
    private boolean signed = false;
    private int tab = 0;
    private Button signButton;
    private final Button[] tabButtons = new Button[TABS.length];

    public ContractScreen() { super(Component.translatable("thorstatue.panel.title")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        super.onClose();
        CutsceneController.end();
    }

    private double slide() {
        double t = (System.nanoTime() - openedNs) / 1.0e9;
        return 1 - CameraPath.smooth(0, 0.55, t); // 1 = off-screen, 0 = docked
    }

    private int panelX() { return (int) (width - PANEL_W - 10 + slide() * (PANEL_W + 40)); }
    private int panelH() { return Math.min(height - 20, 292); }

    @Override
    protected void init() {
        int x = width - PANEL_W - 10, y = 10 + panelH() - 62;
        signButton = addRenderableWidget(Button.builder(Component.translatable("thorstatue.panel.sign"), b -> {
            signed = true; tab = 0; rebuildTabs();
        }).bounds(x + 8, y, PANEL_W - 16, 20).build());
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int tw = (PANEL_W - 16) / TABS.length;
            tabButtons[i] = addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> tab = idx)
                .bounds(x + 8 + i * tw, 14, tw, 16).build());
        }
        rebuildTabs();
    }

    private void rebuildTabs() {
        signButton.visible = !signed;
        for (Button b : tabButtons) b.visible = signed;
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) { /* keep the world visible */ }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        int off = panelX() - (width - PANEL_W - 10); // slide offset applied to every drawn element
        int x = width - PANEL_W - 10 + off, y = 10, h = panelH();
        // panel body + border
        g.fill(x, y, x + PANEL_W, y + h, 0xE00B1424);
        g.outline(x, y, PANEL_W, h, 0xFF24456E);
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, 0x6648A0FF);

        // widgets follow the slide
        signButton.setX(x + 8);
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].setX(x + 8 + i * ((PANEL_W - 16) / TABS.length));

        int tx = x + 8, ty = signed ? 36 : y + 6, w = PANEL_W - 16;
        if (!signed) drawContract(g, tx, ty, w, y + h);
        else if (tab == 2) drawWeapon(g, tx, ty, w, y + h);
        else drawPlaceholder(g, tx, ty);

        g.text(font, "Esc: tutup", x + 8, y + h - 12, 0xFF8FA4C4);
        super.extractRenderState(g, mx, my, a); // widgets
    }

    private int wrap(GuiGraphicsExtractor g, String s, int x, int y, int w, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(s), w);
        for (FormattedCharSequence l : lines) { g.text(font, l, x, y, color); y += 10; }
        return y;
    }

    private void drawContract(GuiGraphicsExtractor g, int x, int y, int w, int bottom) {
        g.text(font, "Patung Thor", x, y, 0xFFFFFFFF);
        y += 16;
        y = wrap(g, "Kontrak = kamu pinjam kekuatan Thor. Kamu cuma bisa ikut 1 dewa.", x, y, w, 0xFFB8C6DD) + 6;
        g.text(font, "Yang kamu dapat", x + 2, y, 0xFF7CE0A0); y += 14;
        String[] gets = {"Weapon takdir dan 3 skill pertama", "3 skill lagi saat Faith naik", "Special Magic di Faith 10"};
        for (int i = 0; i < gets.length; i++) {
            icon(g, x + 2, y, i);
            g.text(font, gets[i], x + 22, y + 1, 0xFFE6EDF8);
            y += 16;
        }
        y += 8;
        g.text(font, "Harganya", x + 2, y, 0xFFFF7A7A); y += 12;
        wrap(g, "Armor kamu tinggal setengah saat Battle Mode.", x, y, w, 0xFFE6EDF8);
        int by = bottom - 62;
        g.text(font, "Belum ada Saint, jadi kontrak langsung aktif.", x, by + 26, 0xFFB8C6DD);
        g.text(font, "0 dari 7 kursi terisi", x, by + 36, 0xFF8FA4C4);
    }

    /** tiny pixel icons standing in for the three skill glyphs of the reference panel */
    private void icon(GuiGraphicsExtractor g, int x, int y, int kind) {
        int c = 0xFFD9E4F5;
        switch (kind) {
            case 0 -> { g.fill(x + 6, y + 1, x + 8, y + 12, c); g.fill(x + 2, y + 4, x + 12, y + 6, c); }
            case 1 -> { g.fill(x + 2, y + 6, x + 12, y + 8, c); g.fill(x + 5, y + 2, x + 7, y + 12, c); g.fill(x + 8, y + 3, x + 10, y + 11, c); }
            default -> { g.fill(x + 3, y + 1, x + 11, y + 3, c); g.fill(x + 6, y + 3, x + 8, y + 9, c); g.fill(x + 2, y + 9, x + 12, y + 12, c); }
        }
    }

    private void drawWeapon(GuiGraphicsExtractor g, int x, int y, int w, int bottom) {
        g.text(font, "Weapon", x, y, 0xFFFFFFFF); y += 12;
        y = wrap(g, "Kamu belum punya Weapon. Tanpa Weapon kamu tidak bisa masuk Battle Mode.", x, y, w, 0xFFB8C6DD) + 4;
        String[] names = {"Gnyr", "Hein", "Tanngnjóstr", "Hymir's Hook"};
        int sw = (w - 9) / 4;
        for (int i = 0; i < 4; i++) {
            int sx = x + i * (sw + 3);
            g.fill(sx, y, sx + sw, y + 52, 0xFF121E33);
            g.outline(sx, y, sw, 52, 0xFF2A3D5C);
            g.text(font, names[i], sx + 3, y + 40, 0xFF8FA4C4);
        }
        y += 60;
        y = wrap(g, "Takdir yang memilih satu dari empat ini. Kamu tidak bisa memilih.", x, y, w, 0xFFB8C6DD) + 4;
        g.text(font, "Bahan:", x, y, 0xFFFFD27A); y += 11;
        g.text(font, "✓ 16 iron", x, y, 0xFF9BE89B); y += 10;
        g.text(font, "✓ 2 lightning rod", x, y, 0xFF9BE89B);
        g.fill(x, bottom - 40, x + w, bottom - 20, 0xFF1F5FA8);
        g.centeredText(font, "Forge Weapon", x + w / 2, bottom - 34, 0xFFFFFFFF);
    }

    private void drawPlaceholder(GuiGraphicsExtractor g, int x, int y) {
        g.text(font, TABS[tab], x, y, 0xFFFFFFFF);
        g.text(font, "(belum tersedia)", x, y + 14, 0xFF8FA4C4);
    }
}
