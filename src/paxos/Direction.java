package paxos;

public enum Direction {
    FORWARD(0),
    LEFT(1),
    RIGHT(2),
    BACKWARD(3);

    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public int asInt() {
        return value;
    }

    public static Direction oppositeFromInt(int value) {
        switch (value) {
        case 0:
            return Direction.BACKWARD;
        case 1:
            return Direction.RIGHT;
        case 2:
            return Direction.LEFT;
        case 3:
            return Direction.FORWARD;
        default:
            throw new IllegalArgumentException("Out of enum range: " + value);
        }
    }

    public static Direction fromInt(int value) {
        for (Direction myEnum : Direction.values()) {
            if (myEnum.asInt() == value) {
                return myEnum;
            }
        }
        throw new IllegalArgumentException("Out of enum range: " + value);
    }
}
