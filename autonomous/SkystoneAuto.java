/* Copyright (c) 2019 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.util.Iterator;

import detectors.FoundationPipeline.SkyStone;
import detectors.OpenCvDetector;

@Autonomous(name = "BlueStoneAuto", group = "BlueSide")
public class SkystoneAuto extends LinearOpMode {
    /*
    --------------------------------------
     */
    private DcMotor frontRight;
    private DcMotor frontLeft;
    private DcMotor backRight;
    private DcMotor backLeft;
    private DcMotor sweepMotorLeft;
    private DcMotor sweepMotorRight;
    private DcMotor lift;

    // servos
    private Servo arm;
    private Servo claw;
    private Servo sweepLeft;
    private Servo sweepRight;
    private Servo foundation;
    private Servo stoneGrabber;

    //sensors
    private DigitalChannel armSensor;

    final double motorPower = 0.75;

    // imu stuff
    BNO055IMU imu;
    Orientation lastAngles = new Orientation();
    double globalAngle, power = .30, correction;
    /*
    --------------------------------------
     */

    public void runOpMode() throws InterruptedException {

        /*
        ----------------- hardware setup ----------------------
        */
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");

        sweepMotorLeft = hardwareMap.get(DcMotor.class, "sweepMotorLeft");
        sweepMotorRight = hardwareMap.get(DcMotor.class, "sweepMotorRight");
        sweepLeft = hardwareMap.get(Servo.class, "sweepLeft");
        sweepRight = hardwareMap.get(Servo.class, "sweepRight");

        lift = hardwareMap.get(DcMotor.class, "lift");
        arm = hardwareMap.get(Servo.class, "arm");
        claw = hardwareMap.get(Servo.class, "claw");

        stoneGrabber = hardwareMap.get(Servo.class, "stoneGrabber");

        telemetry.setAutoClear(true);
        outtake();

        telemetry.addData("Booting Up", " . . .");
        telemetry.update();
        /*
        ---------------------------------------------------------
        */

        OpenCvDetector fieldDetector = new OpenCvDetector(this);
        fieldDetector.start();

        /*
        ------------------------IMU SETUP------------------------
        */

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

        parameters.mode = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled = false;

        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        imu = hardwareMap.get(BNO055IMU.class, "imu");

        imu.initialize(parameters);

        // caliblrating imu
        telemetry.addData("Mode", "calibrating...");
        telemetry.update();

        while (!isStopRequested() && !imu.isGyroCalibrated()) {
            sleep(50);
            idle();
        }

        telemetry.addData("Mode", "waiting for start");
        telemetry.addData("imu calib status", imu.getCalibrationStatus().toString());
        telemetry.update();
        /*
        ----------------------------------------------------------------
        */

        /*
        --------------------------STONE LOCATOR-------------------------
        */
        /*
        double skyStoneX = -10;
        while (!opModeIsActive()) {
            //detector.printposition(detector.getPosition());
            //fieldDetector.print(fieldDetector.getObjectsFoundations());
            //Log.d("GO TO MO","go");
            SkyStone[] skyStone = fieldDetector.getObjectsSkyStones();

            telemetry.addData("Skystones Found", skyStone.length);
            for (int i = 0; i < skyStone.length; i++) {
                try {
                    telemetry.addLine("Skystone 1 X: " + skyStone[i].x + " Y: " + skyStone[i].y);
                    if (skyStone[i].y < 260 && skyStone[i].y > 150)
                        skyStoneX = skyStone[i].x;
                    telemetry.addLine("X COORD: " + skyStoneX);
                } catch (Exception e) {
                }
            }
            telemetry.update();
        }
        */
        // 0 is down
        stoneGrabber.setPosition(0);
        // wait for start command.
        waitForStart();
        /*
        ---------------------------------------------------------------------
        */

        /*
        ------------------------OBTAINING STONE--------------------------
        */


        /*
        // move away from wall
        strafeLeft(0.75);
        sleep(500);
        stopMotors();
        rotate(80, 1);

        moveStone();

        //rotate(175, 1);
        //centerStone(skyStoneX);
        //moveStone();

        // go to line
        goBackward(0.75);
        sleep(100);
        stopMotors();
        strafeRight(0.75);
        sleep(150);
        stopMotors();

         */
        ///////////////////////////////////*/


        telemetry.addLine("frik mah life");
        telemetry.update();

        try {
            fieldDetector.stop();
        } catch (Exception e) {
        }
    }


    /*
    ----------------------------imu methods----------------------------
     */
    /*
     * Resets the cumulative angle tracking to zero.
     */
    private void resetAngle() {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        globalAngle = 0;
    }

    /*
     * Get current cumulative angle rotation from last reset.
     * @return Angle in degrees. + = left, - = right.
     */
    private double getAngle() {

        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        globalAngle += deltaAngle;

        lastAngles = angles;

        return globalAngle;
    }

    /*
     * See if we are moving in a straight line and if not return a power correction value.
     *
     * @return Power adjustment, + is adjust left - is adjust right.
     */
    private double checkDirection() {
        // The gain value determines how sensitive the correction is to direction changes.
        // You will have to experiment with your robot to get small smooth direction changes
        // to stay on a straight line.
        double correction, angle, gain = .10;

        angle = getAngle();

        if (angle == 0)
            correction = 0;             // no adjustment.
        else
            correction = -angle;        // reverse sign of angle for correction.

        correction = correction * gain;

        return correction;
    }
    /*
    -----------------------------------------------------------
     */

    /*
    ----------------------- movement methods ------------------
     */

    public void moveStone() {
        ///////////GEITTING STONE////////////
        goForward(0.3);
        sleep(1600);

        stopMotors();
        sleep(100);

        rotate(20, 1);
        goForward(0.3);
        sleep(600);
        stopMotors();
        sleep(100);

        collect(3);
        lockIn();
        //closeClaw();

        /////////MOVING TO OTHER SIDE//////////
        goBackward(0.4);
        sleep(800);
        rotate(70, 1);

        goForward(0.75);
        sleep(2000);
        outtake();
        sleep(50);
        stopMotors();
        sleep(100);

        ///////// GO BACK TO OTHER SIDE ////////////
        releaseStone(0.75);
        goBackward(0.75);
        sleep(250);
        stopMotors();
    }

    public boolean centerStone(double skyStoneX) {
        // right of the robot
        if (skyStoneX > 400) {
            goForward(0.4);
            sleep((long) (200));
        }
        // basically center
        else if (skyStoneX > 200) {
            goForward(0.4);
            sleep((long) (100));
        }
        // left of the robot
        else if (skyStoneX > 0) {
            goBackward(0.4);
            sleep((long) (200));
        }
        // not detected
        else if (skyStoneX == -10) {
            goForward(0.4);
            sleep(500);
            return false;
        }
        return true;
    }

    private void rotate(int degrees, double Power) {
        resetAngle();
        stopMotors();
        double tgtPower = Power / 4;
        // getAngle() returns + when rotating counter clockwise (left) and - when rotating clockwise (right).

        if (degrees > 0) {   // turn left
            frontLeft.setPower(tgtPower);
            backLeft.setPower(-tgtPower);
            frontRight.setPower(tgtPower);
            backRight.setPower(-tgtPower);
            while (getAngle() < degrees) {
                sleep(1);
            }
        } else if (degrees < 0) {   // turn right
            frontLeft.setPower(-tgtPower);
            backLeft.setPower(tgtPower);
            frontRight.setPower(-tgtPower);
            backRight.setPower(tgtPower);
            while (getAngle() > degrees) {
                sleep(1);
            }

        } else return;

        stopMotors();
        sleep(1000);
        return;
    }

    public void collect(int numTimes) {
        for (int i = 0; i < numTimes; i++) {
            intake(0.75);
            sleep(600);
            outtake();
            sleep(600);
        }
    }

    public void moveArm() {
        while (arm.getPosition() > 0.01) {
            arm.setPosition(arm.getPosition() - 0.0095);
        }
    }

    public void resetArm() {
        arm.setPosition(0.95);
    }

    public void raiseArm(double tgtPower) {
        lift.setPower(tgtPower);
    }

    public void holdArm() {
        //????
    }

    public void lowerArm() {
        if (!armSensor.getState()) {
            lift.setPower(0);
        }
    }

    public void intake(double tgtPower) {
        sweepMotorLeft.setPower(0.75);
        sweepMotorRight.setPower(0.75);
        sweepLeft.setPosition(0);
        sweepRight.setPosition(1);
    }

    public void releaseStone(double tgtPower) {
        sweepMotorLeft.setPower(-tgtPower);
        sweepMotorRight.setPower(-tgtPower);
    }

    public void outtake() {
        sweepMotorLeft.setPower(0);
        sweepMotorRight.setPower(0);
        sweepLeft.setPosition(0.5);
        sweepRight.setPosition(0.5);
    }

    public void lockIn() {
        sweepLeft.setPosition(0);
        sweepRight.setPosition(1);
    }

    public void closeClaw() {
        claw.setPosition(1);
    }

    public void openClaw() {
        claw.setPosition(0);
    }

    public void goForward(double tgtPower) { //done?
        frontRight.setPower(tgtPower);
        frontLeft.setPower(-tgtPower);
        backRight.setPower(-tgtPower);
        backLeft.setPower(tgtPower);
    }

    public void goBackward(double tgtPower) { //done
        frontRight.setPower(-tgtPower);
        frontLeft.setPower(tgtPower);
        backRight.setPower(tgtPower);
        backLeft.setPower(-tgtPower);
    }


    ////////////////////////////////////////////////
    public void strafeRight(double tgtPower) {
        frontRight.setPower(-tgtPower);
        frontLeft.setPower(-tgtPower);
        backRight.setPower(-tgtPower);
        backLeft.setPower(-tgtPower);
    }

    public void strafeLeft(double tgtPower) {
        frontLeft.setPower(tgtPower);
        frontRight.setPower(tgtPower);
        backLeft.setPower(tgtPower);
        backRight.setPower(tgtPower);

    }

    public void stopMotors() {
        frontRight.setPower(0);
        frontLeft.setPower(0);
        backRight.setPower(0);
        backLeft.setPower(0);
        sweepMotorLeft.setPower(0);
        sweepMotorRight.setPower(0);
    }

    /*
    ---------------------------------------------
     */

    public void listhardware() {
        telemetry.setAutoClear(false);

        Iterator<com.qualcomm.robotcore.hardware.HardwareDevice> t = hardwareMap.iterator();
        while (t.hasNext()) {

            telemetry.addData("device found", (t.next().getDeviceName()));
            telemetry.update();
        }

        telemetry.setAutoClear(true);
    }
}
