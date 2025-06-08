package method_classes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

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
            frameList.add(frame.clone());
        }

        cap.release();

        return frameList.toArray(new Mat[0]);
    }

}
