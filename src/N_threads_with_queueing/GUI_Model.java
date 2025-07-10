package N_threads_with_queueing;

import method_classes.SceneChangeDetector;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class GUI_Model {
    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static String Runner(int Threads_Count, String videoPath) throws InterruptedException {
        StringBuilder output = new StringBuilder();

        int NUMBER_OF_THREADS = Threads_Count;
        int PRODUCER_CONSUMER_PAIRS = NUMBER_OF_THREADS/2;
        if (NUMBER_OF_THREADS % 2 != 0) {
            output.append("Number of threads is odd, it will be considered that the number of threads is ")
                    .append(NUMBER_OF_THREADS - 1).append("\n");
            NUMBER_OF_THREADS = NUMBER_OF_THREADS - 1;
        }

        System.out.println("processing started");

        long start = System.currentTimeMillis();

        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            output.append("Video Capture Couldn't Start...\n");
            return output.toString();
        }
        double totalFrames = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        capture.release();

        int framesPerProducer = (int) Math.ceil(totalFrames / PRODUCER_CONSUMER_PAIRS);

        output.append("Video opened, FPS = ").append(fps).append(", and Frames = ").append(totalFrames).append("\n");

        BlockingQueue<Mat>[] queues = new BlockingQueue[PRODUCER_CONSUMER_PAIRS];
        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            queues[i] = new ArrayBlockingQueue<>(69); // QUEUE_CAPACITY
        }

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);
        ExecutorService consumers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);

        List<Integer> sceneChanges = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            final int index = i;
            final BlockingQueue<Mat> queue = queues[i];

            producers.submit(() -> {
                long startTimeProducer = System.currentTimeMillis();

                int startFrame = index * framesPerProducer;
                int endFrame = Math.min((index + 1) * framesPerProducer, (int) totalFrames);

                VideoCapture localCapture = new VideoCapture(videoPath);
                if (!localCapture.isOpened()) {
                    synchronized (output) {
                        output.append("Producer ").append(index).append(": Failed to open video.\n");
                    }
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
                    queue.put(new Mat()); // POISON_PILL
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    localCapture.release();
                }

                long endTimeProducer = System.currentTimeMillis();
                synchronized (output) {
                    output.append("Producer ").append(index)
                            .append(" finished in ").append(endTimeProducer - startTimeProducer).append(" ms\n");
                }
            });

            consumers.submit(() -> {
                long startTimeConsumer = System.currentTimeMillis();

                SceneChangeDetector detector = new SceneChangeDetector();
                try {
                    Mat prev = null;
                    int indexInVideo = 0;
                    while (true) {
                        Mat current = queue.take();
                        if (current.empty()) break; // POISON_PILL
                        if (prev != null) {
                            boolean changed = detector.isSceneChange(prev, current, 12, 12, 25.0);
                            if (changed) {
                                sceneChanges.add(indexInVideo + (index * framesPerProducer));
                            }
                        }
                        prev = current;
                        indexInVideo++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long endTimeConsumer = System.currentTimeMillis();
                synchronized (output) {
                    output.append("Consumer ").append(index)
                            .append(" finished in ").append(endTimeConsumer - startTimeConsumer).append(" ms\n");
                }
            });
        }

        producers.shutdown();
        consumers.shutdown();

        producers.awaitTermination(1, TimeUnit.HOURS);
        consumers.awaitTermination(1, TimeUnit.HOURS);

        sceneChanges.sort(Integer::compareTo);

        if (!sceneChanges.isEmpty()) {
            Iterator<Integer> frameIterator = sceneChanges.iterator();
            int previousFrame = frameIterator.next();
            while (frameIterator.hasNext()) {
                int currentFrame = frameIterator.next();
                if (currentFrame - previousFrame <= 20) {
                    frameIterator.remove();
                } else {
                    previousFrame = currentFrame;
                }
            }
        }

        output.append("\nScene changes at:\n");
        for (int currentFrame : sceneChanges) {
            output.append("→ Frame: ").append(currentFrame)
                    .append(" / -> Time: ").append(String.format("%.2f", currentFrame / fps)).append("s\n");
        }

        long end = System.currentTimeMillis();
        long duration = end - start;
        output.append("\nTotal Time: ").append(duration)
                .append(" ms using ").append(NUMBER_OF_THREADS).append(" threads\n");

        return output.toString();
    }

    public static void main(String[] args) throws InterruptedException{
        System.out.println(Runner(4, "src/myVideos/4K_video_15.mp4"));
    }


}
