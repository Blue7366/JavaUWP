package net.minecraftforge.event;

public class TickEvent {
    public final Phase phase;

    protected TickEvent(Phase phase) {
        this.phase = phase;
    }

    public enum Phase {
        START,
        END
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(phase);
        }
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(phase);
            this.renderTickTime = renderTickTime;
        }
    }
}
