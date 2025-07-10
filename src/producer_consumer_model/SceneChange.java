package producer_consumer_model;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SceneChange {
//    final int frameNumber;
//    final double timestamp;
//    final double difference;
//
//    SceneChange(int frameNumber, double timestamp, double difference) {
//        this.frameNumber = frameNumber;
//        this.timestamp = timestamp;
//        this.difference = difference;
//    }

    public static List<Integer> detectSceneChanges(String videoPath, int numThreads) {
        VideoCapture cap = new VideoCapture(videoPath);

        if (!cap.isOpened()) {
            cap.release();
            return new ArrayList<>();
        }



        double fps = cap.get(Videoio.CAP_PROP_FPS);
        System.out.println("Video Opened: FPS = "+fps);

        List<Integer> sceneChangeFrames = Collections.synchronizedList(new ArrayList<>());


        BlockingQueue<Frame> frameQueue = new ArrayBlockingQueue<>(100);


        AtomicBoolean producerDone = new AtomicBoolean(false);
        AtomicInteger activeConsumers = new AtomicInteger(numThreads-1);

        // Executor for consumer threads
        //ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)){
            // Producer thread - reads frames from video
            executor.submit(() -> {
                Mat frame = new Mat();
                int frameNum = 0;

                try {
                    while (cap.read(frame)) {
                        double timestamp = frameNum / fps;
                        frameQueue.put(new Frame(frame, frameNum, timestamp));
                        frameNum++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producerDone.set(true);
                    cap.release();
                }
            });

            // Consumer threads - process frames for scene changes
            for (int i = 0; i < numThreads-1; i++) {
                executor.submit(() -> {
                    Mat prevGray = null;

                    try {
                        while (true) {
                            Frame frame = frameQueue.poll(1, TimeUnit.SECONDS);

                            if (frame == null) {
                                if (producerDone.get() && frameQueue.isEmpty()) {
                                    break;
                                }
                                continue;
                            }

                            // Convert to grayscale for comparison
                            Mat gray = new Mat();
                            if (frame.image.channels() > 1) {
                                Imgproc.cvtColor(frame.image, gray, Imgproc.COLOR_BGR2GRAY);
                            } else {
                                gray = frame.image.clone();
                            }

                            // Compare with previous frame
                            if (prevGray != null) {
                                double difference = calculateFrameDifference(prevGray, gray);

                                // Threshold for scene change detection
                                if (difference > 30.0) {
                                    sceneChangeFrames.add(frame.frameNumber);
                                }
                                prevGray.release();
                            }
                            prevGray = gray;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        if (prevGray != null) {
                            prevGray.release();
                        }
                        activeConsumers.decrementAndGet();
                    }
                });
            }

            // Shutdown executor and wait for completion
            executor.shutdown();

            boolean terminated = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if(!terminated){
                System.err.println("Timeout elapsed before termination! Some tasks may not have finished.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Sort the results before returning
        sceneChangeFrames.sort(Integer::compareTo);
        return sceneChangeFrames;
    }


    private static double calculateFrameDifference(Mat frame1, Mat frame2) {
        if (frame1.size().equals(frame2.size())) {
            Mat diff = new Mat();
            Core.absdiff(frame1, frame2, diff);

            Scalar meanDiff = Core.mean(diff);
            diff.release();

            return meanDiff.val[0];
        }
        return 0.0;
    }
}

