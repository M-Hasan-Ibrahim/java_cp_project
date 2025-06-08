package producer_consumer_model;

import org.opencv.core.Mat;

public class Frame {
    final Mat image;
    final int frameNumber;
    final double timestamp;

    Frame(Mat image, int frameNumber, double timestamp) {
        this.image = image.clone();
        this.frameNumber = frameNumber;
        this.timestamp = timestamp;
    }
}
