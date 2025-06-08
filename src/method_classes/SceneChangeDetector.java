package method_classes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

public class SceneChangeDetector {
    private final FrameSplitter splitter = new FrameSplitter();

    public List<Integer> detect(Mat[] frames, int rows, int cols, double threshold) {
        List<Integer> sceneChanges = new ArrayList<>();

        if (frames.length < 2) return sceneChanges;

        for (int i = 1; i < frames.length; i++) {
            Mat prev = frames[i - 1];
            Mat curr = frames[i];

            boolean isChange = isSceneChange(prev, curr, rows, cols, threshold);
            if (isChange) {
                sceneChanges.add(i);
            }
        }

        return sceneChanges;
    }


    public boolean isSceneChange(Mat frame1, Mat frame2, int rows, int cols, double threshold) {
        List<Mat> tiles1 = splitter.split(frame1, rows, cols);
        List<Mat> tiles2 = splitter.split(frame2, rows, cols);

        double totalDifference = 0;

        for (int i = 0; i < tiles1.size(); i++) {
            Mat diff = new Mat();
            Core.absdiff(tiles1.get(i), tiles2.get(i), diff);
            Scalar sum = Core.sumElems(diff);
            totalDifference += sum.val[0] + sum.val[1] + sum.val[2];  // B + G + R
        }

        // Normalize difference over total number of pixels
        double averageDiff = totalDifference / (frame1.rows() * frame1.cols() * 3); // 3 = BGR channels

        return averageDiff > threshold;
    }
}
