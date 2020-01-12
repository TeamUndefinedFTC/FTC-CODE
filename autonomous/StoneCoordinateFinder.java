package org.firstinspires.ftc.teamcode.autonomous;

import com.disnodeteam.dogecv.CameraViewDisplay;
import com.disnodeteam.dogecv.detectors.DogeCVDetector;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import detectors.FoundationPipeline.Pipeline;
import detectors.FoundationPipeline.SkyStone;

@Autonomous(name = "Stone Coordinate Finder", group = "Auto")
public class StoneCoordinateFinder extends OpMode {

    private DogeCVDetector detector = new DogeCVDetector() {
        @Override
        public Mat process(Mat rgba) {

        	/*
        	    Here you can specify which elements are being detected.
        	    At the moment, SkyStones are super-reliable, Individual stones can be detected if you set up
        	    the camera right, and Foundations...need work.

        	 */
            Pipeline.doFoundations=false;
            Pipeline.doStones=true;
            Pipeline.doSkyStones=true;


            Mat m = Pipeline.process(rgba);

            telemetry.update();

            Imgproc.resize(m, m, new Size(640*1.3, 480*1.3 ));
            return m;
        }
        @Override
        public void useDefaults() {}
    };

    @Override
    public void init() {
        telemetry.setAutoClear(true);
        // Set up detector
        detector.init(hardwareMap.appContext, CameraViewDisplay.getInstance()); // Initialize it with the app context and camera
        detector.enable();

    }
    /*
     * Code to run REPEATEDLY when the driver hits INIT
     */
    @Override
    public void init_loop() {
    }

    /*
     * Code to run ONCE when the driver hits PLAY
     */
    @Override
    public void start() {

    }

    /*
     * Code to run REPEATEDLY when the driver hits PLAY
     */
    @Override
    public void loop() {
        if (Pipeline.skyStones != null && Pipeline.skyStones.size() > 0) {
            List<SkyStone> stoneList = Pipeline.skyStones;

            int stoneCount = stoneList.size();
            for (int i = 0; i < stoneCount; i++) {
                try {
                    telemetry.addData("Stone " + (i + 1) + " X:", stoneList.get(i).x);
                } catch (Exception e){}
            }
        }

        telemetry.update();
    }

    /*
     * Code to run ONCE after the driver hits STOP
     */
    @Override
    public void stop() {
        if(detector != null) detector.disable(); //Make sure to run this on stop!
    }

    public static void clean(){
        System.gc();
    }
}