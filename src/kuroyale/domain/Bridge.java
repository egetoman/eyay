package kuroyale.domain;

public class Bridge {
    private final Position start;
    private final Position end;

    public Bridge(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }
}
