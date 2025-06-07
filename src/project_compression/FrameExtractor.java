package project_compression;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.concurrent.BlockingQueue;

import java.util.ArrayList;
import java.util.List;

public class FrameExtractor {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  // Load native OpenCV library
    }

    public Mat[] extract(VideoCapture cap) {
        List<Mat> frameList = new ArrayList<>();

        if (!cap.isOpened()) {
            System.err.println("Error: VideoCapture is not opened from frameExtractor.");
            return new Mat[0];
        }

        Mat frame = new Mat();

        while (cap.read(frame)) {
            frameList.add(frame.clone()); // clone() is important to avoid overwriting by OpenCV
        }

        cap.release();

        return frameList.toArray(new Mat[0]);
    }


    public Mat[] extractRange(VideoCapture cap, int count) {
        List<Mat> frames = new ArrayList<>();
        Mat frame = new Mat();

        int read = 0;
        while (read < count && cap.read(frame)) {
            frames.add(frame.clone());
            read++;
        }

        return frames.toArray(new Mat[0]);
    }

}
