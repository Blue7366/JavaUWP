package banditvault.controllercore;

import java.util.List;

public final class GridNavigation {
    private static final int ALIGN_TOLERANCE = 4;

    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public static final class Point {
        public final int id;
        public final int x;
        public final int y;

        public Point(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private GridNavigation() {
    }

    public static int next(List<Point> points, int currentId, Direction direction) {
        Point current = find(points, currentId);
        if (current == null) {
            return -1;
        }

        return findDirectional(points, current, direction);
    }

    public static int nextLoose(List<Point> points, int currentId, Direction direction) {
        Point current = find(points, currentId);
        if (current == null) {
            return -1;
        }
        int aligned = findAligned(points, current, direction);
        if (aligned >= 0) {
            return aligned;
        }
        return findDirectional(points, current, direction);
    }

    private static int findDirectional(List<Point> points, Point current, Direction direction) {
        Point best = null;
        long bestScore = Long.MAX_VALUE;
        for (Point candidate : points) {
            if (candidate.id == current.id || !isAhead(current, candidate, direction)) {
                continue;
            }
            int primary = isHorizontal(direction)
                ? Math.abs(candidate.x - current.x)
                : Math.abs(candidate.y - current.y);
            int cross = isHorizontal(direction)
                ? Math.abs(candidate.y - current.y)
                : Math.abs(candidate.x - current.x);
            long score = (long) primary * primary + (long) cross * cross * 4L;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best == null ? -1 : best.id;
    }

    private static Point find(List<Point> points, int id) {
        for (Point point : points) {
            if (point.id == id) {
                return point;
            }
        }
        return null;
    }

    private static int findAligned(List<Point> points, Point current, Direction direction) {
        Point best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Point candidate : points) {
            if (candidate.id == current.id || !isAhead(current, candidate, direction)) {
                continue;
            }
            int crossDistance = isHorizontal(direction)
                ? Math.abs(candidate.y - current.y)
                : Math.abs(candidate.x - current.x);
            if (crossDistance > ALIGN_TOLERANCE) {
                continue;
            }
            int distance = isHorizontal(direction)
                ? Math.abs(candidate.x - current.x)
                : Math.abs(candidate.y - current.y);
            if (distance < bestDistance || (distance == bestDistance && crossDistance < crossDistance(best, current, direction))) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best == null ? -1 : best.id;
    }

    private static boolean isAhead(Point current, Point candidate, Direction direction) {
        switch (direction) {
            case UP: return candidate.y < current.y;
            case DOWN: return candidate.y > current.y;
            case LEFT: return candidate.x < current.x;
            case RIGHT: return candidate.x > current.x;
            default: return false;
        }
    }

    private static boolean isHorizontal(Direction direction) {
        return direction == Direction.LEFT || direction == Direction.RIGHT;
    }

    private static int crossDistance(Point point, Point current, Direction direction) {
        if (point == null) {
            return Integer.MAX_VALUE;
        }
        return isHorizontal(direction) ? Math.abs(point.y - current.y) : Math.abs(point.x - current.x);
    }

    public static void main(String[] args) {
        java.util.List<Point> grid = java.util.Arrays.asList(
            new Point(0, 0, 0), new Point(1, 18, 0), new Point(2, 36, 0),
            new Point(3, 0, 18), new Point(4, 36, 18),
            new Point(5, 0, 54), new Point(6, 18, 54), new Point(7, 36, 54));
        assert next(grid, 0, Direction.RIGHT) == 1;
        assert next(grid, 1, Direction.DOWN) == 3;
        assert next(grid, 3, Direction.RIGHT) == 4;
        assert next(grid, 4, Direction.DOWN) == 7;
        assert next(grid, 0, Direction.LEFT) == -1;
        assert nextLoose(grid, 3, Direction.RIGHT) == 4;

        java.util.List<Point> inventory = java.util.Arrays.asList(
            new Point(0, 8, 44),
            new Point(1, 8, 26),
            new Point(2, 98, 18),
            new Point(3, 98, 36),
            new Point(4, 154, 28));
        assert next(inventory, 0, Direction.UP) == 1;
        assert next(inventory, 1, Direction.RIGHT) == 2;
        assert next(inventory, 4, Direction.LEFT) == 3;
    }
}
