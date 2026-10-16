package io.github.justhurky.groundsegment.telemetry;

import java.util.Optional;

public record ReplayWindow(int lastSequence, long seenMask) {

    public static final int SEQUENCE_SPACE = 16384;
    public static final int MAX_WINDOW_SIZE = Long.SIZE;

    public static ReplayWindow startingAt(int sequence) {
        requireInRange(sequence);
        return new ReplayWindow(sequence, 1L);
    }

    public Optional<ReplayWindow> accept(int sequence, int windowSize) {
        requireInRange(sequence);
        int effectiveWindow = Math.min(Math.max(windowSize, 1), MAX_WINDOW_SIZE);
        long retainMask = effectiveWindow == Long.SIZE ? -1L : (1L << effectiveWindow) - 1L;

        int distanceAhead = Math.floorMod(sequence - lastSequence, SEQUENCE_SPACE);
        if (distanceAhead == 0) {
            return Optional.empty();
        }
        if (distanceAhead < SEQUENCE_SPACE / 2) {
            long shifted = distanceAhead >= Long.SIZE ? 0L : seenMask << distanceAhead;
            return Optional.of(new ReplayWindow(sequence, (shifted | 1L) & retainMask));
        }

        int distanceBehind = SEQUENCE_SPACE - distanceAhead;
        if (distanceBehind >= effectiveWindow) {
            return Optional.empty();
        }
        long bit = 1L << distanceBehind;
        if ((seenMask & bit) != 0L) {
            return Optional.empty();
        }
        return Optional.of(new ReplayWindow(lastSequence, (seenMask | bit) & retainMask));
    }

    private static void requireInRange(int sequence) {
        if (sequence < 0 || sequence >= SEQUENCE_SPACE) {
            throw new IllegalArgumentException("Sequence count " + sequence + " is outside the 14 bit range");
        }
    }
}
