package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.tfod.TfodProcessor;

public abstract class Robot extends LinearOpMode {
    public IMU imu;
    public TfodProcessor tfod;
    public VisionPortal visionPortal;
    public Servo LA, RA, LH, RH, ALL, ARL, IT, DP, ADP;
    public DcMotorEx FL, FR, BL, BR, encoderleft, encoderRight, encoderMid ;
    public int FL_Target, FR_Target, BL_Target, BR_Target;
    public final double[] tileSize            = {60.96, 60.96};  // Width * Length
    /* TETRIX Motor Encoder per revolution */
    public final int      Counts_per_TETRIX   = 24;
    /** HD HEX Motor Encoder per revolution */
    public final int      Counts_per_HD_HEX   = 28;
    /** 20:1 HD HEX Motor Encoder per revolution */
    public final int      Gear_20_HD_HEX      = Counts_per_HD_HEX * 20;
    /** (3 * 4 * 5):1 UltraPlanetary HD HEX Motor Encoder per revolution */
    public final double   Gear_60_HD_HEX      = Counts_per_HD_HEX * 54.8;
    public final double   Wheel_Diameter_Inch = 7.5/2.54;
    public final double   Counts_per_Inch     = Gear_20_HD_HEX / (Wheel_Diameter_Inch * Math.PI);
    public double[]       currentXY           = {0, 0};
    public double         L                   = 35.50; //distance between 1 and 2 in cm
    public double         B                   = 21.50; //distance between center of 1 and 2 and 3 in cm
    public final double   r                   = 2.5 ; // Odomentry wheel radius in cm
    public final double   N                   = 2000 ; // ticks per one rotation
    public double         cm_per_tick         = 2.0 * Math.PI * r / N ;
    public int            dn1, dn2, dn3 ;
    public double         dx, dy, Posx, Posy  ;
    public double         R                   = 5.08; //mecanum wheel radius in cm

    // update encoder
    public int            CurrentLeftPosition= 0 ;
    public int            CurrentRightPosition= 0 ;
    public int            CurrentMidPosition= 0 ;

    public int            OldLeftPosition= 0 ;
    public int            OldRightPosition= 0 ;
    public int            OldMidPosition= 0;

    public void Odomentry (){
      OldLeftPosition         = CurrentLeftPosition;
      OldRightPosition        = CurrentRightPosition;
      OldMidPosition          = CurrentMidPosition;

      CurrentLeftPosition     = encoderleft.getCurrentPosition();
      CurrentRightPosition    = encoderRight.getCurrentPosition();
      CurrentMidPosition      = encoderMid.getCurrentPosition();

      dn1 = CurrentLeftPosition - OldLeftPosition;
      dn2 = CurrentRightPosition - OldRightPosition;
      dn3 = CurrentMidPosition - OldMidPosition;

      dx = cm_per_tick * ( dn1 + dn2 ) / 2.0 ;
      dy = cm_per_tick * ( dn3 - ( dn2 - dn1 ) * B / L );

      double yaw   = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
      Posy += dx * Math.cos(yaw) - dy * Math.sin(yaw);
      Posx += dx * Math.sin(yaw) + dy * Math.cos(yaw);

    }
    public void move(double tilex, double tiley, double omega, double timeout){

        double targetx = tilex * tileSize[0];
        double targety = tiley * tileSize[1];
        double R = 5.08; //mecanum wheel radius in cm
        double DistanceLR = 33.655;
        double DistanceFB = 28.50;
        double DeltaX = targetx - Posx;
        double DeltaY = targety - Posy;

        double Vx = 0.1 * DeltaX;
        double Vy = 0.1 * DeltaY;

        //
        double VFL = (Vx + Vy - omega * (DistanceFB + DistanceLR) / R);
        double VFR = (Vx - Vy + omega * (DistanceFB + DistanceLR) / R);
        double VBL = (Vx - Vy - omega * (DistanceFB + DistanceLR) / R);
        double VBR = (Vx + Vy + omega * (DistanceFB + DistanceLR) / R);
        ElapsedTime runtime = new ElapsedTime();
        runtime.reset();


        while (opModeIsActive()) {
            Odomentry();
            double yaw = -imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            MovePower(VFL,VFR,VBL, VBR);
            telemetry.addData("XY", "%6f cm %6f cm" , Posx, Posy);
            telemetry.update();
            if ((runtime.seconds() >= timeout) ) break;
        }
        Break(0.4);
    }


    public void MovePower(double Front_Left, double Front_Right,
                          double Back_Left,  double Back_Right) {
        FL.setPower(Front_Left);
        FR.setPower(Front_Right);
        BL.setPower(Back_Left);
        BR.setPower(Back_Right);
    }
    public void Break(double stopSecond) {
        if (stopSecond == 0) return;
        MovePower(0, 0, 0, 0);
        MoveMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        sleep((long) (stopSecond * 1000));
    }

    public void MoveMode(DcMotor.RunMode moveMode) {
        FL.setMode(moveMode);
        FR.setMode(moveMode);
        BL.setMode(moveMode);
        BR.setMode(moveMode);
    }
    public void Initialize(DcMotor.RunMode moveMode, double[] DuoServoAng, double[] ServoAng) {
        imu = hardwareMap.get(IMU.class,       "imu");
        FL  = hardwareMap.get(DcMotorEx.class, "Front_Left");    FR  = hardwareMap.get(DcMotorEx.class, "Front_Right");
        BL  = hardwareMap.get(DcMotorEx.class, "Back_Left");     BR  = hardwareMap.get(DcMotorEx.class, "Back_Right");
        encoderleft = FL ;
        encoderRight = FR;
        encoderMid = BL;

        // Initialize IMU
      imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                RevHubOrientationOnRobot.UsbFacingDirection .BACKWARD)));

        // Reverse Servo

        // Set Servo Position

        // Reverse Motors
        FR.setDirection(DcMotorSimple.Direction.REVERSE);
        BR.setDirection(DcMotorSimple.Direction.REVERSE);
        // setMode Motors

        // SetBehavior Motors
        FL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        FR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        // SetPower Motors
        MovePower(0, 0, 0, 0);
    }

}