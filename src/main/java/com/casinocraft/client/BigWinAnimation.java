package com.casinocraft.client;

/** Presentation of an already-settled server payout; never updates the player's balance. */
public final class BigWinAnimation {
    public enum Tier {
        BIG(1200, 3200, 24, 0xFFFFC94A, "gui.casinocraft.big_win"),
        MEGA(1800, 4100, 40, 0xFFFFA943, "gui.casinocraft.mega_win"),
        EPIC(2400, 5000, 64, 0xFFE2ADFF, "gui.casinocraft.epic_win");

        public final int countMs;
        public final int durationMs;
        public final int coins;
        public final int color;
        public final String titleKey;

        Tier(int countMs, int durationMs, int coins, int color, String titleKey) {
            this.countMs = countMs;
            this.durationMs = durationMs;
            this.coins = coins;
            this.color = color;
            this.titleKey = titleKey;
        }
    }

    private Tier tier;
    private long started;
    private long payout;
    private long stake;
    private long finishedAt = -1L;
    private boolean jackpot;

    public boolean start(long now, long payout, long stake, boolean jackpot) {
        clear();
        if (stake <= 0L || payout <= 0L || payout / stake < 10L) {
            return false;
        }
        long multiple = payout / stake;
        tier = multiple >= 100L ? Tier.EPIC : multiple >= 25L ? Tier.MEGA : Tier.BIG;
        started = now;
        this.payout = payout;
        this.stake = stake;
        this.jackpot = jackpot;
        return true;
    }

    public void clear() {
        tier = null;
        finishedAt = -1L;
        started = 0L;
        payout = 0L;
        stake = 0L;
        jackpot = false;
    }

    public Tier tier() { return tier; }
    public long payout() { return payout; }
    public long profit() { return payout - stake; }
    public boolean jackpot() { return jackpot; }
    public long elapsed(long now) { return Math.max(0L, now - started); }

    private long endAt() {
        long naturalEnd = started + tier.durationMs;
        return finishedAt < 0L ? naturalEnd : Math.min(naturalEnd, finishedAt + 900L);
    }

    public boolean active(long now) {
        return tier != null && now >= started && now < endAt();
    }

    public void finishCounting(long now) {
        if (active(now) && finishedAt < 0L) {
            finishedAt = now;
        }
    }

    public long displayedPayout(long now) {
        if (tier == null) {
            return 0L;
        }
        if (finishedAt >= 0L || elapsed(now) >= tier.countMs) {
            return payout;
        }
        double progress = (double) elapsed(now) / tier.countMs;
        double remaining = 1.0 - progress;
        return Math.min(payout, Math.max(0L, Math.round(payout * (1.0 - remaining * remaining * remaining))));
    }

    public float opacity(long now) {
        if (!active(now)) {
            return 0.0f;
        }
        return (float) Math.min(1.0, Math.min(elapsed(now) / 180.0, (endAt() - now) / 450.0));
    }

    public float textScale(long now) {
        double progress = Math.min(1.0, elapsed(now) / 300.0);
        return (float) (0.86 + 0.14 * (1.0 - Math.pow(1.0 - progress, 3.0)));
    }
}
