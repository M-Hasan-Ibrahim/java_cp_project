package four_threads;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.FrameExtractor;
import method_classes.SceneChangeDetector;

import java.util.ArrayList;
import java.util.List;

public class Main_FourThreads {
    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        long start = System.currentTimeMillis();
        String videoPath = "src/project_compression/myVideos/4K_video_40sec_30fps.mp4";

        VideoCapture capture = new VideoCapture(videoPath);

        double fps = capture.get(Videoio.CAP_PROP_FPS);
        System.out.println("Video captured successfully");
        System.out.println("Capture FPS = " + fps);

        // Step 1: Extract all frames (single-threaded)
        FrameExtractor extractor = new FrameExtractor();
        long startTimeExtractor = System.currentTimeMillis();
        Mat[] allFrames = extractor.extract(capture);
        capture.release();
        long endTimeExtractor = System.currentTimeMillis();
        System.out.println("Extraction took: " + (endTimeExtractor - startTimeExtractor) + " ms");
        System.out.println("Total frames extracted: " + allFrames.length);

        // Step 2: Split frames into 4 parts
        int total = allFrames.length;
        int q1 = total / 4;
        int q2 = q1 * 2;
        int q3 = q1 * 3;

        Mat[] part1 = new Mat[q1];
        Mat[] part2 = new Mat[q1];
        Mat[] part3 = new Mat[q1];
        Mat[] part4 = new Mat[total - q3]; // handle remainder here

        System.arraycopy(allFrames, 0, part1, 0, q1);
        System.arraycopy(allFrames, q1, part2, 0, q1);
        System.arraycopy(allFrames, q2, part3, 0, q1);
        System.arraycopy(allFrames, q3, part4, 0, total - q3);

        // Step 3: Scene change detection (4 threads)
        SceneChangeDetector detector = new SceneChangeDetector();
        int rows = 12, cols = 12;
        double threshold = 25.0;

        List<Integer>[] result = new List[4];

        Thread t1 = new Thread(() -> result[0] = detector.detect(part1, rows, cols, threshold));

        Thread t2 = new Thread(() -> {
            List<Integer> temp = detector.detect(part2, rows, cols, threshold);
            for (int i = 0; i < temp.size(); i++) {
                temp.set(i, temp.get(i) + q1);
            }
            result[1] = temp;
        });

        Thread t3 = new Thread(() -> {
            List<Integer> temp = detector.detect(part3, rows, cols, threshold);
            for (int i = 0; i < temp.size(); i++) {
                temp.set(i, temp.get(i) + q2);
            }
            result[2] = temp;
        });

        Thread t4 = new Thread(() -> {
            List<Integer> temp = detector.detect(part4, rows, cols, threshold);
            for (int i = 0; i < temp.size(); i++) {
                temp.set(i, temp.get(i) + q3);
            }
            result[3] = temp;
        });

        long startTimeDetector = System.currentTimeMillis();
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        long endTimeDetector = System.currentTimeMillis();

        System.out.println("Detection took: " + (endTimeDetector - startTimeDetector) + " ms");

        // Step 4: Combine and print results
        List<Integer> allChanges = new ArrayList<>();
        allChanges.addAll(result[0]);
        allChanges.addAll(result[1]);
        allChanges.addAll(result[2]);
        allChanges.addAll(result[3]);

        System.out.println("Scene changes detected at:");
        for (int idx : allChanges) {
            System.out.print("→ Frame: " + idx);
            System.out.println(" / -> Time: " + idx/fps + "s");
        }

        long end = System.currentTimeMillis();
        System.out.println("Total Time: "+(end-start)+" ms");
    }
}