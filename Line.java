
import java.util.*;

public class Line {
	private final String name;
	private final List<Station> stations;
	private final Map<String, Map<String, Integer>> distances;

	public Line(String name) {
		this.name = name;
	    this.stations = new ArrayList<>();
	    this.distances = new HashMap<>();
	}

	public void addStation(Station station) {
	    stations.add(station);
	    distances.putIfAbsent(station.getName(), new HashMap<>());
	}

	public void addDistance(String from, String to, int distance) {
	    distances.get(from).put(to, distance);
	    distances.get(to).put(from, distance);
	}

	public String getName() {
	    return name;
	}

	public List<Station> getStations() {
	    return stations;
	}

	public Integer getDistance(String from, String to) {
		return distances.get(from).get(to);
	}
}

