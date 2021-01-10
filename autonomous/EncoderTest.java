package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;

import org.firstinspires.ftc.robotcontroller.external.samples.HardwarePushbot;

/**
 * This file illustrates the concept of driving a path based on encoder counts.
 * It uses the common Pushbot hardware class to define the drive on the robot.
 * The code is structured as a LinearOpMode
 * <p>
 * The code REQUIRES that you DO have encoders on the wheels,
 * otherwise you would use: PushbotAutoDriveByTime;
 * <p>
 * This code ALSO requires that the drive Motors have been configured such that a positive
 * power command moves them forwards, and causes the encoders to count UP.
 * <p>
 * The desired path in this example is:
 * - Drive forward for 48 inches
 * - Spin right for 12 Inches
 * - Drive Backwards for 24 inches
 * - Stop and close the claw.
 * <p>
 * The code is written using a method called: encoderDrive(speed, leftInches, rightInches, timeoutS)
 * that performs the actual movement.
 * This methods assumes that each movement is relative to the last stopping place.
 * There are other ways to perform encoder based moves, but this method is probably the simplest.
 * This code uses the RUN_TO_POSITION mode to enable the Motor controllers to generate the run profile
 * <p>
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@Autonomous(name = "EncoderandCameraTest", group = "Linear OpMode")
//@Disabled
public class CameraDetection extends LinearOpMode {

    private static final String TFOD_MODEL_ASSET = "UltimateGoal.tflite";
    private static final String LABEL_FIRST_ELEMENT = "Quad";
    private static final String LABEL_SECOND_ELEMENT = "Single";

    private static final String VUFORIA_KEY =
            "AeflKlH/////AAABmQAedZNXCkKmqQ2CkC55GVkZGoxK0YlVMNeDwQgN5B9Jq26R9J8TZ0qlrBQVz2o3vEgIjMfV8rZF2Z7PPxZJnScBap/Jh2cxT0teLCWkuBk/mZzWC0bRjhpwT0JkU3AGpztJHL4oJZDEaf4fUDilG1NdczNT5V8nL/ZraZzRZvGBwYO7q42b32DKKb+05OemiCOCx34h0qq0lkahDKKO7k1UTpznzyK33IPVtvutSgGvdrpNe/Jv5ApIvHcib4bKom7XVqf800+Adi0bDD94NSWFeJq+i/IZnJJqH9iXXdl3Qjptri6irrciVJtmjtyZCnFB0n4ni90VmmDb5We3Dvft6wjdPrVO5UVAotWZJAnr";

    private VuforiaLocalizer vuforia;

    private TFObjectDetector tfod;

    private ElapsedTime runtime = new ElapsedTime();

    // moving motors
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    // sweeping motors
    private DcMotor wheelRack;
    private DcMotor belt;

    // launching motor
    private DcMotor launcher;

    // servos
    private Servo wobbleArm;
    private Servo launcherPush;  // moves the rings into the launching motor
    private Servo claw;

    double launcherPower = 0.75;

    static final double COUNTS_PER_MOTOR_REV = 383.6;    // eg: TETRIX Motor Encoder
    static final double DRIVE_GEAR_REDUCTION = 1.0;     // This is < 1.0 if geared UP... maybe 1??
    static final double WHEEL_DIAMETER_INCHES = 10 / 2.54;     // For figuring circumference
    static final double COUNTS_PER_INCH = (12 / 9.6)/* (12/10.25) */ * (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.1415);
    static final double DRIVE_SPEED = .1;
    static final double TURN_SPEED = 0.5;

    @Override
    public void runOpMode() {
        initVuforia();
        initTfod();

        /**
         * Activate TensorFlow Object Detection before we wait for the start command.
         * Do it here so that the Camera Stream window will have the TensorFlow annotations visible.
         **/
        if (tfod != null) {
            tfod.activate();
        }

        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");

        launcher = hardwareMap.get(DcMotor.class, "launcher");
        launcherPush = hardwareMap.get(Servo.class, "launcherPush");

        wheelRack = hardwareMap.get(DcMotor.class, "wheelRack");
        belt = hardwareMap.get(DcMotor.class, "belt");

        //arm is god servo
        wobbleArm = hardwareMap.get(Servo.class, "arm");
        claw = hardwareMap.get(Servo.class, "claw");

        wobbleArm.setPosition(1);
        wobbleArm.setPosition(wobbleArm.getPosition());

        claw.setPosition(1);

        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        //put encoder stuff IN HERE; while opModeisActive is for everything

        goForward(0.5);
        sleep(1300);

        stopMotors();
        sleep(200);

        String objectsFound = "None";

        for (int j = 0; j < 5000000; j++) {
            if (tfod != null) { //tfod !=null is just for the camera thing
                // getUpdatedRecognitions() will return null if no new information is available since
                // the last time that call was made.
                List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                if (updatedRecognitions != null) {
                    telemetry.addData("# Object Detected", updatedRecognitions.size());
                    // step through the list of recognitions and display boundary info.
                    int i = 0;
                    for (Recognition recognition : updatedRecognitions) {
                        telemetry.addData(String.format("label (%d)", i), recognition.getLabel());
                        telemetry.addData(String.format("  left,top (%d)", i), "%.03f , %.03f",
                                recognition.getLeft(), recognition.getTop());
                        telemetry.addData(String.format("  right,bottom (%d)", i), "%.03f , %.03f",
                                recognition.getRight(), recognition.getBottom());
                        objectsFound = recognition.getLabel();
                    }
                    telemetry.update();
                }
            }
        }

        // moving to the correct square based on the amount of rings
        if (objectsFound.equals("None")) {
            goForward(0.5);
            sleep(1100);

            while (wobbleArm.getPosition() < .9) {
                wobbleArm.setPosition(wobbleArm.getPosition() + 0.0055);
            }

            claw.setPosition(0.5);
            sleep(500);
            //encoderDrive(DRIVE_SPEED, -37, -37, -37, -37, 5.0);
        } else if (objectsFound.equals("Single")) {
            goForward(0.5);
            sleep(2100);
            strafeLeft(0.5);
            sleep(1300);

            stopMotors();
            sleep(200);

            while (wobbleArm.getPosition() < .9) {
                wobbleArm.setPosition(wobbleArm.getPosition() + 0.0055);
            }

            claw.setPosition(0.5);
            sleep(500);

            goBackward(0.5);
            sleep(650);
            //encoderDrive(DRIVE_SPEED, -67, -67, -67, -67, 5.0);
            //encoderDrive(DRIVE_SPEED, 20, -20, -20, 20, 5.0); // strafing left
        } else {
            goForward(0.5);
            sleep(3100);

            stopMotors();
            sleep(200);

            while (wobbleArm.getPosition() < .9) {
                wobbleArm.setPosition(wobbleArm.getPosition() + 0.0055);
            }

            claw.setPosition(0.5);
            sleep(500);

            goBackward(0.5);
            sleep(1650);
            //encoderDrive(DRIVE_SPEED, -95, -95, -95, -95, 5.0);
        }

        stopMotors();
        sleep(500);     // pause for servos to move

        telemetry.addData("Path", "Complete");
        telemetry.update();

        if (tfod != null) {
            tfod.shutdown();
        }
    }



    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the TensorFlow Object Detection engine.
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.8f;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABEL_FIRST_ELEMENT, LABEL_SECOND_ELEMENT);
    }

    /*
     *  Method to perform a relative move, based on encoder counts.
     *  Encoders are not reset as the move is based on the current position.
     *  Move will stop if any of three conditions occur:
     *  1) Move gets to the desired position
     *  2) Move runs out of time
     *  3) Driver stops the opmode running.
     */
    public void encoderDrive(double speed,
                             double frontLeftInches, double frontRightInches, double backLeftInches, double backRightInches,
                             double timeoutS) {
        int newFrontLeftTarget;
        int newFrontRightTarget;
        int newBackLeftTarget;
        int newBackRightTarget;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            //DUE TO ORIENTATION OF MOTORS, LEFT MOTORS HAVE TO HAVE SIGN REVERSED FOR DISTANCES. maybe.
            newFrontLeftTarget = frontLeft.getCurrentPosition() + (int) (-frontLeftInches * COUNTS_PER_INCH);
            newFrontRightTarget = frontRight.getCurrentPosition() + (int) (frontRightInches * COUNTS_PER_INCH);
            newBackLeftTarget = backLeft.getCurrentPosition() + (int) (-backLeftInches * COUNTS_PER_INCH);
            newBackRightTarget = backRight.getCurrentPosition() + (int) (backRightInches * COUNTS_PER_INCH);
            frontLeft.setTargetPosition(newFrontLeftTarget);
            frontRight.setTargetPosition(newFrontRightTarget);
            backLeft.setTargetPosition(newBackLeftTarget);
            backRight.setTargetPosition(newBackRightTarget);

            // Turn On RUN_TO_POSITION
            frontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            frontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            backLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            backRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // reset the timeout time and start motion.
            runtime.reset();
            frontLeft.setPower(speed);
            frontRight.setPower(speed);
            backLeft.setPower(speed);
            backRight.setPower(speed);

            // keep looping while we are still active, and there is time left, and both motors are running.
            // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
            // its target position, the motion will stop.  This is "safer" in the event that the robot will
            // always end the motion as soon as possible.
            // However, if you require that BOTH motors have finished their moves before the robot continues
            // onto the next step, use (isBusy() || isBusy()) in the loop test.
            while (opModeIsActive() &&
                    (runtime.seconds() < timeoutS) &&
                    (frontLeft.isBusy() && frontRight.isBusy())) { //might need to change this to all being busy..

                // Display it for the driver.
                telemetry.addData("Path1", "Running to %7d :%7d : %7d : %7d", newFrontLeftTarget, newFrontRightTarget, newBackLeftTarget, newBackRightTarget);
                telemetry.addData("Path2", "Running at %7d :%7d : %7d : %7d",
                        frontLeft.getCurrentPosition(),
                        frontRight.getCurrentPosition(), backLeft.getCurrentPosition(), backRight.getCurrentPosition());
                telemetry.update();
            }

            // Stop all motion;
            frontLeft.setPower(0);
            frontRight.setPower(0);
            backLeft.setPower(0);
            backRight.setPower(0);

            // Turn off RUN_TO_POSITION
            frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            sleep(100);   // optional pause after each move
        }
    }

    protected void goBackward(double tgtPower) {
        frontRight.setPower(tgtPower);
        frontLeft.setPower(-tgtPower);
        backRight.setPower(tgtPower);
        backLeft.setPower(-tgtPower);
    }

    protected void goForward(double tgtPower) {
        frontRight.setPower(-tgtPower);
        frontLeft.setPower(tgtPower);
        backRight.setPower(-tgtPower);
        backLeft.setPower(tgtPower);
    }

    protected void stopMotors() {
        frontRight.setPower(0);
        frontLeft.setPower(0);
        backRight.setPower(0);
        backLeft.setPower(0);
    }

    private void strafeRight(double tgtPower) {
        frontRight.setPower(tgtPower);
        frontLeft.setPower(tgtPower);
        backRight.setPower(-tgtPower);
        backLeft.setPower(-tgtPower);
    }

    private void strafeLeft(double tgtPower) {
        frontRight.setPower(-tgtPower);
        frontLeft.setPower(-tgtPower);
        backRight.setPower(tgtPower);
        backLeft.setPower(tgtPower);
    }
}
