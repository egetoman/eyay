package kuroyale.domain;

public class Bridge {

    private Position start;
    private Position end;
    private int width;

    public Bridge() {
    }

    public Bridge(Position start, Position end, int width) {
        this.start = start;
        this.end = end;
        this.width = width;
    }

    public Bridge(Bridge other) {
        this(
            other.start != null ? new Position(other.start.getX(), other.start.getY()) : null,
            other.end != null ? new Position(other.end.getX(), other.end.getY()) : null,
            other.width
        );
    }

    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Position getEnd() {
        return end;
    }

    public void setEnd(Position end) {
        this.end = end;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}

