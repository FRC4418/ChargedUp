// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.manndibleCommands;

import frc.robot.subsystems.Arms;
import edu.wpi.first.wpilibj2.command.CommandBase;

/** An example command that uses an example subsystem. */
public class ArmsCloseCone extends CommandBase {
  @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
  private final Arms arms;
  double position;

  /**
   * Creates a new ExampleCommand.
   *
   * @param subsystem The subsystem used by this command.
   */
  public ArmsCloseCone(Arms arms) {
    this.arms = arms;
    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(arms);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {}

  // Called every time the scheduler runs while the command is scheduled. (this requires tuning, this numebr represents the position, not speed)
  @Override
  public void execute() {
    //ADD SCALING FOR WHEN REALLY CLOSE TO SETPOINT
    System.out.println(arms.getPos());
    if(arms.getPos() < -5500){
      arms.grab(0.2);
    } else if(arms.getPos() >-5500){
      arms.grab(-0.2);
    } else{
      arms.stop();
    }
    //arms.grab(0.2);
    // double currentPos = arms.getPos();
    // if(currentPos != arms.getPos()){
    //   new armsClose(arms);
    }


  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    arms.grab(0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    if(arms.getPos() == -5500){
      return true;
    } else{
      return false;
    }
  }
}
