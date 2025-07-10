package N_threads;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.FrameExtractor;
import method_classes.SceneChangeDetector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main_NThreads {

    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of threads: ");
        int NUMBER_OF_THREADS = scanner.nextInt();


        long start = System.currentTimeMillis();

        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";
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
                System.out.println("Thread "+index+" has started");
                long threadStart = System.currentTimeMillis();

                List<Integer> temp = detector.detect(chunks[index], rows, cols, threshold);
                for (int j = 0; j < temp.size(); j++) {
                    temp.set(j, temp.get(j) + offset);  // Adjust index
                }
                results[index] = temp;

                long threadEnd = System.currentTimeMillis();
                System.out.println("Thread "+index+" took "+(threadEnd-threadStart)+" ms to finish detection");
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTimeDetector = System.currentTimeMillis();
        long durationDetection = endTimeDetector-startTimeDetector;
        System.out.println("Detection took: " + durationDetection + " ms");

        List<Integer> sceneChanges = new ArrayList<>();
        for (List<Integer> list : results) {
            sceneChanges.addAll(list);
        }

        sceneChanges.sort(Integer::compareTo);

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
        long duration = end-start;
        System.out.println("Total Time: " + duration + " ms");

        String filePath = "src/Timing_without_queueing.txt";
        String message = "Trial took " + duration + " ms, and the detection time was " +durationDetection+ " using " + NUMBER_OF_THREADS + " threads";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error while writing to the file:");
            e.printStackTrace();
            return;
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            lines.sort(Comparator.comparingInt(line -> {
                String[] parts = line.trim().split(" ");
                try {
                    return Integer.parseInt(parts[parts.length - 2]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Skipping line due to format issue: " + line);
                    return Integer.MAX_VALUE;
                }
            }));
            Files.write(Paths.get(filePath), lines);
        } catch (IOException e) {
            System.err.println("Error while reading or writing the file:");
            e.printStackTrace();
        }


    }
}