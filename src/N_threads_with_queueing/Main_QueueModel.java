package N_threads_with_queueing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.SceneChangeDetector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class Main_QueueModel {
    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    private static final int QUEUE_CAPACITY = 100;
    private static final Mat POISON_PILL = new Mat(); // empty mat as poison pill

    public static void Runner(int Threads_Count, String videoPath) throws InterruptedException {

        int NUMBER_OF_THREADS = Threads_Count;
        int PRODUCER_CONSUMER_PAIRS = NUMBER_OF_THREADS/2;
        if(NUMBER_OF_THREADS%2!=0){
            System.out.println("Number of threads is odd, it will be considered that the number of threads is " + (NUMBER_OF_THREADS-1));
            NUMBER_OF_THREADS = NUMBER_OF_THREADS - 1;
        }

        long start = System.currentTimeMillis();


        VideoCapture capture = new VideoCapture(videoPath);
        if(!capture.isOpened()){
            System.err.println("Video Capture Coudn't Start...");
            return;
        }
        double totalFrames = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        capture.release();

        int framesPerProducer = (int) Math.ceil(totalFrames / PRODUCER_CONSUMER_PAIRS);


        System.out.println("Video opened, FPS = " + fps + ", and Frames = " + totalFrames);

        BlockingQueue<Mat>[] queues = new BlockingQueue[PRODUCER_CONSUMER_PAIRS];
        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            queues[i] = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        }

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);
        ExecutorService consumers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);

        List<Integer> sceneChanges = Collections.synchronizedList(new ArrayList<>());

        // Start producer-consumer pairs
        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            final int index = i;
            final BlockingQueue<Mat> queue = queues[i];

            producers.submit(() -> {
                long startTimeProducer = System.currentTimeMillis();

                int startFrame = index * framesPerProducer;
                int endFrame = Math.min((index + 1) * framesPerProducer, (int) totalFrames);
                System.out.println("Producer " + index + " started with " + (endFrame-startFrame) + " frames to process");


                VideoCapture localCapture = new VideoCapture(videoPath);
                if (!localCapture.isOpened()) {
                    System.err.println("Producer " + index + ": Failed to open video.");
                    return;
                }

                localCapture.set(Videoio.CAP_PROP_POS_FRAMES, startFrame);
                int frameIndex = startFrame;
                Mat frame = new Mat();

                try {
                    while (frameIndex < endFrame && localCapture.read(frame)) {
                        queue.put(frame.clone());
                        frameIndex++;
                    }
                    queue.put(POISON_PILL);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    localCapture.release();
                }

                long endTimeProducer = System.currentTimeMillis();
                System.out.println("Producer " + index + " finished in " + (endTimeProducer - startTimeProducer) + " ms");
            });

            consumers.submit(() -> {
                System.out.println("Consumer "+index+" started");
                long startTimeConsumer = System.currentTimeMillis();

                SceneChangeDetector detector = new SceneChangeDetector();
                try {
                    Mat prev = null;
                    int indexInVideo = 0;
                    while (true) {
                        Mat current = queue.take();
                        if (current == POISON_PILL) break;

                        if (prev != null) {
                            boolean changed = detector.isSceneChange(prev, current, 12, 12, 25.0);
                            if (changed) {
                                sceneChanges.add(indexInVideo+(index*framesPerProducer));
                            }
                        }

                        prev = current;
                        indexInVideo++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long endTimeConsumer = System.currentTimeMillis();
                System.out.println("Consumer "+index+" finished in "+(endTimeConsumer - startTimeConsumer)+" ms");
            });
        }

        producers.shutdown();
        consumers.shutdown();

        producers.awaitTermination(1, TimeUnit.HOURS);
        consumers.awaitTermination(1, TimeUnit.HOURS);
        capture.release();

        sceneChanges.sort(Integer::compareTo);

        Iterator<Integer> frameIterator = sceneChanges.iterator();
        int previousFrame = frameIterator.next();
        while (frameIterator.hasNext()) {
            int currentFrame = frameIterator.next();
            if(currentFrame - previousFrame <= 20){
                frameIterator.remove();
            }
            else{
                previousFrame = currentFrame;
            }
        }


        System.out.println("Scene changes at:");
        for (int currentFrame : sceneChanges) {
            System.out.print("→ Frame: " + currentFrame);
            System.out.println(" / -> Time: " + currentFrame /fps + "s");
        }

        long end = System.currentTimeMillis();
        long duration = end-start;
        System.out.println("Total Time: " + duration + " ms");

        String filePath = "src/Timing_with_queueing_40sec_30fps.txt";
        String message = "Trial took " + duration + " ms using " +NUMBER_OF_THREADS+ " threads";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void main(String[] args) throws InterruptedException{
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("Enter number of threads: ");
//        int NUMBER_OF_THREADS = scanner.nextInt();
//        int PRODUCER_CONSUMER_PAIRS = NUMBER_OF_THREADS/2;

        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";

        Runner(4, videoPath);
    }
}
