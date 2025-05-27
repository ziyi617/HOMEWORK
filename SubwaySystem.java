import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
class SubwaySystem {
    private Map<String, Station> stations;
    private Map<String, Line> lines;

    public SubwaySystem() {
        stations = new HashMap<>();
        lines = new HashMap<>();
    }

    public void loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 4) continue;

                String lineName = parts[0];
                String station1Name = parts[1];
                String station2Name = parts[2];
                int distance = Integer.parseInt(parts[3]);

                Line currentLine = lines.computeIfAbsent(lineName, Line::new);
                
                Station station1 = stations.computeIfAbsent(station1Name, Station::new);
                station1.addLine(lineName);
                currentLine.addStation(station1);
                
                Station station2 = stations.computeIfAbsent(station2Name, Station::new);
                station2.addLine(lineName);
                currentLine.addStation(station2);
                
                currentLine.addDistance(station1Name, station2Name, distance);
            }
        }
    }

// 1) 识别所有地铁中转站
    public Set<Map.Entry<String, Set<String>>> getTransferStations() {
        return stations.entrySet().stream()
                .filter(entry -> entry.getValue().isTransfer())
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getLines()))
                .collect(Collectors.toSet());
    }

    // 2) 获取距离小于n的所有站点
    public Set<Map.Entry<String, Map.Entry<String, Integer>>> getNearbyStations(String stationName, int n) {
        if (!stations.containsKey(stationName)) {
            throw new IllegalArgumentException("Station not found: " + stationName);
        }

        Set<Map.Entry<String, Map.Entry<String, Integer>>> result = new HashSet<>();
        Station station = stations.get(stationName);

        for (String lineName : station.getLines()) {
            Line line = lines.get(lineName);
            List<Station> lineStations = line.getStations();
            
            int stationIndex = -1;
            for (int i = 0; i < lineStations.size(); i++) {
                if (lineStations.get(i).getName().equals(stationName)) {
                    stationIndex = i;
                    break;
                }
            }

            // 向左搜索
            int accumulatedDistance = 0;
            for (int i = stationIndex - 1; i >= 0; i--) {
                String currentStation = lineStations.get(i).getName();
                int segmentDistance = line.getDistance(stationName, currentStation);
                accumulatedDistance += segmentDistance;
                
                if (accumulatedDistance <= n) {
                    result.add(Map.entry(currentStation, Map.entry(lineName, accumulatedDistance)));
                } else {
                    break;
                }
            }

            // 向右搜索
            accumulatedDistance = 0;
            for (int i = stationIndex + 1; i < lineStations.size(); i++) {
                String currentStation = lineStations.get(i).getName();
                int segmentDistance = line.getDistance(stationName, currentStation);
                accumulatedDistance += segmentDistance;
                
                if (accumulatedDistance <= n) {
                    result.add(Map.entry(currentStation, Map.entry(lineName, accumulatedDistance)));
                } else {
                    break;
                }
            }
        }

        return result;
    }

    // 3) 获取所有无环路径
    public Set<List<String>> getAllPaths(String start, String end) {
        if (!stations.containsKey(start) || !stations.containsKey(end)) {
            throw new IllegalArgumentException("Station not found");
        }

        Set<List<String>> paths = new HashSet<>();
        List<String> currentPath = new ArrayList<>();
        currentPath.add(start);
        dfsFindPaths(start, end, currentPath, paths);
        return paths;
    }

    private void dfsFindPaths(String current, String end, List<String> currentPath, Set<List<String>> paths) {
        if (current.equals(end)) {
            paths.add(new ArrayList<>(currentPath));
            return;
        }

        Station station = stations.get(current);
        for (String lineName : station.getLines()) {
            Line line = lines.get(lineName);
            List<Station> lineStations = line.getStations();
            
            int stationIndex = -1;
            for (int i = 0; i < lineStations.size(); i++) {
                if (lineStations.get(i).getName().equals(current)) {
                    stationIndex = i;
                    break;
                }
            }

            // 检查相邻站点
            if (stationIndex > 0) {
                String prevStation = lineStations.get(stationIndex - 1).getName();
                if (!currentPath.contains(prevStation)) {
                    currentPath.add(prevStation);
                    dfsFindPaths(prevStation, end, currentPath, paths);
                    currentPath.remove(currentPath.size() - 1);
                }
            }

            if (stationIndex < lineStations.size() - 1) {
                String nextStation = lineStations.get(stationIndex + 1).getName();
                if (!currentPath.contains(nextStation)) {
                    currentPath.add(nextStation);
                    dfsFindPaths(nextStation, end, currentPath, paths);
                    currentPath.remove(currentPath.size() - 1);
                }
            }
        }
    }

 // 4) 获取最短路径 (使用Dijkstra算法)
    public Path getShortestPath(String start, String end) {
        if (!stations.containsKey(start) || !stations.containsKey(end)) {
            throw new IllegalArgumentException("Station not found");
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        // 初始化
        for (String station : stations.keySet()) {
            if (station.equals(start)) {
                distances.put(station, 0);
            } else {
                distances.put(station, Integer.MAX_VALUE);
            }
            queue.add(station);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) break;

            Station station = stations.get(current);
            for (String lineName : station.getLines()) {
                Line line = lines.get(lineName);
                List<Station> lineStations = line.getStations();
                
                int stationIndex = -1;
                for (int i = 0; i < lineStations.size(); i++) {
                    if (lineStations.get(i).getName().equals(current)) {
                        stationIndex = i;
                        break;
                    }
                }

                // 检查相邻站点
                if (stationIndex > 0) {
                    String prevStation = lineStations.get(stationIndex - 1).getName();
                    int distance = line.getDistance(current, prevStation);
                    int alt = distances.get(current) + distance;
                    if (alt < distances.get(prevStation)) {
                        distances.put(prevStation, alt);
                        previous.put(prevStation, current);
                        queue.remove(prevStation);
                        queue.add(prevStation);
                    }
                }

                if (stationIndex < lineStations.size() - 1) {
                    String nextStation = lineStations.get(stationIndex + 1).getName();
                    int distance = line.getDistance(current, nextStation);
                    int alt = distances.get(current) + distance;
                    if (alt < distances.get(nextStation)) {
                        distances.put(nextStation, alt);
                        previous.put(nextStation, current);
                        queue.remove(nextStation);
                        queue.add(nextStation);
                    }
                }
            }
        }

        // 构建路径
        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        return new Path(path, distances.get(end));
    }

   // 5) 简洁打印路径
    public void printPath(Path path) {
        List<String> stations = path.getStations();
        if (stations.size() < 2) {
            System.out.println("路径太短");
            return;
        }

        String currentLine = getCommonLine(stations.get(0), stations.get(1));
        String startStation = stations.get(0);
        
        for (int i = 1; i < stations.size(); i++) {
            String nextLine = i < stations.size() - 1 ? getCommonLine(stations.get(i), stations.get(i + 1)) : currentLine;
            
            if (!nextLine.equals(currentLine)) {
                System.out.printf("乘坐%s从%s到%s，", currentLine, startStation, stations.get(i));
                startStation = stations.get(i);
                currentLine = nextLine;
            }
        }
        
        System.out.printf("乘坐%s从%s到%s\n", currentLine, startStation, stations.get(stations.size() - 1));
    }

    private String getCommonLine(String station1, String station2) {
        Set<String> lines1 = stations.get(station1).getLines();
        Set<String> lines2 = stations.get(station2).getLines();
        
        for (String line : lines1) {
            if (lines2.contains(line)) {
                return line;
            }
        }
        return "";
    }

    // 6) 计算普通票价
    public int calculateFare(Path path) {
        int distance = path.getDistance();
        if (distance <= 4) return 2;
        if (distance <= 8) return 3;
        if (distance <= 12) return 4;
        if (distance <= 18) return 5;
        if (distance <= 24) return 6;
        return 7; // 超过24公里
    }

    // 7) 计算日票票价
    public int calculateDailyPassFare() {
        return 0;
    }
}
