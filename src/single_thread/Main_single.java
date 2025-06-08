package single_thread;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import method_classes.FrameExtractor;
import method_classes.SceneChangeDetector;

import java.util.Iterator;
import java.util.List;

public class Main_single {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        long start = System.currentTimeMillis();

        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";
        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            System.err.println("Error: VideoCapture is not opened from main.");
            return;
        }

        double fps = capture.get(Videoio.CAP_PROP_FPS);
        System.out.println("Video captured successfully");
        System.out.println("Capture FPS = " + fps);

        FrameExtractor extractor = new FrameExtractor();

        long startTimeExtractor = System.currentTimeMillis();
        Mat[] frames = extractor.extract(capture);
        capture.release();
        long endTimeExtractor = System.currentTimeMillis();
        System.out.println("Extraction Took: " + (endTimeExtractor - startTimeExtractor) + " ms");
        System.out.println("Frames extracted: " + frames.length);

        if (frames.length > 1) {
            SceneChangeDetector detector = new SceneChangeDetector();

            int rows = 12;
            int cols = 12;
            double threshold = 25.0;

            long startTimeDetector = System.currentTimeMillis();

            List<Integer> sceneChanges = detector.detect(frames, rows, cols, threshold);

            long endTimeDetector = System.currentTimeMillis();
            System.out.println("Detection Took: " + (endTimeDetector - startTimeDetector) + " ms");


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
            for (int detectedFrame : sceneChanges) {
                System.out.print("→ Frame: " + detectedFrame);
                System.out.println(" / -> Time: " + detectedFrame / fps + "s");
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Total Time: " + (end - start) + " ms");
    }
}
