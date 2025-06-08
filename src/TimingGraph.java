import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

public class TimingGraph {
    public static Map<Integer, List<Integer>> getMap(String filePath) {
        Map<Integer, List<Integer>> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                int threads = Integer.parseInt(parts[parts.length - 2]);
                int detectionTime = Integer.parseInt(parts[parts.length - 4]);

                map.computeIfAbsent(threads, k -> new ArrayList<>()).add(detectionTime);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    // Computes the average detection time per thread count
    public static Map<Integer, Double> averageMap(Map<Integer, List<Integer>> originalMap) {
        Map<Integer, Double> avgMap = new TreeMap<>();

        for (Map.Entry<Integer, List<Integer>> entry : originalMap.entrySet()) {
            int threads = entry.getKey();
            List<Integer> times = entry.getValue();
            double avg = times.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
            avgMap.put(threads, avg);
        }

        return avgMap;
    }

    public static void plotMap(Map<Integer, ?> map, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<Integer, ?> entry : map.entrySet()) {
            dataset.addValue(((Number) entry.getValue()).doubleValue(), "Detection Time", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                "Number of Threads",
                "Detection Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Save to 'graphs' directory
        try {
            new File("src/graphs").mkdirs(); // Create directory if not exist
            ChartUtils.saveChartAsPNG(new File("src/graphs/" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".png"), chart, 800, 600);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Display in window
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }


    public static void plotScatterPoints(List<Map.Entry<Integer, Double>> points, String title) {
        XYSeries series = new XYSeries("Detection Time");

        for (Map.Entry<Integer, Double> point : points) {
            series.add(point.getKey(), point.getValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                "Number of Threads",
                "Detection Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        try {
            new File("src/graphs").mkdirs();
            ChartUtils.saveChartAsPNG(new File("src/graphs/" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".png"), chart, 800, 600);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }



    public static void main(String[] args){
        String filePath = "src/Timing_without_queueing.txt";

        Map<Integer, List<Integer>> originalMap = getMap(filePath);
        Map<Integer, Double> avgMap = averageMap(originalMap);

        List<Map.Entry<Integer, Double>> scatterPoints = new ArrayList<>();
        originalMap.forEach((threadCount, times) -> {
            for (Integer time : times) {
                scatterPoints.add(new AbstractMap.SimpleEntry<>(threadCount, time.doubleValue()));            }
        });

        plotScatterPoints(scatterPoints, "All Detection Times for 40sec_30fps without queueing");

        // Line chart with average values
        plotMap(avgMap, "Average Detection Times for 40sec_30fps without queueing");
    }
}
