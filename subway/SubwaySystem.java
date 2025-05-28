package subway;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
public class SubwaySystem {
	private final Map<String, Station> stations;
    private final Map<String, Line> lines;

    public SubwaySystem() {
        stations = new HashMap<>();
        lines = new HashMap<>();
    }
    public void loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String currentLineName = null;
            String line;
            boolean skipNextLine = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过空行
                if (line.isEmpty()) {
                    skipNextLine = false;
                    continue;
                }
                
                // 检测汉字线路标题（如"1号线站点间距"、"二号线站点信息"）
                if (line.contains("号线")) {
                    // 提取汉字线路名称（如"1号线"、"二号线"）
                    int index = line.indexOf("号线");
                    if (index > 0) {
                        currentLineName = line.substring(0, index + 2);
                        skipNextLine = true; // 下一行是表头，需要跳过
                    }
                    continue;
                }
                
                // 跳过表头行（如"站点名称 间距（KM）"）
                if (skipNextLine || line.contains("站点名称") || line.contains("间距")) {
                    skipNextLine = false;
                    continue;
                }
                
                // 解析站点数据（支持多种分隔符：---、——、~）
                if (currentLineName != null && (line.contains("---") || line.contains("——") || line.contains("~"))) {
                    // 统一替换不同分隔符
                    String normalizedLine = line.replace("——", "---").replace("~", "---");
                    String[] parts = normalizedLine.split("---|\\s+");
                    List<String> validParts = new ArrayList<>();
                    
                    // 过滤空字符串
                    for (String part : parts) {
                        if (!part.isEmpty() && !part.equals("KM") && !part.equals("（KM）")) {
                            validParts.add(part);
                        }
                    }
                    
                    if (validParts.size() >= 2) {
                        String station1 = validParts.get(0);
                        String station2 = validParts.get(1);
                        double distance = 0;
                        
                        // 解析距离（可能在第2或第3个位置）
                        if (validParts.size() >= 3) {
                            try {
                                distance = Double.parseDouble(validParts.get(2)) * 1000; // KM转米
                            } catch (NumberFormatException e) {
                                // 如果第三个部分不是数字，尝试第二个部分
                                try {
                                    distance = Double.parseDouble(validParts.get(1)) * 1000;
                                    station2 = validParts.get(0); // 调整站点位置
                                } catch (NumberFormatException ex) {
                                    System.err.println("距离解析失败: " + line);
                                    continue;
                                }
                            }
                        }
                        
                        // 添加站点信息
                        addStationToLine(currentLineName, station1, station2, (int)distance);
                    }
                }
            }
        }
    }
    
    private void addStationToLine(String lineName, String station1, String station2, int distance) {
        // 确保线路名称包含"号线"后缀
        if (!lineName.endsWith("号线")) {
            lineName += "号线";
        }
        
        Line currentLine = lines.computeIfAbsent(lineName, Line::new);
        
        // 处理站点名称中的括号内容（如"华中科技大学(地铁站)"）
        station1 = cleanStationName(station1);
        station2 = cleanStationName(station2);
        
        Station s1 = stations.computeIfAbsent(station1, Station::new);
        s1.addLine(lineName);
        if (!currentLine.getStations().contains(s1)) {
            currentLine.addStation(s1);
        }
        
        Station s2 = stations.computeIfAbsent(station2, Station::new);
        s2.addLine(lineName);
        if (!currentLine.getStations().contains(s2)) {
            currentLine.addStation(s2);
        }
        
        // 添加双向距离
        currentLine.addDistance(station1, station2, distance);
    }

    // 清理站点名称中的无关字符
    private String cleanStationName(String name) {
        // 移除括号内容（如"(地铁站)"）
        name = name.replaceAll("\\(.*?\\)", "");
        // 移除特殊符号
        name = name.replaceAll("[【】]", "");
        return name.trim();
    }
    
    public int getStationCount() {
        return stations.size();
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
            throw new IllegalArgumentException("站点不存在: " + stationName);
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
            if (stationIndex == -1) continue;

            // 向左搜索（累加实际距离）
            int leftDistance = 0;
            for (int i = stationIndex - 1; i >= 0; i--) {
                String currentStation = lineStations.get(i).getName();
                String nextStation = lineStations.get(i + 1).getName();
                int segmentDistance = line.getDistance(currentStation, nextStation);
                leftDistance += segmentDistance;
                
                if (leftDistance <= n) {
                    result.add(Map.entry(currentStation, Map.entry(lineName, leftDistance)));
                } else {
                    break;
                }
            }

            // 向右搜索（累加实际距离）
            int rightDistance = 0;
            for (int i = stationIndex + 1; i < lineStations.size(); i++) {
                String currentStation = lineStations.get(i).getName();
                String prevStation = lineStations.get(i - 1).getName();
                int segmentDistance = line.getDistance(prevStation, currentStation);
                rightDistance += segmentDistance;
                
                if (rightDistance <= n) {
                    result.add(Map.entry(currentStation, Map.entry(lineName, rightDistance)));
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
