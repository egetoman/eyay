package kuroyale.emote;

import java.util.ArrayDeque;
import java.util.Deque;

public class EmoteLimiter {
    private final long cooldownMillis;
    private final int maxInWindow;
    private final long windowMillis;
    private final long blockMillis;
    private final Deque<Long> recent = new ArrayDeque<>();
    private long lastSentAt = 0;
    private long blockedUntil = 0;

    public EmoteLimiter(long cooldownMillis, int maxInWindow, long windowMillis, long blockMillis) {
        this.cooldownMillis = cooldownMillis;
        this.maxInWindow = maxInWindow;
        this.windowMillis = windowMillis;
        this.blockMillis = blockMillis;
    }

    public static EmoteLimiter defaultLimiter() {
        return new EmoteLimiter(1200, 4, 4000, 2500);
    }

    public EmoteSendResult tryConsume(long nowMillis) {
        if (nowMillis < blockedUntil) {
            long wait = blockedUntil - nowMillis;
            return EmoteSendResult.blocked("Emotes muted for " + (wait / 1000.0) + "s");
        }
        if (nowMillis - lastSentAt < cooldownMillis) {
            return EmoteSendResult.blocked("Emote cooldown");
        }
        while (!recent.isEmpty() && nowMillis - recent.peekFirst() > windowMillis) {
            recent.pollFirst();
        }
        if (recent.size() >= maxInWindow) {
            blockedUntil = nowMillis + blockMillis;
            recent.clear();
            return EmoteSendResult.blocked("Emote spam block");
        }
        recent.addLast(nowMillis);
        lastSentAt = nowMillis;
        return EmoteSendResult.allowed();
    }

    public static final class EmoteSendResult {
        private final boolean allowed;
        private final String reason;

        private EmoteSendResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static EmoteSendResult allowed() {
            return new EmoteSendResult(true, "");
        }

        public static EmoteSendResult blocked(String reason) {
            return new EmoteSendResult(false, reason != null ? reason : "");
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }
}
