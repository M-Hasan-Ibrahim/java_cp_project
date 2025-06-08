package two_threads;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.FrameExtractor;
import method_classes.SceneChangeDetector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main_TwoThreads {
    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        long start = System.currentTimeMillis();
        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";

        // Get FPS for timestamp calculation
        VideoCapture capture = new VideoCapture(videoPath);

        double fps = capture.get(Videoio.CAP_PROP_FPS);
        System.out.println("Video captured successfully");
        System.out.println("Capture FPS = " + fps);

        // Extract all frames (single-threaded)
        FrameExtractor extractor = new FrameExtractor();

        long startTimeExtractor = System.currentTimeMillis();
        Mat[] allFrames = extractor.extract(capture);
        capture.release();

        int mid = allFrames.length / 2;
        Mat[] part1 = new Mat[mid];
        Mat[] part2 = new Mat[allFrames.length - mid];

        System.arraycopy(allFrames, 0, part1, 0, mid);
        System.arraycopy(allFrames, mid, part2, 0, part2.length);

        long endTimeExtractor = System.currentTimeMillis();
        System.out.println("Extraction took: " + (endTimeExtractor - startTimeExtractor) + " ms");
        System.out.println("Total frames extracted: " + allFrames.length);



        SceneChangeDetector detector = new SceneChangeDetector();
        int rows = 12;
        int cols = 12;
        double threshold = 25.0;

        List<Integer>[] result = new List[2];

        // Thread 1: detect in first half
        Thread t1 = new Thread(() -> result[0] = detector.detect(part1, rows, cols, threshold));

        // Thread 2: detect in second half
        Thread t2 = new Thread(() -> {
            List<Integer> temp = detector.detect(part2, rows, cols, threshold);
            for (int i = 0; i < temp.size(); i++) {
                temp.set(i, temp.get(i) + mid);  // adjust indices
            }
            result[1] = temp;
        });

        long startTimeDetector = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long endTimeDetector = System.currentTimeMillis();

        System.out.println("Detection took: " + (endTimeDetector - startTimeDetector) + " ms");

        // Combine results
        List<Integer> sceneChanges = new ArrayList<>();
        sceneChanges.addAll(result[0]);
        sceneChanges.addAll(result[1]);

        Iterator<Integer> frameIterator = sceneChanges.iterator();
        int previousFrame = frameIterator.next();
        while (frameIterator.hasNext()) {
            int currentFrame = frameIterator.next();
            if(currentFrame - previousFrame < 15){
                frameIterator.remove();
            }
            else{
                previousFrame = currentFrame;
            }
        }

        System.out.println("Scene changes detected at:");
        for (int currentFrame : sceneChanges) {
            System.out.print("→ Frame: " + currentFrame);
            System.out.println(" / -> Time: " + currentFrame /fps + "s");
        }

        long end = System.currentTimeMillis();
        System.out.println("Total Time: "+(end-start)+" ms");
    }
}
