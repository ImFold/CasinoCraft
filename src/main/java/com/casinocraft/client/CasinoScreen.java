package com.casinocraft.client;

import com.casinocraft.CasinoLayout;
import com.casinocraft.economy.PriceSnapshot;
import com.casinocraft.game.Roulette;
import com.casinocraft.game.RouletteBet;
import com.casinocraft.game.SlotSymbol;
import com.casinocraft.game.SlotMachine;
import com.casinocraft.network.ModNetworking;
import com.casinocraft.screen.CasinoScreenHandler;
import com.casinocraft.util.Money;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Интерфейс казино. Вся графика рисуется кодом — текстуры не нужны,
 * поэтому мод нормально выглядит с любым ресурспаком.
 */
public class CasinoScreen extends HandledScreen<CasinoScreenHandler> {
    private enum Tab {
        SLOTS, ROULETTE, SHOP
    }

    private static final int[] BET_STEPS = {10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 50000};

    private static final int C_PANEL = 0xFF121218;
    private static final int C_PANEL_EDGE = 0xFF404058;
    private static final int C_INNER = 0xFF181822;
    private static final int C_INNER_EDGE = 0xFF34344A;
    private static final int C_TEXT = 0xFFEDEDF2;
    private static final int C_DIM = 0xFF9A9AAE;
    private static final int C_GOLD = 0xFFFFC94A;
    private static final int C_GREEN = 0xFF57E07A;
    private static final int C_RED = 0xFFFF5B5B;
    private static final int C_BTN = 0xFF23232F;
    private static final int C_BTN_HOVER = 0xFF323244;
    private static final int C_BTN_EDGE = 0xFF4A4A60;

    private static final int TABS_Y = 30;
    private static final int TAB_H = 16;
    private static final int TAB_W = 58;
    private static final int SLOT_TAB_X = 155;
    private static final int ROULETTE_TAB_X = 215;
    private static final int SHOP_TAB_X = 275;

    private static final int BET_Y = 50;
    private static final int BET_BTN_W = 18;
    private static final int BET_BTN_H = 14;
    private static final int BET_MINUS_X = 158;
    private static final int BET_PLUS_X = 310;
    private static final int CONTENT_CENTER = 243;

    private static final int SELL_X = 14;
    private static final int SELL_Y = 94;
    private static final int SELL_W = 126;
    private static final int SELL_H = 16;

    private static final int WITHDRAW_X = 14;
    private static final int WITHDRAW_Y = 112;
    private static final int WITHDRAW_W = 126;
    private static final int WITHDRAW_H = 16;

    private static final int REEL_Y = 65;
    private static final int CELL_W = 32;
    private static final int CELL_H = 16;
    private static final int[] REEL_X = {158, 192, 226, 260, 294};

    private static final int SPIN_X = 158;
    private static final int SPIN_Y = 132;
    private static final int SPIN_W = 170;
    private static final int SPIN_H = 14;

    private static final int ROULETTE_STRIP_Y = 68;
    private static final int ROULETTE_CELL_W = 33;
    private static final int ROULETTE_CELL_STEP = 34;
    private static final int ROULETTE_BETS_Y = 102;

    private static final int SHOP_X = 156;
    private static final int SHOP_Y = 52;
    private static final int SHOP_CELL_W = 38;
    private static final int SHOP_CELL_H = 38;
    private static final int SHOP_STEP_X = 44;
    private static final int SHOP_STEP_Y = 42;
    private static final int SHOP_COLUMNS = 4;
    private static final int SHOP_ROWS = 2;
    private static final int SHOP_SCROLL_X = 329;
    private static final int SHOP_SCROLL_H = 80;
    private static final int SHOP_SEARCH_X = 156;
    private static final int SHOP_SEARCH_Y = 136;
    private static final int SHOP_SEARCH_W = 130;
    private static final int SHOP_SEARCH_H = 14;

    private Tab tab = Tab.SLOTS;
    private int betIndex = 3;
    private int shopRow;
    private String shopQuery = "";
    private TextFieldWidget searchField;
    private List<Item> sortedCatalog;
    private List<Item> filteredCatalog;
    private Map<Item, String> nameCache;
    private boolean filterDirty = true;
    private PriceSnapshot catalogPrices;

    private long shownBalance;
    private double localMouseX;
    private double localMouseY;

    private final SlotSymbol[] reels = new SlotSymbol[SlotMachine.CELL_COUNT];
    private List<SlotMachine.LineWin> lastLines = List.of();
    private boolean showPaytable;
    private boolean spinning;
    private long spinStart;
    private final long[] reelStopAt = new long[SlotMachine.REELS];
    private final double[] reelStartPosition = new double[SlotMachine.REELS];
    private final int[] reelEndStep = new int[SlotMachine.REELS];
    private long reelMotionStart;
    private SlotSymbol[] pendingReels;
    private long pendingPayout;
    private long pendingBalance;
    private long lastPayout;
    private long lastStake;
    private long resultFlashUntil;
    private long slotRevealAt = -1L;
    private boolean lastJackpot;
    private final CasinoVfx slotEffects = new CasinoVfx();

    private RouletteBet rouletteBet = RouletteBet.RED;
    private boolean rouletteSpinning;
    private long rouletteStart;
    private int rouletteTarget = -1;
    private long roulettePayout;
    private long rouletteStake;
    private long rouletteBalance;
    private long rouletteFlashUntil;
    private long rouletteRevealAt = -1L;
    private final CasinoVfx rouletteEffects = new CasinoVfx();
    private final BigWinAnimation bigWin = new BigWinAnimation();
    private Tab bigWinTab = Tab.SLOTS;

    private String noticeKey = "";
    private long noticeAmount;
    private long noticeUntil;
    private boolean noticePositive;

    public CasinoScreen(CasinoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = CasinoLayout.WIDTH;
        this.backgroundHeight = CasinoLayout.HEIGHT;
        for (int i = 0; i < reels.length; i++) {
            reels[i] = SlotSymbol.values()[(i + i / SlotMachine.ROWS) % 6];
        }
    }

    public static CasinoScreen current() {
        return MinecraftClient.getInstance().currentScreen instanceof CasinoScreen screen ? screen : null;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
        this.shownBalance = this.handler.getBalance();

        this.searchField = new TextFieldWidget(textRenderer, this.x + SHOP_SEARCH_X, this.y + SHOP_SEARCH_Y,
                SHOP_SEARCH_W, SHOP_SEARCH_H, Text.translatable("gui.casinocraft.shop.search"));
        this.searchField.setMaxLength(32);
        this.searchField.setText(shopQuery);
        this.searchField.setChangedListener(value -> {
            shopQuery = value;
            filterDirty = true;
        });
        this.filterDirty = true;
    }

    // ------------------------------------------------------------- рендер

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.localMouseX = mouseX - this.x;
        this.localMouseY = mouseY - this.y;
        updateAnimations(Util.getMeasuringTimeMs());
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate((float) this.x, (float) this.y, 0.0F);

        box(context, 0, 0, CasinoLayout.WIDTH, CasinoLayout.HEIGHT, C_PANEL, C_PANEL_EDGE);
        context.fill(6, 22, CasinoLayout.WIDTH - 6, 23, 0xFF2E2E40);
        box(context, CasinoLayout.LEFT_X, CasinoLayout.LEFT_Y, CasinoLayout.LEFT_W, CasinoLayout.LEFT_H, C_INNER, C_INNER_EDGE);
        box(context, CasinoLayout.RIGHT_X, CasinoLayout.RIGHT_Y, CasinoLayout.RIGHT_W, CasinoLayout.RIGHT_H, C_INNER, C_INNER_EDGE);

        for (Slot slot : this.handler.slots) {
            slotFrame(context, slot.x, slot.y);
        }

        matrices.pop();
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        long now = Util.getMeasuringTimeMs();

        context.drawTextWithShadow(textRenderer, Text.literal("КАЗИНО"), 10, 8, C_GOLD);
        Text balanceText = Text.translatable("gui.casinocraft.balance", Money.format(shownBalance));
        context.drawTextWithShadow(textRenderer, balanceText,
                CasinoLayout.WIDTH - 10 - textRenderer.getWidth(balanceText), 8, C_GOLD);

        drawCashier(context);
        drawTabs(context);

        long bet = currentBet();
        if (tab != Tab.SHOP) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable(tab == Tab.SLOTS ? "gui.casinocraft.total_bet" : "gui.casinocraft.bet", Money.format(bet)),
                    CONTENT_CENTER, BET_Y + 3, bet <= shownBalance ? C_TEXT : C_RED);
            drawButton(context, BET_MINUS_X, BET_Y, BET_BTN_W, BET_BTN_H, Text.literal("<"), false,
                    betIndex > 0 && !spinning && !rouletteSpinning, C_BTN);
            drawButton(context, BET_PLUS_X, BET_Y, BET_BTN_W, BET_BTN_H, Text.literal(">"), false,
                    betIndex < BET_STEPS.length - 1 && !spinning && !rouletteSpinning, C_BTN);
        }

        if (tab == Tab.SLOTS) {
            drawSlots(context, now, bet);
        } else if (tab == Tab.ROULETTE) {
            drawRoulette(context, now, bet);
        } else {
            drawShop(context);
        }

        if (tab == bigWinTab && bigWin.active(now) && (tab != Tab.SLOTS || !showPaytable)) {
            drawBigWin(context, now);
        }
        drawNotice(context, now);

        if (tab == Tab.SHOP && searchField != null) {
            searchField.setX(this.x + SHOP_SEARCH_X);
            searchField.setY(this.y + SHOP_SEARCH_Y);
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate((float) -this.x, (float) -this.y, 0.0F);
            searchField.render(context, mouseX, mouseY, 0.0F);
            matrices.pop();
            if (searchField.getText().isEmpty()) {
                context.drawTextWithShadow(textRenderer, Text.translatable("gui.casinocraft.shop.search"),
                        SHOP_SEARCH_X + 5, SHOP_SEARCH_Y + 3, 0xFF77778A);
            }
        }
    }

    private void drawCashier(DrawContext context) {
        context.drawTextWithShadow(textRenderer, Text.translatable("gui.casinocraft.cashier"), 12, 29, C_GOLD);

        long preview = depositPreview();
        context.drawTextWithShadow(textRenderer,
                Text.translatable("gui.casinocraft.to_sell", Money.format(preview)),
                12, 84, preview > 0 ? C_TEXT : C_DIM);

        drawButton(context, SELL_X, SELL_Y, SELL_W, SELL_H,
                Text.translatable("gui.casinocraft.sell"), false, preview > 0, C_BTN);

        drawButton(context, WITHDRAW_X, WITHDRAW_Y, WITHDRAW_W, WITHDRAW_H,
                Text.translatable("gui.casinocraft.withdraw"), false, shownBalance >= 10, C_BTN);

        context.drawTextWithShadow(textRenderer,
                Text.translatable(tab == Tab.SHOP ? "gui.casinocraft.shop.hint" : "gui.casinocraft.hint"),
                12, 134, C_DIM);
    }

    private void drawTabs(DrawContext context) {
        drawButton(context, SLOT_TAB_X, TABS_Y, TAB_W, TAB_H,
                Text.translatable("gui.casinocraft.tab.slots"), tab == Tab.SLOTS, true, C_BTN);
        drawButton(context, ROULETTE_TAB_X, TABS_Y, TAB_W, TAB_H,
                Text.translatable("gui.casinocraft.tab.roulette"), tab == Tab.ROULETTE, true, C_BTN);
        drawButton(context, SHOP_TAB_X, TABS_Y, TAB_W, TAB_H,
                Text.translatable("gui.casinocraft.tab.shop"), tab == Tab.SHOP, true, C_BTN);
    }

    private void drawSlots(DrawContext context, long now, long bet) {
        for (int reel = 0; reel < SlotMachine.REELS; reel++) {
            int x = REEL_X[reel];
            box(context, x, REEL_Y, CELL_W, CELL_H * SlotMachine.ROWS, 0xFF0B0B12, 0xFF3A3A50);
            boolean moving = spinning && (pendingReels == null || now < reelStopAt[reel]);
            double position = pendingReels == null ? (now - spinStart) / 55.0
                    : ReelMotion.position(now, reelMotionStart, reelStopAt[reel],
                    reelStartPosition[reel], reelEndStep[reel]);
            if (moving) {
                drawReelTrail(context, x, now - spinStart + reel * 140L);
            }
            context.enableScissor(this.x + x + 1, this.y + REEL_Y + 1,
                    this.x + x + CELL_W - 1, this.y + REEL_Y + CELL_H * SlotMachine.ROWS - 1);
            try {
                int step = (int) Math.floor(position);
                double fraction = position - step;
                for (int row = -1; row <= SlotMachine.ROWS; row++) {
                    SlotSymbol symbol;
                    double y;
                    if (moving) {
                        int virtual = step + row;
                        int targetRow = virtual - reelEndStep[reel];
                        symbol = pendingReels != null && targetRow >= 0 && targetRow < SlotMachine.ROWS
                                ? pendingReels[reel * SlotMachine.ROWS + targetRow]
                                : SlotSymbol.values()[Math.floorMod(virtual + reel * 3, SlotSymbol.values().length)];
                        y = REEL_Y + (row - fraction) * CELL_H;
                    } else {
                        if (row < 0 || row >= SlotMachine.ROWS) {
                            continue;
                        }
                        symbol = reels[reel * SlotMachine.ROWS + row];
                        y = REEL_Y + row * CELL_H;
                    }
                    drawSlotSymbol(context, symbol, x + CELL_W / 2.0, y + 3.0, 1.2f);
                }
            } finally {
                context.disableScissor();
            }
            for (int row = 1; row < SlotMachine.ROWS; row++) {
                context.fill(x + 1, REEL_Y + row * CELL_H, x + CELL_W - 1,
                        REEL_Y + row * CELL_H + 1, 0x553A3A50);
            }
            float landing = spinning && pendingReels != null
                    ? CasinoVfx.envelope(now, reelStopAt[reel], 240) : 0.0f;
            CasinoVfx.glow(context::fill, x, REEL_Y, CELL_W, CELL_H * SlotMachine.ROWS, C_GOLD, landing);
        }
        if (!spinning && now < resultFlashUntil && !lastLines.isEmpty()) {
            int index = (int) ((now - slotRevealAt) / 850L % lastLines.size());
            SlotMachine.LineWin win = lastLines.get(index);
            for (int reel = 0; reel < SlotMachine.REELS; reel++) {
                int row = SlotMachine.rowOnLine(win.line(), reel);
                if (reel + 1 < SlotMachine.REELS) {
                    drawPaylineSegment(context, REEL_X[reel] + CELL_W / 2, REEL_Y + row * CELL_H + 13,
                            REEL_X[reel + 1] + CELL_W / 2,
                            REEL_Y + SlotMachine.rowOnLine(win.line(), reel + 1) * CELL_H + 13);
                }
                if (reel < win.matches()) {
                    CasinoVfx.glow(context::fill, REEL_X[reel] + 1, REEL_Y + row * CELL_H + 1,
                            CELL_W - 2, CELL_H - 2, C_GOLD, 0.65f);
                }
            }
        }
        if (effectParticleCount(1) > 0) {
            slotEffects.render(context::fill, now, 155, 65, 333, 115);
        }
        Text result = Text.translatable("gui.casinocraft.paytable_hint");
        int color = C_DIM;
        if (spinning) {
            result = Text.translatable("gui.casinocraft.spinning");
        } else if (now < resultFlashUntil) {
            result = lastPayout == 0 ? Text.translatable("gui.casinocraft.lose")
                    : Text.translatable(lastJackpot ? "gui.casinocraft.jackpot" : "gui.casinocraft.slot_payout",
                    Money.format(lastPayout));
            color = lastPayout > lastStake ? C_GREEN : (lastPayout > 0 ? C_GOLD : C_RED);
        }
        drawFittedText(context, result, 235, 119, 150, color);
        drawButton(context, 314, 116, 14, 14, Text.literal("?"), showPaytable, true, C_BTN);
        if (showPaytable) {
            drawPaytable(context, bet);
        }
        drawButton(context, SPIN_X, SPIN_Y, SPIN_W, SPIN_H,
                Text.translatable("gui.casinocraft.spin_bet", Money.format(bet)),
                false, !spinning && !rouletteSpinning && bet <= shownBalance, C_BTN);
    }

    private void drawSlotSymbol(DrawContext context, SlotSymbol symbol, double x, double y, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0);
        matrices.scale(scale, scale, 1.0f);
        if (symbol == SlotSymbol.WILD) {
            box(context, -6, -1, 12, 11, 0xFF352047, 0xFF9D65C2);
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(symbol.glyph), 0, 0, symbol.color);
        matrices.pop();
    }

    private void drawFittedText(DrawContext context, Text text, int center, int y, int width, int color) {
        drawFittedText(context, text, center, y, width, color, 1.0f);
    }

    private void drawFittedText(DrawContext context, Text text, int center, int y, int width, int color, float maxScale) {
        float scale = Math.min(maxScale, (float) width / Math.max(1, textRenderer.getWidth(text)));
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(center, y, 0);
        matrices.scale(scale, scale, 1.0f);
        context.drawCenteredTextWithShadow(textRenderer, text, 0, 0, color);
        matrices.pop();
    }

    private void drawPaylineSegment(DrawContext context, int x1, int y1, int x2, int y2) {
        for (int x = x1; x <= x2; x++) {
            int y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
            context.fill(x, y, x + 1, y + 1, 0xAAFFC94A);
        }
    }

    private void drawPaytable(DrawContext context, long bet) {
        box(context, 155, 49, 176, 82, 0xFF151520, C_GOLD);
        drawFittedText(context, Text.translatable("gui.casinocraft.paytable_title"), 235, 53, 146, C_GOLD);
        drawButton(context, 316, 51, 12, 12, Text.literal("X"), false, true, C_BTN);
        context.drawTextWithShadow(textRenderer, Text.literal("3"), 209, 65, C_DIM);
        context.drawTextWithShadow(textRenderer, Text.literal("4"), 250, 65, C_DIM);
        context.drawTextWithShadow(textRenderer, Text.literal("5"), 293, 65, C_DIM);
        for (int i = 0; i < 6; i++) {
            SlotSymbol symbol = SlotSymbol.values()[i];
            int y = 75 + i * 7;
            drawSlotSymbol(context, symbol, 175, y, 0.75f);
            for (int matches = 3; matches <= 5; matches++) {
                drawFittedText(context, Text.literal("x" + symbol.multiplier(matches)),
                        212 + (matches - 3) * 42, y, 38, symbol.color, 0.75f);
            }
        }
        drawFittedText(context, Text.translatable("gui.casinocraft.line_stake", Money.format(bet / SlotMachine.LINE_COUNT)),
                CONTENT_CENTER, 119, 168, C_DIM);
    }

    private void drawRoulette(DrawContext context, long now, long bet) {
        int center;
        if (rouletteSpinning) {
            center = (int) (((now - rouletteStart) / 55L) % 37L);
            if (center < 0) {
                center = 0;
            }
        } else {
            center = Math.max(0, rouletteTarget);
        }

        for (int k = -2; k <= 2; k++) {
            int number = Math.floorMod(center + k, 37);
            drawNumberCell(context, 158 + (k + 2) * ROULETTE_CELL_STEP, ROULETTE_STRIP_Y,
                    ROULETTE_CELL_W, 32, number, k == 0);
        }

        float landing = !rouletteSpinning
                ? CasinoVfx.envelope(now, rouletteRevealAt, roulettePayout > rouletteStake ? 1700 : 450)
                : 0.0f;
        CasinoVfx.glow(context::fill, 158 + 2 * ROULETTE_CELL_STEP, ROULETTE_STRIP_Y,
                ROULETTE_CELL_W, 32, roulettePayout > rouletteStake ? C_GOLD : C_DIM, landing);
        if (effectParticleCount(1) > 0) {
            rouletteEffects.render(context::fill, now, 155, 64, 333, 100);
        }

        for (int i = 0; i < RouletteBet.values().length; i++) {
            RouletteBet type = RouletteBet.values()[i];
            int base = switch (type) {
                case RED -> 0xFF7E2A26;
                case BLACK -> 0xFF222228;
                case ZERO -> 0xFF1E7A3C;
                default -> C_BTN;
            };
            drawButton(context, 158 + i * ROULETTE_CELL_STEP, ROULETTE_BETS_Y, ROULETTE_CELL_W, 14,
                    Text.translatable(type.translationKey), rouletteBet == type, true, base);
        }

        if (rouletteSpinning) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("gui.casinocraft.spinning"), CONTENT_CENTER, 120, C_DIM);
        } else if (rouletteTarget >= 0) {
            Text result = roulettePayout > 0
                    ? Text.translatable("gui.casinocraft.roulette.result",
                    String.valueOf(rouletteTarget), Money.format(roulettePayout))
                    : Text.translatable("gui.casinocraft.roulette.result_loss",
                    String.valueOf(rouletteTarget), Money.format(rouletteStake));
            context.drawCenteredTextWithShadow(textRenderer, result, CONTENT_CENTER, 120,
                    now < rouletteFlashUntil ? (roulettePayout > 0 ? C_GREEN : C_RED) : C_DIM);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("gui.casinocraft.roulette.hint"), CONTENT_CENTER, 120, C_DIM);
        }

        drawButton(context, SPIN_X, SPIN_Y, SPIN_W, SPIN_H,
                Text.translatable("gui.casinocraft.spin_bet", Money.format(bet)),
                false, !rouletteSpinning && !spinning && bet <= shownBalance, C_BTN);
    }

    private void drawNumberCell(DrawContext context, int x, int y, int w, int h, int number, boolean highlighted) {
        int fill = number == 0 ? 0xFF1E7A3C : (Roulette.isRed(number) ? 0xFFB0322A : 0xFF26262E);
        box(context, x, y, w, h, fill, highlighted ? C_GOLD : 0xFF3A3A50);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf(number)),
                x + w / 2, y + 12, 0xFFFFFFFF);
    }

    private void drawReelTrail(DrawContext context, int x, long elapsed) {
        if (effectParticleCount(1) == 0) {
            return;
        }
        int height = CELL_H * SlotMachine.ROWS;
        int offset = (int) ((Math.max(0L, elapsed) / 12L) % (height - 4));
        for (int i = 0; i < 4; i++) {
            int y = REEL_Y + 2 + Math.floorMod(offset - i * 3, height - 4);
            context.fill(x + 3, y, x + CELL_W - 3, y + 1, ((40 - i * 9) << 24) | (C_GOLD & 0xFFFFFF));
        }
    }

    private int effectParticleCount(int fullCount) {
        return switch (MinecraftClient.getInstance().options.getParticles().getValue()) {
            case ALL -> fullCount;
            case DECREASED -> Math.max(1, fullCount / 3);
            case MINIMAL -> 0;
        };
    }

    private boolean startBigWin(Tab game, long now, long payout, long stake, boolean jackpot) {
        if (tab != game || (game == Tab.SLOTS && showPaytable)) {
            return false;
        }
        if (!bigWin.start(now, payout, stake, jackpot)) {
            return false;
        }
        bigWinTab = game;
        slotEffects.clear();
        rouletteEffects.clear();
        return true;
    }

    private void drawBigWin(DrawContext context, long now) {
        float opacity = bigWin.opacity(now);
        if (opacity < 0.04f) {
            return;
        }
        boolean compact = tab == Tab.ROULETTE;
        int left = 156;
        int top = 64;
        int right = 331;
        int bottom = compact ? 100 : 115;
        int accent = bigWin.tier().color;
        context.fill(left, top, right, bottom, BigWinVfx.tint(0x100C18, opacity * 0.96f));
        for (int inset = 0; inset < 5; inset++) {
            context.fill(left + inset, top + inset, right - inset, top + inset + 1,
                    BigWinVfx.tint(accent, opacity * (0.25f - inset * 0.04f)));
            context.fill(left + inset, bottom - inset - 1, right - inset, bottom - inset,
                    BigWinVfx.tint(accent, opacity * (0.25f - inset * 0.04f)));
        }
        CasinoVfx.glow(context::fill, left + 4, top + 4, right - left - 8, bottom - top - 8,
                accent, opacity * 0.65f);
        BigWinVfx.draw(context::fill, bigWin.elapsed(now), effectParticleCount(bigWin.tier().coins),
                opacity, left, top, right, bottom, accent);
        // Text is drawn last so flying coins never obscure the amount.
        String title = bigWin.jackpot() ? "gui.casinocraft.jackpot_title" : bigWin.tier().titleKey;
        float entrance = bigWin.textScale(now);
        drawFittedText(context, Text.translatable(title), CONTENT_CENTER, compact ? 66 : 68, 156,
                BigWinVfx.tint(accent, opacity), (compact ? 0.78f : 1.15f) * entrance);
        drawFittedText(context, Text.literal(Money.format(bigWin.displayedPayout(now))),
                CONTENT_CENTER, compact ? 76 : 82, 150,
                BigWinVfx.tint(0xFFF4D4, opacity), (compact ? 1.1f : 1.6f) * entrance);
        drawFittedText(context, Text.translatable("gui.casinocraft.net_profit", Money.format(bigWin.profit())),
                CONTENT_CENTER, compact ? 90 : 100, 156, BigWinVfx.tint(0xA6EDB5, opacity), 0.65f);
        if (!compact) {
            drawFittedText(context, Text.translatable("gui.casinocraft.skip_count"), CONTENT_CENTER,
                    109, 156, BigWinVfx.tint(C_DIM, opacity), 0.55f);
        }
    }

    private void drawShop(DrawContext context) {
        List<Item> items = visibleShop();
        if (items.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable(shopQuery.trim().isEmpty()
                            ? "gui.casinocraft.shop.empty"
                            : "gui.casinocraft.shop.not_found"), CONTENT_CENTER, 80, C_DIM);
            return;
        }

        int maxRow = maxShopRow(items.size());
        shopRow = Math.max(0, Math.min(shopRow, maxRow));
        int start = shopRow * SHOP_COLUMNS;

        Text hoveredName = null;
        for (int i = 0; i < SHOP_COLUMNS * SHOP_ROWS; i++) {
            int index = start + i;
            if (index >= items.size()) {
                break;
            }
            Item item = items.get(index);
            long price = shopPrice(item);
            int x = SHOP_X + (i % SHOP_COLUMNS) * SHOP_STEP_X;
            int y = SHOP_Y + (i / SHOP_COLUMNS) * SHOP_STEP_Y;
            boolean hovered = inside(localMouseX, localMouseY, x, y, SHOP_CELL_W, SHOP_CELL_H);

            box(context, x, y, SHOP_CELL_W, SHOP_CELL_H,
                    hovered ? 0xFF262636 : 0xFF141420, hovered ? C_GOLD : 0xFF34344A);
            context.drawItem(new ItemStack(item), x + (SHOP_CELL_W - 16) / 2, y + 4);
            Text priceText = Text.literal(Money.format(price));
            context.drawCenteredTextWithShadow(textRenderer, priceText, x + SHOP_CELL_W / 2, y + 25,
                    price <= shownBalance ? C_GOLD : C_RED);

            if (hovered) {
                hoveredName = new ItemStack(item).getName();
            }
        }

        drawShopScrollbar(context, items.size(), maxRow);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal((shopRow + 1) + "/" + (maxRow + 1)),
                311, SHOP_SEARCH_Y + 3, C_DIM);

        if (hoveredName != null) {
            drawFloatingText(context, hoveredName);
        }
    }

    private void drawShopScrollbar(DrawContext context, int size, int maxRow) {
        int totalRows = (size + SHOP_COLUMNS - 1) / SHOP_COLUMNS;
        context.fill(SHOP_SCROLL_X, SHOP_Y, SHOP_SCROLL_X + 3, SHOP_Y + SHOP_SCROLL_H, 0xFF0A0A10);
        int thumbHeight = Math.max(10, SHOP_SCROLL_H * SHOP_ROWS / Math.max(1, totalRows));
        int thumbY = SHOP_Y + (maxRow <= 0 ? 0 : (SHOP_SCROLL_H - thumbHeight) * shopRow / maxRow);
        context.fill(SHOP_SCROLL_X, thumbY, SHOP_SCROLL_X + 3, thumbY + thumbHeight, C_GOLD);
    }

    private List<Item> visibleShop() {
        PriceSnapshot prices = handler.getPrices();
        if (catalogPrices != prices) {
            catalogPrices = prices;
            sortedCatalog = null;
            filteredCatalog = null;
            filterDirty = true;
            shopRow = 0;
        }
        if (!filterDirty && filteredCatalog != null) {
            return filteredCatalog;
        }
        if (sortedCatalog == null) {
            sortedCatalog = new ArrayList<>();
            prices.entries().forEach((rawId, entry) -> {
                Identifier id = Identifier.tryParse(rawId);
                if (entry.shop() > 0L && id != null && Registries.ITEM.containsId(id)) {
                    sortedCatalog.add(Registries.ITEM.get(id));
                }
            });
            nameCache = new HashMap<>();
            for (Item item : sortedCatalog) {
                nameCache.put(item, new ItemStack(item).getName().getString().toLowerCase(Locale.ROOT));
            }
            sortedCatalog.sort(Comparator.comparing(item -> nameCache.get(item)));
        }
        String query = shopQuery.trim().toLowerCase(Locale.ROOT);
        filteredCatalog = new ArrayList<>();
        for (Item item : sortedCatalog) {
            if (query.isEmpty()
                    || nameCache.get(item).contains(query)
                    || Registries.ITEM.getId(item).toString().contains(query)) {
                filteredCatalog.add(item);
            }
        }
        shopRow = 0;
        filterDirty = false;
        return filteredCatalog;
    }

    private static int maxShopRow(int size) {
        int totalRows = (size + SHOP_COLUMNS - 1) / SHOP_COLUMNS;
        return Math.max(0, totalRows - SHOP_ROWS);
    }

    private void drawFloatingText(DrawContext context, Text text) {
        int width = textRenderer.getWidth(text);
        int x = (int) localMouseX + 10;
        int y = (int) localMouseY - 12;
        if (x + width + 4 > CasinoLayout.WIDTH) {
            x = CasinoLayout.WIDTH - width - 4;
        }
        if (y < 0) {
            y = 0;
        }
        context.fill(x - 3, y - 3, x + width + 3, y + 11, 0xF0100018);
        context.fill(x - 3, y - 3, x + width + 3, y - 2, 0xFF7A1F2B);
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFFFF);
    }

    private void drawNotice(DrawContext context, long now) {
        if (noticeKey.isEmpty() || now >= noticeUntil) {
            return;
        }
        Text text = Text.translatable(noticeKey, Text.literal(Money.format(noticeAmount)));
        int width = textRenderer.getWidth(text);
        context.fill(8, CasinoLayout.NOTICE_Y - 3, CasinoLayout.WIDTH - 8, CasinoLayout.NOTICE_Y + 11, 0xB008080C);
        context.drawTextWithShadow(textRenderer, text, (CasinoLayout.WIDTH - width) / 2,
                CasinoLayout.NOTICE_Y, noticePositive ? C_GREEN : C_RED);
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h, Text label,
                            boolean active, boolean enabled, int baseColor) {
        boolean hovered = enabled && inside(localMouseX, localMouseY, x, y, w, h);
        int background = !enabled ? 0xFF1A1A22 : active ? 0xFF6E1F2A : hovered ? C_BTN_HOVER : baseColor;
        context.fill(x, y, x + w, y + h, background);
        context.fill(x, y, x + w, y + 1, active ? C_GOLD : C_BTN_EDGE);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF14141C);
        int color = !enabled ? 0xFF6A6A7A : active ? 0xFFFFD9A0 : C_TEXT;
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, color);
    }

    private static void box(DrawContext context, int x, int y, int w, int h, int fill, int border) {
        context.fill(x, y, x + w, y + h, fill);
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
    }

    private static void slotFrame(DrawContext context, int x, int y) {
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF07070B);
        context.fill(x - 1, y - 1, x + 17, y, 0xFF2C2C3C);
        context.fill(x - 1, y - 1, x, y + 17, 0xFF2C2C3C);
        context.fill(x + 16, y, x + 17, y + 17, 0xFF15151F);
        context.fill(x, y + 16, x + 17, y + 17, 0xFF15151F);
    }

    private static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    // -------------------------------------------------------------- ввод

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double px = mouseX - this.x;
        double py = mouseY - this.y;

        if (button == 0) {
            if (tab == bigWinTab && bigWin.active(Util.getMeasuringTimeMs())
                    && inside(px, py, 156, 64, 175, tab == Tab.ROULETTE ? 36 : 51)) {
                bigWin.finishCounting(Util.getMeasuringTimeMs());
                return true;
            }
            if (tab == Tab.SLOTS) {
                if ((!showPaytable && inside(px, py, 314, 116, 14, 14))
                        || (showPaytable && inside(px, py, 316, 51, 12, 12))) {
                    showPaytable = !showPaytable;
                    bigWin.clear();
                    return true;
                }
                if (showPaytable && inside(px, py, 155, 49, 176, 82)) {
                    return true;
                }
            }
            if (tab == Tab.SHOP && searchField != null
                    && inside(px, py, SHOP_SEARCH_X, SHOP_SEARCH_Y, SHOP_SEARCH_W, SHOP_SEARCH_H)) {
                searchField.setFocused(true);
                searchField.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            if (searchField != null && searchField.isFocused()) {
                searchField.setFocused(false);
            }
            if (inside(px, py, SLOT_TAB_X, TABS_Y, TAB_W, TAB_H)) {
                bigWin.clear();
                tab = Tab.SLOTS;
                return true;
            }
            if (inside(px, py, ROULETTE_TAB_X, TABS_Y, TAB_W, TAB_H)) {
                bigWin.clear();
                tab = Tab.ROULETTE;
                return true;
            }
            if (inside(px, py, SHOP_TAB_X, TABS_Y, TAB_W, TAB_H)) {
                bigWin.clear();
                tab = Tab.SHOP;
                return true;
            }
            if (inside(px, py, SELL_X, SELL_Y, SELL_W, SELL_H)) {
                if (depositPreview() > 0) {
                    ClientPlayNetworking.send(ModNetworking.SELL, PacketByteBufs.create());
                }
                return true;
            }
            if (inside(px, py, WITHDRAW_X, WITHDRAW_Y, WITHDRAW_W, WITHDRAW_H)) {
                if (shownBalance >= 10) {
                    ClientPlayNetworking.send(ModNetworking.WITHDRAW, PacketByteBufs.create());
                }
                return true;
            }
            if (tab != Tab.SHOP && inside(px, py, BET_MINUS_X, BET_Y, BET_BTN_W, BET_BTN_H)) {
                changeBet(-1);
                return true;
            }
            if (tab != Tab.SHOP && inside(px, py, BET_PLUS_X, BET_Y, BET_BTN_W, BET_BTN_H)) {
                changeBet(1);
                return true;
            }
            if (tab != Tab.SHOP && inside(px, py, SPIN_X, SPIN_Y, SPIN_W, SPIN_H)) {
                if (tab == Tab.SLOTS) {
                    spinSlots();
                } else {
                    spinRoulette();
                }
                return true;
            }
            if (tab == Tab.ROULETTE) {
                for (int i = 0; i < RouletteBet.values().length; i++) {
                    if (inside(px, py, 158 + i * ROULETTE_CELL_STEP, ROULETTE_BETS_Y,
                            ROULETTE_CELL_W, 14)) {
                        rouletteBet = RouletteBet.values()[i];
                        return true;
                    }
                }
            }
            if (tab == Tab.SHOP && handleShopClick(px, py)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleShopClick(double px, double py) {
        List<Item> items = visibleShop();
        if (items.isEmpty()) {
            return false;
        }
        int maxRow = maxShopRow(items.size());
        if (inside(px, py, SHOP_SCROLL_X - 3, SHOP_Y, 9, SHOP_SCROLL_H) && maxRow > 0) {
            double ratio = (py - SHOP_Y - 4.0) / (SHOP_SCROLL_H - 8.0);
            shopRow = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * maxRow);
            return true;
        }
        int start = shopRow * SHOP_COLUMNS;
        for (int i = 0; i < SHOP_COLUMNS * SHOP_ROWS; i++) {
            int index = start + i;
            if (index >= items.size()) {
                break;
            }
            int x = SHOP_X + (i % SHOP_COLUMNS) * SHOP_STEP_X;
            int y = SHOP_Y + (i / SHOP_COLUMNS) * SHOP_STEP_Y;
            if (inside(px, py, x, y, SHOP_CELL_W, SHOP_CELL_H)) {
                buy(items.get(index), net.minecraft.client.gui.screen.Screen.hasShiftDown() ? 64 : 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (tab == Tab.SHOP) {
            int maxRow = maxShopRow(visibleShop().size());
            shopRow = Math.max(0, Math.min(maxRow, shopRow - (int) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bigWin.active(Util.getMeasuringTimeMs()) && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            bigWin.clear();
            return true;
        }
        if (tab == Tab.SLOTS && showPaytable && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            showPaytable = false;
            return true;
        }
        if (tab == Tab.SHOP && searchField != null && searchField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchField.setFocused(false);
                return true;
            }
            searchField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (tab == Tab.SHOP && searchField != null && searchField.isFocused()) {
            searchField.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void buy(Item item, int requested) {
        long price = shopPrice(item);
        if (price <= 0L) {
            return;
        }
        long affordable = shownBalance / price;
        int count = (int) Math.min(requested, affordable);
        if (count <= 0) {
            showLocalNotice("message.casinocraft.not_enough", shownBalance);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(Registries.ITEM.getId(item));
        buf.writeVarInt(count);
        buf.writeLong(price);
        ClientPlayNetworking.send(ModNetworking.BUY, buf);
    }

    private long shopPrice(Item item) {
        return handler.getPrices().shopPrice(Registries.ITEM.getId(item).toString());
    }

    private void changeBet(int direction) {
        if (spinning || rouletteSpinning) {
            return;
        }
        betIndex = Math.max(0, Math.min(BET_STEPS.length - 1, betIndex + direction));
    }

    private long currentBet() {
        return BET_STEPS[betIndex];
    }

    private long depositPreview() {
        long total = 0L;
        for (int i = 0; i < CasinoScreenHandler.DEPOSIT_SLOTS; i++) {
            ItemStack stack = this.handler.getDeposit().getStack(i);
            if (!stack.isEmpty()) {
                total += handler.getPrices().totalSellPrice(Registries.ITEM.getId(stack.getItem()).toString(),
                        stack.getCount(), stack.getDamage(), stack.isDamageable() ? stack.getMaxDamage() : 0);
            }
        }
        return total;
    }

    // --------------------------------------------------------- анимации

    private void spinSlots() {
        if (spinning || rouletteSpinning) {
            return;
        }
        long bet = currentBet();
        if (bet > shownBalance) {
            showLocalNotice("message.casinocraft.not_enough", shownBalance);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt((int) bet);
        ClientPlayNetworking.send(ModNetworking.SPIN_SLOTS, buf);

        bigWin.clear();
        spinning = true;
        spinStart = Util.getMeasuringTimeMs();
        showPaytable = false;
        lastLines = List.of();
        pendingReels = null;
        lastPayout = 0L;
        lastStake = bet;
        resultFlashUntil = 0L;
        slotRevealAt = -1L;
        lastJackpot = false;
        slotEffects.clear();
    }

    private void spinRoulette() {
        if (rouletteSpinning || spinning) {
            return;
        }
        long bet = currentBet();
        if (bet > shownBalance) {
            showLocalNotice("message.casinocraft.not_enough", shownBalance);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt((int) bet);
        buf.writeVarInt(rouletteBet.ordinal());
        ClientPlayNetworking.send(ModNetworking.SPIN_ROULETTE, buf);

        bigWin.clear();
        rouletteSpinning = true;
        rouletteStart = Util.getMeasuringTimeMs();
        rouletteTarget = -1;
        roulettePayout = 0L;
        rouletteStake = bet;
        rouletteFlashUntil = 0L;
        rouletteRevealAt = -1L;
        rouletteEffects.clear();
    }

    private void updateAnimations(long now) {
        slotEffects.expire(now);
        rouletteEffects.expire(now);
        if (spinning) {
            if (pendingReels != null) {
                for (int reel = 0; reel < SlotMachine.REELS; reel++) {
                    if (now >= reelStopAt[reel]) {
                        System.arraycopy(pendingReels, reel * SlotMachine.ROWS, reels,
                                reel * SlotMachine.ROWS, SlotMachine.ROWS);
                    }
                }
                if (now >= reelStopAt[SlotMachine.REELS - 1] + 180L) {
                    spinning = false;
                    System.arraycopy(pendingReels, 0, reels, 0, reels.length);
                    shownBalance = pendingBalance;
                    lastPayout = pendingPayout;
                    lastLines = SlotMachine.winningLines(reels);
                    resultFlashUntil = now + 5500L;
                    slotRevealAt = now;
                    lastJackpot = lastLines.stream().anyMatch(win -> win.symbol() == SlotSymbol.SEVEN && win.matches() == 5);
                    boolean largeWin = startBigWin(Tab.SLOTS, now, lastPayout, lastStake, lastJackpot);
                    if (lastPayout > lastStake && !largeWin) {
                        boolean bigWin = lastStake > 0L && (double) lastPayout / lastStake >= 10.0;
                        slotEffects.burst(CONTENT_CENTER, REEL_Y + CELL_H * SlotMachine.ROWS - 3,
                                effectParticleCount(lastJackpot ? 64 : (bigWin ? 48 : 28)), bigWin, now);
                    }
                    pendingReels = null;
                }
            }
        } else if (rouletteSpinning) {
            if (rouletteTarget >= 0 && now >= rouletteStart + 1400L) {
                rouletteSpinning = false;
                shownBalance = rouletteBalance;
                rouletteFlashUntil = now + 5000L;
                rouletteRevealAt = now;
                boolean largeWin = startBigWin(Tab.ROULETTE, now, roulettePayout, rouletteStake, false);
                if (roulettePayout > rouletteStake && !largeWin) {
                    boolean bigWin = rouletteStake > 0L && (double) roulettePayout / rouletteStake >= 10.0;
                    rouletteEffects.burst(CONTENT_CENTER, ROULETTE_STRIP_Y + 29,
                            effectParticleCount(bigWin ? 48 : 24), bigWin, now);
                }
            }
        } else {
            shownBalance = this.handler.getBalance();
        }
    }

    // --------------------------------------------------------- пакеты

    public void onSlotsResult(SlotSymbol[] grid, long payout, long stake, long newBalance) {
        long now = Util.getMeasuringTimeMs();
        double position = spinning ? (now - spinStart) / 55.0 : 0.0;
        spinning = true;
        pendingReels = grid.clone();
        reelMotionStart = now;
        for (int reel = 0; reel < SlotMachine.REELS; reel++) {
            reelStartPosition[reel] = position;
            reelEndStep[reel] = (int) Math.ceil(position) + 22 + reel * 3;
            reelStopAt[reel] = now + 900L + reel * 220L;
        }
        pendingPayout = payout;
        lastStake = stake;
        pendingBalance = newBalance;
    }

    public void onRouletteResult(int number, long payout, long stake, long newBalance) {
        rouletteTarget = number;
        roulettePayout = payout;
        rouletteStake = stake;
        rouletteBalance = newBalance;
    }

    public void onNotice(String key, long amount) {
        if (key.endsWith("not_enough") || key.endsWith("spin_too_fast")
                || key.endsWith("bet_too_small") || key.endsWith("bet_too_big")
                || key.endsWith("invalid_slot_bet")) {
            if (pendingReels == null) {
                spinning = false;
            }
            if (rouletteTarget < 0) {
                rouletteSpinning = false;
            }
        }
        showLocalNotice(key, amount);
        noticePositive = key.endsWith("sold") || key.endsWith("sold_partial")
                || key.endsWith("withdrawn") || key.endsWith("bought");
    }

    private void showLocalNotice(String key, long amount) {
        noticeKey = key;
        noticeAmount = amount;
        noticePositive = false;
        noticeUntil = Util.getMeasuringTimeMs() + 4500L;
    }
}

