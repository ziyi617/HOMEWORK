import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
public class Line {
	private String name;
	private List<Station> stations;
	private Map<String, Map<String, Integer>> distances;

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

