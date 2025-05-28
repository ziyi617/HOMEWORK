import java.io.*;
import java.util.*;
public class Test {
    public static void main(String[] args) {
        SubwaySystem subway = new SubwaySystem();
        try {
            // 加载数据
            subway.loadFromFile("subway.txt");
            
            // 测试1: 获取中转站
            System.out.println("中转站:");
            Set<Map.Entry<String, Set<String>>> transfers = subway.getTransferStations();
            transfers.forEach(entry -> 
                System.out.println(entry.getKey() + ": " + entry.getValue()));
            
            // 测试2: 获取附近站点
            System.out.println("\n附近站点:");
            Set<Map.Entry<String, Map.Entry<String, Integer>>> nearby = subway.getNearbyStations("华中科技大学站", 1);
            nearby.forEach(entry -> 
                System.out.println(entry.getKey() + ": " + entry.getValue().getKey() + ", " + entry.getValue().getValue()));
            
            // 测试3: 获取所有路径
            System.out.println("\n所有路径:");
            Set<List<String>> allPaths = subway.getAllPaths("武昌火车站", "汉口火车站");
            allPaths.forEach(System.out::println);
            
            // 测试4: 获取最短路径
            System.out.println("\n最短路径:");
            Path shortestPath = subway.getShortestPath("武昌火车站", "汉口火车站");
            System.out.println(shortestPath.getStations() + ", 距离: " + shortestPath.getDistance());
            
            // 测试5: 简洁打印路径
            System.out.println("\n路径指引:");
            subway.printPath(shortestPath);
            
            // 测试6: 计算票价
            System.out.println("\n票价:");
            System.out.println("普通票: " + subway.calculateFare(shortestPath) + "元");
            System.out.println("日票: " + subway.calculateDailyPassFare() + "元");
            
        } catch (IOException e) {
            System.err.println("加载文件失败: " + e.getMessage());
        }
    }
}