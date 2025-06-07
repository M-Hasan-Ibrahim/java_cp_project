package N_threads_with_queueing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.SceneChangeDetector;

import java.util.*;
import java.util.concurrent.*;

public class Main_QueueModel {
    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    private static final int QUEUE_CAPACITY = 10;
    private static final Mat POISON_PILL = new Mat(); // empty mat as poison pill

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of threads: ");
        int PRODUCER_CONSUMER_PAIRS = scanner.nextInt()/2;

        long start = System.currentTimeMillis();

        String videoPath = "src/project_compression/myVideos/4K_video_40sec_30fps.mp4";
        VideoCapture capture = new VideoCapture(videoPath);
        double fps = capture.get(Videoio.CAP_PROP_FPS);

        if (!capture.isOpened()) {
            System.err.println("Failed to open video.");
            return;
        }

        System.out.println("Video opened, FPS = " + fps);

        BlockingQueue<Mat>[] queues = new BlockingQueue[PRODUCER_CONSUMER_PAIRS];
        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            queues[i] = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        }

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);
        ExecutorService consumers = Executors.newFixedThreadPool(PRODUCER_CONSUMER_PAIRS);

        List<Integer> allSceneChanges = Collections.synchronizedList(new ArrayList<>());

        // Start producer-consumer pairs
        for (int i = 0; i < PRODUCER_CONSUMER_PAIRS; i++) {
            final int index = i;
            final BlockingQueue<Mat> queue = queues[i];

            producers.submit(() -> {
                long startTimeProducer = System.currentTimeMillis();
                try {
                    int frameIndex = 0;
                    Mat frame = new Mat();
                    synchronized (capture) {
                        while (capture.read(frame)) {
                            queue.put(frame.clone());
                            frameIndex++;
                        }
                    }
                    queue.put(POISON_PILL); // signal end
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long endTimeProducer = System.currentTimeMillis();
                System.out.println("Producer "+index+" finished in "+(endTimeProducer-startTimeProducer)+" ms");
            });

            consumers.submit(() -> {
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
                                allSceneChanges.add(indexInVideo);
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

        // Output
        allSceneChanges.sort(Integer::compareTo);
        System.out.println("Scene changes at:");
        for (int idx : allSceneChanges) {
            System.out.print("→ Frame: " + idx);
            System.out.println(" / -> Time: " + idx/fps + "s");
        }

        long end = System.currentTimeMillis();
        System.out.println("Total Time: " +(end-start)+ " ms");
    }
}
