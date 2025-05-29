package subway;

import java.util.*;

public class Path {
	
	private List<String> stations;
	private int distance;

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

