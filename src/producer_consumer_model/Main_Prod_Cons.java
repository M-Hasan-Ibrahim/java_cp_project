package producer_consumer_model;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import static producer_consumer_model.SceneChange.detectSceneChanges;


public class Main_Prod_Cons {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {

        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";
        VideoCapture capture = new VideoCapture(videoPath);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        int numThreads = 4;

        long start = System.currentTimeMillis();
        List<Integer> sceneChanges = detectSceneChanges(videoPath, numThreads);

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
            System.out.println(" / -> Time: " + currentFrame/fps + "s");
        }



        long end = System.currentTimeMillis();
        System.out.println("Trial took "+(end-start)+" ms");
    }
}