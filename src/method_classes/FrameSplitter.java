package method_classes;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;
import java.util.ArrayList;

public class FrameSplitter {

    public List<Mat> split(Mat frame, int rows, int cols) {
        List<Mat> tiles = new ArrayList<>();
        int tileHeight = frame.rows() / rows;
        int tileWidth = frame.cols() / cols;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int y = r * tileHeight;
                int x = c * tileWidth;
                Rect rect = new Rect(x, y, tileWidth, tileHeight);
                tiles.add(new Mat(frame, rect));
            }
        }
        return tiles;
    }


}
