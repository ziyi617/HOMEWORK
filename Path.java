import java.util.*;
class Path {
    private final List<String> stations;
    private final int distance;

    public Path(List<String> stations, int distance) {
        this.stations = stations;
        this.distance = distance;
    }

    public List<String> getStations() {
        return stations;
    }

    public int getDistance() {
        return distance;
    }
}