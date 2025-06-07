package N_threads;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.FrameExtractor;
import method_classes.SceneChangeDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main_NThreads {

    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of threads: ");
        int NUMBER_OF_THREADS = scanner.nextInt();


        long start = System.currentTimeMillis();

        String videoPath = "src/project_compression/myVideos/4K_video_40sec_30fps.mp4";
        VideoCapture capture = new VideoCapture(videoPath);

        double fps = capture.get(Videoio.CAP_PROP_FPS);
        System.out.println("Video captured successfully");
        System.out.println("Capture FPS = " + fps);

        FrameExtractor extractor = new FrameExtractor();
        long startTimeExtractor = System.currentTimeMillis();
        Mat[] allFrames = extractor.extract(capture);
        capture.release();
        long endTimeExtractor = System.currentTimeMillis();
        System.out.println("Extraction took: " + (endTimeExtractor - startTimeExtractor) + " ms");
        System.out.println("Total frames extracted: " + allFrames.length);
        
        int total = allFrames.length;
        int chunkSize = total / NUMBER_OF_THREADS;

        Mat[][] chunks = new Mat[NUMBER_OF_THREADS][];
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            int startIdx = i * chunkSize;
            int endIdx = (i+1) * chunkSize;
            int len = endIdx - startIdx;

            chunks[i] = new Mat[len];
            System.arraycopy(allFrames, startIdx, chunks[i], 0, len);
        }

        // Step 3: Scene detection threads
        SceneChangeDetector detector = new SceneChangeDetector();
        int rows = 12;
        int cols = 12;
        double threshold = 25.0;

        List<Integer>[] results = new List[NUMBER_OF_THREADS];
        Thread[] threads = new Thread[NUMBER_OF_THREADS];

        long startTimeDetector = System.currentTimeMillis();

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            final int index = i;
            final int offset = index * chunkSize;

            threads[i] = new Thread(() -> {
                long threadStart = System.currentTimeMillis();
                List<Integer> temp = detector.detect(chunks[index], rows, cols, threshold);
                for (int j = 0; j < temp.size(); j++) {
                    temp.set(j, temp.get(j) + offset);  // Adjust index
                }
                results[index] = temp;
                long threadEnd = System.currentTimeMillis();
                System.out.println("Thread "+index+" took: "+(threadEnd-threadStart)+" ms to finish detection");
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTimeDetector = System.currentTimeMillis();
        System.out.println("Detection took: " + (endTimeDetector - startTimeDetector) + " ms");

        // Step 4: Combine and print results
        List<Integer> allChanges = new ArrayList<>();
        for (List<Integer> list : results) {
            allChanges.addAll(list);
        }

        allChanges.sort(Integer::compareTo);
        System.out.println("Scene changes detected at:");
        for (int idx : allChanges) {
            System.out.print("→ Frame: " + idx);
            System.out.println(" / -> Time: " + idx/fps + "s");
        }

        long end = System.currentTimeMillis();
        System.out.println("Total Time: " + (end - start) + " ms");
    }
}