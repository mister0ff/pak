package uk.lgl.modmenu;

/**
 * Deterministic aim-smoothing helper for synthetic targets owned by the
 * practice app. It accepts normalized coordinates only and has no screen,
 * process, memory, input-injection, or game integration APIs.
 */
public final class PracticeAimEngine {
    public static final class Point {
        public final float x, y;
        public Point(float x, float y) { this.x = x; this.y = y; }
    }

    private PracticeAimEngine() {}

    public static Point step(Point current, Point target, float smoothing, float maxStep) {
        float s = clamp(smoothing, 0.01f, 1.0f);
        float limit = clamp(maxStep, 0.001f, 1.0f);
        float dx = (target.x - current.x) * s;
        float dy = (target.y - current.y) * s;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length > limit) {
            dx = dx / length * limit;
            dy = dy / length * limit;
        }
        return new Point(clamp(current.x + dx, 0f, 1f),
                         clamp(current.y + dy, 0f, 1f));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
