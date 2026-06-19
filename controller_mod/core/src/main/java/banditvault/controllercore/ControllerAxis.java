package banditvault.controllercore;

public enum ControllerAxis {
    LEFT_X(0),
    LEFT_Y(1),
    RIGHT_X(2),
    RIGHT_Y(3),
    LEFT_TRIGGER(4),
    RIGHT_TRIGGER(5);

    public final int index;

    ControllerAxis(int index) {
        this.index = index;
    }
}
