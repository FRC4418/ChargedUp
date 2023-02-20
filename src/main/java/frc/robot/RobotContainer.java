// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.AutoConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.commands.DrivetrainDrive;
import frc.robot.commands.resetOdometry;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.Vision;

import java.util.List;

import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.PathPoint;
import com.pathplanner.lib.commands.PPRamseteCommand;
import com.pathplanner.lib.server.PathPlannerServer;
import com.stuypulse.stuylib.input.gamepads.AutoGamepad;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.constraint.DifferentialDriveVoltageConstraint;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RamseteCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  public final AutoGamepad driver = new AutoGamepad(0);
  public final Vision vision = new Vision();
  
  public final DriveSubsystem driveTrain = new DriveSubsystem(vision);


  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Configure the trigger bindings
    configureDefualtCommands();
    configureCommnads();
    PathPlannerServer.startServer(5811);
  }
  public void configureDefualtCommands(){
    driveTrain.setDefaultCommand(new DrivetrainDrive(driveTrain, driver));
  }
  public void configureCommnads(){
    driver.getDPadUp().whileTrue(new resetOdometry(driveTrain));
  }

  public Command visionAlign() {
    // Create a voltage constraint to ensure we don't accelerate too fast
    var autoVoltageConstraint =
        new DifferentialDriveVoltageConstraint(
            new SimpleMotorFeedforward(
                DriveConstants.ksVolts,
                DriveConstants.kvVoltSecondsPerMeter,
                DriveConstants.kaVoltSecondsSquaredPerMeter),
            DriveConstants.kDriveKinematics,
            10);

    // Create config for trajectory
    TrajectoryConfig config =
        new TrajectoryConfig(
                AutoConstants.kMaxSpeedMetersPerSecond,
                AutoConstants.kMaxAccelerationMetersPerSecondSquared)
            // Add kinematics to ensure max speed is actually obeyed
            .setKinematics(DriveConstants.kDriveKinematics)
            // Apply the voltage constraint
            .addConstraint(autoVoltageConstraint);

    // An example trajectory to follow.  All units in meters.
    // Trajectory exampleTrajectory =
    //     TrajectoryGenerator.generateTrajectory(
    //         // Start at the origin facing the +X direction
    //         new Pose2d(0, 0, new Rotation2d(0)),
    //         // Pass through these two interior waypoints, making an 's' curve path
    //         List.of(),
    //         // End 3 meters straight ahead of where we started, facing forward
    //         //A 1 MEANS 10 METERS, DO MATH
    //         new Pose2d(0.5, 0, new Rotation2d(0)),
    //         // Pass config
    //         config); 
    Trajectory photonTrak = TrajectoryGenerator.generateTrajectory(
        vision.getCameraToTarget(),
        List.of(), 
        new Pose2d(0.8,0.0, new Rotation2d(Units.degreesToRadians(180))),
        config
      );

    RamseteCommand ramseteCommand =
        new RamseteCommand(
            photonTrak,
            /**
            This is the main difference between V1 and V2. V2 is using the getPose method
            which returns values from odometry as opposed to V1 which used the pose estimator
            relative to the target. The thinking behind this was that becuase we were having trouble
            with looisng the target, if we drove the trajectory using odometry instead of pose
            estimation, we wouldn't need to deal with that whole null handeling and realignment.
            **/
            driveTrain::getPose,
            new RamseteController(AutoConstants.kRamseteB, AutoConstants.kRamseteZeta),
            new SimpleMotorFeedforward(
                DriveConstants.ksVolts,
                DriveConstants.kvVoltSecondsPerMeter,
                DriveConstants.kaVoltSecondsSquaredPerMeter),
            DriveConstants.kDriveKinematics,
            driveTrain::getWheelSpeeds,
            new PIDController(6.0, 1.5, 2.0),
            new PIDController(6.0, 1.5, 2.0),
            // RamseteCommand passes volts to the callback
            
            driveTrain::tankDriveVolts,
            driveTrain);

    // Reset odometry to the starting pose of the trajectory.
    driveTrain.resetOdometry(photonTrak.getInitialPose());

    // Run path following command, then stop at the end.
    return ramseteCommand.andThen(() -> driveTrain.tankDriveVolts(0, 0));

  }

  
  public Command drivePath(boolean isFirstPath, String nameOfPath) {
    // An example command will be run in autonomous

    PathPlannerTrajectory drivePath1 = PathPlanner.loadPath(nameOfPath, new PathConstraints(0.2, 0.2));
    PathPlannerServer.sendActivePath(drivePath1.getStates());

    return new SequentialCommandGroup(
      new InstantCommand(() -> {
        // Reset odometry for the first path you run during auto
        if(isFirstPath){
          Pose2d e = drivePath1.getInitialPose();  
          //Pose2d flippedPose = new Pose2d(e.getX(),e.getY(),e.getRotation().minus(Rotation2d.fromDegrees(180)));
          //driveTrain.resetOdometry(flippedPose);
          driveTrain.resetOdometry(e);
        }
      }),
      new PPRamseteCommand(
          drivePath1, 
          driveTrain::getPose, // Pose supplier
          new RamseteController(),  
          new SimpleMotorFeedforward(0.66871, 1.98, 0.61067),
          driveTrain.kinematics, // DifferentialDriveKinematics
          driveTrain::getWheelSpeeds, // DifferentialDriveWheelSpeeds supplier
          new PIDController(6.4, 0.8, 0.2), // Left controller. Tune these values for your robot. Leaving them 0 will only use feedforwards.
          new PIDController(6.4, 0.8, 0.2), // Right controller (usually the same values as left controller)
          driveTrain::tankDriveVolts, // Voltage bicnsumer
          false, // Should the path be automatically mirrored depending on alliance color. Optional, defaults to true
          driveTrain // Requires this drive subsystem
      )
  );
}

public Command getAutonomousCommand(boolean isFirstPath){
   return new SequentialCommandGroup(drivePath(true, "Pos3Across"), visionAlign());
   //return drivePath(isFirstPath, "markToComm").andThen(visionAlign(), null);
}
  //  public Command getAutonomousCommand(boolean isFirstPath) {
  //   // An example command will be run in autonomous

  //   PathPlannerTrajectory examplePath = PathPlanner.loadPath("lineCurvePath", new PathConstraints(0.4, 0.2));
  //   PathPlannerServer.sendActivePath(examplePath.getStates());

  //   return new SequentialCommandGroup(
  //     new InstantCommand(() -> {
  //       // Reset odometry for the first path you run during auto
  //       if(isFirstPath){
  //           driveTrain.resetOdometry(examplePath.getInitialPose());
  //       }
  //     }),
  //     new PPRamseteCommand(
  //         examplePath, 
  //         driveTrain::getPose, // Pose supplier
  //         new RamseteController(),
  //         new SimpleMotorFeedforward(0.66871, 1.98, 0.61067),
  //         driveTrain.kinematics, // DifferentialDriveKinematics
  //         driveTrain::getWheelSpeeds, // DifferentialDriveWheelSpeeds supplier
  //         new PIDController(0.6, 0.1, 0.3), // Left controller. Tune these values for your robot. Leaving them 0 will only use feedforwards.
  //         new PIDController(0.6, 0.1, 0.3), // Right controller (usually the same values as left controller)
  //         driveTrain::tankDriveVolts, // Voltage biconsumer
  //         true, // Should the path be automatically mirrored depending on alliance color. Optional, defaults to true
  //         driveTrain // Requires this drive subsystem
  //     )
  // );
  // }

}
