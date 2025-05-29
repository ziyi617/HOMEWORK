package subway;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
public class Station {
	private String name;
	private Set<String> lines;

    public Station(String name) {
        this.name = name;
        this.lines = new HashSet<>();
    }

    public void addLine(String line) {
        lines.add(line);
    }

    public String getName() {
        return name;
    }

    public Set<String> getLines() {
        return lines;
    }

    public boolean isTransfer() {
        return lines.size() >= 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return name.equals(station.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
