package net.create_nomad.util;

public final class BackpackRefillHighlightState {
    public static final int HOTBAR_SLOTS = 9;
    public static final int HIGHLIGHT_TICKS = 20;
    private static final int[] BACKPACK_SLOT_TIMERS = new int[HOTBAR_SLOTS];
    private static final int[] TRACKPACK_SLOT_TIMERS = new int[HOTBAR_SLOTS];

    private BackpackRefillHighlightState() {
    }

    public static void markBackpackSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            BACKPACK_SLOT_TIMERS[slot] = HIGHLIGHT_TICKS;
        }
    }

    public static void markTrackpackSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            TRACKPACK_SLOT_TIMERS[slot] = HIGHLIGHT_TICKS;
        }
    }

    public static void tickDown() {
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (BACKPACK_SLOT_TIMERS[i] > 0)
                BACKPACK_SLOT_TIMERS[i]--;
            if (TRACKPACK_SLOT_TIMERS[i] > 0)
                TRACKPACK_SLOT_TIMERS[i]--;
        }
    }

    public static int getBackpackTimer(int slot) {
        if (slot < 0 || slot >= HOTBAR_SLOTS) {
            return 0;
        }
        return BACKPACK_SLOT_TIMERS[slot];
    }

    public static int getTrackpackTimer(int slot) {
        if (slot < 0 || slot >= HOTBAR_SLOTS) {
            return 0;
        }
        return TRACKPACK_SLOT_TIMERS[slot];
    }
}
