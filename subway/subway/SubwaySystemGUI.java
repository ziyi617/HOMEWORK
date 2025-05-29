package subway;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

public class SubwayGUI extends JFrame {
    private SubwaySystem subway;
    private MapPanel mapPanel;
    private JTextArea infoArea;
    private JTextField searchField;
    private JComboBox<String> lineFilter;
    private double scale = 1.0;
    private Point2D.Double translate = new Point2D.Double(0, 0);
    private Point lastDragPoint;
    private String selectedStation;

    public SubwayGUI(SubwaySystem subway) {
        this.subway = subway;
        setTitle("地铁线路查询系统");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
    }

    private void initComponents() {
        // 创建主布局
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(800);
        
        // 左侧地图面板
        mapPanel = new MapPanel();
        mapPanel.setBackground(new Color(240, 248, 255));
        mapPanel.addMouseListener(new MapMouseListener());
        mapPanel.addMouseMotionListener(new MapMouseMotionListener());
        mapPanel.addMouseWheelListener(new MapMouseWheelListener());
        
        // 右侧控制面板
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 搜索面板
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        searchField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchField.addActionListener(e -> searchStation());
        
        JButton searchButton = new JButton("搜索");
        searchButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        searchButton.addActionListener(e -> searchStation());
        
        JPanel searchGroup = new JPanel(new BorderLayout());
        searchGroup.add(searchField, BorderLayout.CENTER);
        searchGroup.add(searchButton, BorderLayout.EAST);
        
        searchPanel.add(new JLabel("站点搜索:"), BorderLayout.WEST);
        searchPanel.add(searchGroup, BorderLayout.CENTER);
        
        // 线路筛选
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("线路筛选:"));
        lineFilter = new JComboBox<>();
        lineFilter.addItem("全部线路");
        for (String line : subway.getLines().keySet()) {
            lineFilter.addItem(line);
        }
        lineFilter.addActionListener(e -> filterByLine());
        filterPanel.add(lineFilter);
        
        // 功能按钮面板
        JPanel functionPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        functionPanel.setBorder(BorderFactory.createTitledBorder("系统功能"));
        
        String[] buttonLabels = {
            "显示中转站", "附近站点查询", "查找所有路径", 
            "查找最短路径", "计算票价", "重置视图"
        };
        
        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            button.addActionListener(new FunctionButtonListener());
            functionPanel.add(button);
        }
        
        // 信息显示区
        infoArea = new JTextArea();
        infoArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setBorder(BorderFactory.createTitledBorder("信息显示"));
        
        // 组装控制面板
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.add(searchPanel);
        topPanel.add(filterPanel);
        
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(functionPanel, BorderLayout.CENTER);
        controlPanel.add(infoScroll, BorderLayout.SOUTH);
        
        // 状态栏
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("就绪 | 使用鼠标滚轮缩放，拖拽平移地图 | 点击站点查看详情");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusBar.add(statusLabel);
        
        // 添加到主窗口
        mainSplit.setLeftComponent(new JScrollPane(mapPanel));
        mainSplit.setRightComponent(controlPanel);
        
        add(mainSplit, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);


// 初始化显示
        showWelcomeMessage();
    }
    
    private void showWelcomeMessage() {
        infoArea.setText("地铁线路查询系统\n\n" +
                         "系统功能:\n" +
                         "1. 显示中转站 - 显示所有换乘站点\n" +
                         "2. 附近站点查询 - 输入站点名和距离查询附近站点\n" +
                         "3. 查找所有路径 - 查找两站间的所有可能路径\n" +
                         "4. 查找最短路径 - 查找两站间的最短路径\n" +
                         "5. 计算票价 - 根据路径计算票价\n" +
                         "6. 重置视图 - 恢复地图原始大小和位置\n\n" +
                         "操作提示:\n" +
                         "- 使用鼠标滚轮缩放地图\n" +
                         "- 按住鼠标左键拖拽平移地图\n" +
                         "- 点击站点查看详细信息\n" +
                         "- 在搜索框输入站点名称进行搜索");
    }
    
    private void searchStation() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;
        
        for (String station : subway.getStations().keySet()) {
            if (station.contains(query)) {
                selectedStation = station;
                mapPanel.repaint();
                showStationInfo(station);
                return;
            }
        }
        
        infoArea.setText("未找到站点: " + query);
    }
    
    private void filterByLine() {
        String selectedLine = (String) lineFilter.getSelectedItem();
        if ("全部线路".equals(selectedLine)) {
            mapPanel.setHighlightedLine(null);
        } else {
            mapPanel.setHighlightedLine(selectedLine);
        }
        mapPanel.repaint();
    }
    
    private void showStationInfo(String stationName) {
        Station station = subway.getStations().get(stationName);
        if (station == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("站点: ").append(stationName).append("\n");
        sb.append("所属线路: ").append(String.join(", ", station.getLines())).append("\n");
        sb.append("是否中转站: ").append(station.isTransfer() ? "是" : "否").append("\n\n");
        
        // 显示附近站点
        Set<Map.Entry<String, Map.Entry<String, Integer>>> nearby = subway.getNearbyStations(stationName, 2000);
        if (!nearby.isEmpty()) {
            sb.append("附近站点 (2公里内):\n");
            for (Map.Entry<String, Map.Entry<String, Integer>> entry : nearby) {
                String station1 = entry.getKey();
                String line = entry.getValue().getKey();
                int distance = entry.getValue().getValue();
                sb.append(String.format(" - %s (%s): %d米\n", station1, line, distance));
            }
        } else {
            sb.append("附近2公里内没有其他站点\n");
        }
        
        infoArea.setText(sb.toString());
    }
    private class FunctionButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String command = ((JButton) e.getSource()).getText();
            
            switch (command) {
                case "显示中转站":
                    showTransferStations();
                    break;
                case "附近站点查询":
                    showNearbyStationsDialog();
                    break;
                case "查找所有路径":
                    showPathDialog(false);
                    break;
                case "查找最短路径":
                    showPathDialog(true);
                    break;
                case "计算票价":
                    showFareDialog();
                    break;
                case "重置视图":
                    resetView();
                    break;
            }
        }
    }
    
    private void showTransferStations() {
        Set<Map.Entry<String, Set<String>>> transfers = subway.getTransferStations();
        if (transfers.isEmpty()) {
            infoArea.setText("系统中没有中转站");
            return;
        }
        
        StringBuilder sb = new StringBuilder("中转站列表:\n\n");
        for (Map.Entry<String, Set<String>> entry : transfers) {
            sb.append(entry.getKey()).append(": ");
            sb.append(String.join(", ", entry.getValue()));
            sb.append("\n");
        }
        
        infoArea.setText(sb.toString());
        
        // 在地图上高亮显示中转站
        List<String> transferStations = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : transfers) {
            transferStations.add(entry.getKey());
        }
        mapPanel.setHighlightedStations(transferStations);
        mapPanel.repaint();
    }
    
    private void showNearbyStationsDialog() {
        String station = JOptionPane.showInputDialog(this, "请输入站点名称:", "附近站点查询", JOptionPane.QUESTION_MESSAGE);
        if (station == null || station.trim().isEmpty()) return;
        
        String distanceStr = JOptionPane.showInputDialog(this, "请输入查询距离(米):", "2000");
        if (distanceStr == null || distanceStr.trim().isEmpty()) return;
        
        try {
            int distance = Integer.parseInt(distanceStr);
            Set<Map.Entry<String, Map.Entry<String, Integer>>> nearby = subway.getNearbyStations(station, distance);
            
            if (nearby.isEmpty()) {
                infoArea.setText(station + " 附近" + distance + "米内没有其他站点");
                return;
            }
            
            StringBuilder sb = new StringBuilder(station + " 附近" + distance + "米内的站点:\n\n");
            for (Map.Entry<String, Map.Entry<String, Integer>> entry : nearby) {
                String stationName = entry.getKey();
                String lineName = entry.getValue().getKey();
                int stationDistance = entry.getValue().getValue();
                sb.append(String.format(" - %s (%s): %d米\n", stationName, lineName, stationDistance));
            }
            
            infoArea.setText(sb.toString());
            
            // 在地图上高亮显示
            List<String> highlight = new ArrayList<>();
            highlight.add(station);
            for (Map.Entry<String, Map.Entry<String, Integer>> entry : nearby) {
                highlight.add(entry.getKey());
            }
            mapPanel.setHighlightedStations(highlight);
            mapPanel.repaint();
            
        } catch (Exception ex) {
            infoArea.setText("错误: " + ex.getMessage());
        }
    }
    
    private void showPathDialog(boolean shortest) {
        JTextField startField = new JTextField(15);
        JTextField endField = new JTextField(15);
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("起点站:"));
        panel.add(startField);
        panel.add(new JLabel("终点站:"));
        panel.add(endField);
        
        int result = JOptionPane.showConfirmDialog(
            this, panel, 
            shortest ? "查找最短路径" : "查找所有路径", 
            JOptionPane.OK_CANCEL_OPTION
        );
        
        if (result != JOptionPane.OK_OPTION) return;
        
        String start = startField.getText().trim();
        String end = endField.getText().trim();
        
        if (start.isEmpty() || end.isEmpty()) {
            infoArea.setText("请输入起点站和终点站");
            return;
        }
        try {
            if (shortest) {
                Path path = subway.getShortestPath(start, end);
                if (path == null) {
                    infoArea.setText("未找到从 " + start + " 到 " + end + " 的路径");
                    return;
                }
                
                StringBuilder sb = new StringBuilder("最短路径: ");
                sb.append(String.join(" → ", path.getStations()));
                sb.append("\n总距离: ").append(path.getDistance()).append("米\n");
                sb.append("票价: ").append(subway.calculateFare(path)).append("元");
                
                infoArea.setText(sb.toString());
                
                // 在地图上显示路径
                mapPanel.setHighlightedPath(path.getStations());
                mapPanel.repaint();
                
            } else {
                Set<List<String>> paths = subway.getAllPaths(start, end);
                if (paths.isEmpty()) {
                    infoArea.setText("未找到从 " + start + " 到 " + end + " 的路径");
                    return;
                }
                
                StringBuilder sb = new StringBuilder("找到 " + paths.size() + " 条路径:\n\n");
                int count = 1;
                for (List<String> path : paths) {
                    sb.append("路径 ").append(count++).append(":\n");
                    sb.append(String.join(" → ", path));
                    sb.append("\n\n");
                }
                
                infoArea.setText(sb.toString());
                
                // 在地图上显示第一条路径
                if (!paths.isEmpty()) {
                    mapPanel.setHighlightedPath(paths.iterator().next());
                    mapPanel.repaint();
                }
            }
            
        } catch (Exception ex) {
            infoArea.setText("错误: " + ex.getMessage());
        }
    }
    
    private void showFareDialog() {
        JTextField pathField = new JTextField(30);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("请输入路径（用逗号分隔站点）:"));
        panel.add(pathField);
        
        int result = JOptionPane.showConfirmDialog(
            this, panel, "计算票价", JOptionPane.OK_CANCEL_OPTION
        );
        
        if (result != JOptionPane.OK_OPTION) return;
        
        String pathStr = pathField.getText().trim();
        if (pathStr.isEmpty()) return;
        
        String[] stations = pathStr.split(",");
        if (stations.length < 2) {
            infoArea.setText("路径至少需要两个站点");
            return;
        }
        
        List<String> pathList = Arrays.asList(stations);
        
        // 计算路径距离
        int totalDistance = 0;
        for (int i = 0; i < pathList.size() - 1; i++) {
            String from = pathList.get(i);
            String to = pathList.get(i+1);
            
            // 查找连接这两个站点的线路和距离
            Station station = subway.getStations().get(from);
            if (station != null) {
                for (String lineName : station.getLines()) {
                    Line line = subway.getLines().get(lineName);
                    Integer distance = line.getDistance(from, to);
                    if (distance != null) {
                        totalDistance += distance;
                        break;
                    }
                }
            }
        }
        
        Path path = new Path(pathList, totalDistance);
        int fare = subway.calculateFare(path);
        infoArea.setText("路径: " + String.join(" → ", pathList) + 
                        "\n总距离: " + totalDistance + "米" +
                        "\n票价: " + fare + "元");
    }
    
    private void resetView() {
        scale = 1.0;
        translate.setLocation(0, 0);
        mapPanel.repaint();
        infoArea.setText("视图已重置");
    }
    
    // 地图面板
    private class MapPanel extends JPanel {
        private String highlightedLine;
        private List<String> highlightedStations = new ArrayList<>();
        private List<String> highlightedPath = new ArrayList<>();
        
        public void setHighlightedLine(String line) {
            this.highlightedLine = line;
        }
        
        public void setHighlightedStations(List<String> stations) {
            this.highlightedStations = stations;
        }
        
        public void setHighlightedPath(List<String> path) {
            this.highlightedPath = path;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // 应用缩放和平移
            AffineTransform originalTransform = g2d.getTransform();
            g2d.scale(scale, scale);
            g2d.translate(translate.x, translate.y);
            
            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 绘制线路
            drawLines(g2d);
            
            // 绘制站点
            drawStations(g2d);
            
            // 绘制高亮路径
            drawHighlightedPath(g2d);
            
            // 恢复原始变换
            g2d.setTransform(originalTransform);
        }
        
        private void drawLines(Graphics2D g2d) {
            // 定义线路颜色
            Map<String, Color> lineColors = new HashMap<>();
            lineColors.put("1号线", new Color(220, 60, 50));
            lineColors.put("2号线", new Color(0, 150, 70));
            lineColors.put("3号线", new Color(150, 60, 180));
            lineColors.put("4号线", new Color(0, 100, 200));
            lineColors.put("5号线", new Color(200, 150, 0));
            lineColors.put("6号线", new Color(0, 180, 200));
            lineColors.put("7号线", new Color(180, 100, 0));
            lineColors.put("8号线", new Color(100, 50, 150));
            
            // 绘制所有线路
            for (Map.Entry<String, Line> entry : subway.getLines().entrySet()) {
                String lineName = entry.getKey();
                Line line = entry.getValue();
                
                // 如果线路被筛选，跳过其他线路
                if (highlightedLine != null && !highlightedLine.equals(lineName)) {
                    continue;
                }
                
                // 设置线路颜色
                Color color = lineColors.getOrDefault(lineName, Color.GRAY);
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(5));
                
                // 绘制线路
                List<Station> stations = line.getStations();
                for (int i = 0; i < stations.size() - 1; i++) {
                    Station s1 = stations.get(i);
                    Station s2 = stations.get(i + 1);
                    
                    Point p1 = getStationPosition(s1.getName());
                    Point p2 = getStationPosition(s2.getName());
                    
                    if (p1 != null && p2 != null) {
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
        }
        
        private void drawStations(Graphics2D g2d) {
            // 绘制所有站点
            for (Map.Entry<String, Station> entry : subway.getStations().entrySet()) {
                String stationName = entry.getKey();
                Point pos = getStationPosition(stationName);
                if (pos == null) continue;
                
                // 设置站点颜色
                if (selectedStation != null && selectedStation.equals(stationName)) {
                    g2d.setColor(new Color(255, 215, 0)); // 金色 - 选中的站点
                } else if (highlightedStations.contains(stationName)) {
                    g2d.setColor(new Color(50, 150, 250)); // 蓝色 - 高亮的站点
                } else {
                    g2d.setColor(Color.DARK_GRAY);
                }
                
                // 绘制站点
                int size = 8;
                g2d.fillOval(pos.x - size/2, pos.y - size/2, size, size);
                
                // 绘制站点名称
                if (scale > 0.5) { // 只在适当缩放级别显示名称
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                    g2d.drawString(stationName, pos.x + 10, pos.y - 10);
                }
            }
        }
        
        private void drawHighlightedPath(Graphics2D g2d) {
            if (highlightedPath.isEmpty()) return;
            
            // 设置路径样式
            g2d.setColor(new Color(255, 140, 0, 180)); // 半透明橙色
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // 绘制路径
            for (int i = 0; i < highlightedPath.size() - 1; i++) {
                Point p1 = getStationPosition(highlightedPath.get(i));
                Point p2 = getStationPosition(highlightedPath.get(i+1));
                
                if (p1 != null && p2 != null) {
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
            
            // 绘制路径上的站点
            g2d.setColor(new Color(255, 69, 0)); // 红色
            for (String station : highlightedPath) {
                Point pos = getStationPosition(station);
                if (pos != null) {
                    int size = 10;
                    g2d.fillOval(pos.x - size/2, pos.y - size/2, size, size);
                }
            }
        }
        
        private Point getStationPosition(String stationName) {
            // 在实际应用中，这里应该从数据中获取站点的真实坐标
            // 为了演示，我们使用简单的算法生成位置
            int index = 0;
            for (String name : subway.getStations().keySet()) {
                if (name.equals(stationName)) {
                    break;
                }
                index++;
            }
            
            int x = 200 + (index % 10) * 70;
            int y = 200 + (index / 10) * 60;
            return new Point(x, y);
        }
    }
    
    // 地图交互监听器
    private class MapMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            lastDragPoint = e.getPoint();
            
            // 检查是否点击了站点
            Point2D.Double mapPoint = toMapPoint(e.getPoint());
            for (String station : subway.getStations().keySet()) {
                Point pos = mapPanel.getStationPosition(station);
                if (pos != null && mapPoint.distance(pos.x, pos.y) < 10) {
                    selectedStation = station;
                    showStationInfo(station);
                    mapPanel.repaint();
                    return;
                }
            }
            selectedStation = null;
            mapPanel.repaint();
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                resetView();
            }
        }
    }
    
    private class MapMouseMotionListener extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (lastDragPoint != null) {
                Point currentPoint = e.getPoint();
                double dx = (currentPoint.x - lastDragPoint.x) / scale;
                double dy = (currentPoint.y - lastDragPoint.y) / scale;
                
                translate.x += dx;
                translate.y += dy;
                
                lastDragPoint = currentPoint;
                mapPanel.repaint();
            }
        }
    }
    
    private class MapMouseWheelListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double scaleFactor = 1.0 - e.getWheelRotation() * 0.1;
            scale *= scaleFactor;
            
            // 限制缩放范围
            scale = Math.max(0.3, Math.min(scale, 3.0));
            
            mapPanel.repaint();
        }
    }
    
    private Point2D.Double toMapPoint(Point screenPoint) {
        return new Point2D.Double(
            (screenPoint.x / scale) - translate.x,
            (screenPoint.y / scale) - translate.y
        );
    }
    
    public static void main(String[] args) {
        // 创建地铁系统并加载数据
        SubwaySystem subway = new SubwaySystem();
        try {
            subway.loadFromFile("subway.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "无法加载地铁数据: " + e.getMessage(), 
                                         "错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // 创建并显示GUI
        SwingUtilities.invokeLater(() -> {
            SubwayGUI gui = new SubwayGUI(subway);
            gui.setVisible(true);
        });
    }
}


//扩展地铁系统类以支持GUI所需的方法
