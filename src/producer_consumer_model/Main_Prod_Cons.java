package producer_consumer_model;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static producer_consumer_model.SceneChange.detectSceneChanges;


public class Main_Prod_Cons {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static void writeInFile(String filePath, String message){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))){
            writer.write(message);
            writer.newLine();
        }catch (IOException e) {
            e.printStackTrace();
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            lines.sort(Comparator.comparingInt(line -> {
                String[] parts = line.trim().split(" ");
                try {
                    return Integer.parseInt(parts[parts.length - 2]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Skipping line due to format issue: " + line);
                    return Integer.MAX_VALUE;
                }
            }));
            Files.write(Paths.get(filePath), lines);
        } catch (IOException e) {
            System.err.println("Error while reading or writing the file:");
            e.printStackTrace();
        }
    }

    public static void Runner(String videoPath, int numThreads) {


        VideoCapture capture = new VideoCapture(videoPath);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        capture.release();


        long start = System.currentTimeMillis();
        List<Integer> sceneChanges = detectSceneChanges(videoPath, numThreads);

        long end = System.currentTimeMillis();
        System.out.println("Detection Took " +(end-start)+ " ms");
        System.out.println("Output filtering starting now...");
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
            System.out.println(" / -> Time: " + currentFrame/fps + " s");
        }



        end = System.currentTimeMillis();
        System.out.println("Trial took "+(end-start)+" ms");


        String filePath = "src/producer_consumer_model/results/PCM_40sec_30fps.txt";
        String message = "Trial took " +(end-start)+ " ms using " +numThreads+ " threads";
        writeInFile(filePath, message);

        System.gc();
    }

    public static void main(String[] args) throws InterruptedException {
        String videoPath = "src/myVideos/4K_video_40sec_30fps.mp4";

        int numThreads = 6;
        Runner(videoPath, numThreads);

        for(int i = 6; i <= 8; i+=2){
            for(int j = 0; j < 2; j++){
                System.out.printf(">>> Running with %d threads (run %d)...%n", i, j);
                Runner(videoPath, i);
                System.gc();
                TimeUnit.SECONDS.sleep(15);
                System.out.println();
            }
            TimeUnit.SECONDS.sleep(15);
            System.out.println();
        }

        for(int i = 4; i >= 2; i-=2){
            for(int j = 0; j < 2; j++){
                System.out.printf(">>> Running with %d threads (run %d)...%n", i, j);
                Runner(videoPath, i);
                System.gc();
                TimeUnit.SECONDS.sleep(15);
                System.out.println();
            }
            TimeUnit.SECONDS.sleep(15);
            System.out.println();
        }

    }
}