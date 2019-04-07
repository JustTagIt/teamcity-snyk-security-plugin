package io.snyk.plugins.teamcity.agent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.snyk.plugins.teamcity.agent.commands.SnykTestCommand;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TeamCityRuntimeException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.CommandExecution;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.snyk.plugins.teamcity.common.SnykSecurityRunnerConstants.SNYK_REPORT_JSON_FILE;
import static java.util.Objects.requireNonNull;

public class SnykCommandBuildSession implements MultiCommandBuildSession {

  private final BuildRunnerContext buildRunnerContext;

  private Iterator<CommandExecutionAdapter> buildSteps;
  private CommandExecutionAdapter lastCommand;

  SnykCommandBuildSession(@NotNull BuildRunnerContext buildRunnerContext) {
    this.buildRunnerContext = requireNonNull(buildRunnerContext);
  }

  @Override
  public void sessionStarted() {
    buildSteps = getBuildSteps();
  }

  @Nullable
  @Override
  public CommandExecution getNextCommand() {
    if (buildSteps.hasNext()) {
      lastCommand = buildSteps.next();
      return lastCommand;
    }
    return null;
  }

  @Nullable
  @Override
  public BuildFinishedStatus sessionFinished() {
    return lastCommand.getResult();
  }

  private Iterator<CommandExecutionAdapter> getBuildSteps() {
    List<CommandExecutionAdapter> steps = new ArrayList<>(3);
    String buildTempDirectory = buildRunnerContext.getBuild().getBuildTempDirectory().getAbsolutePath();

    // Disable for development process
    // SnykVersionCommand snykVersionCommand = new SnykVersionCommand();
    // steps.add(addCommand(snykVersionCommand, Paths.get(buildTempDirectory, "version.txt")));

    SnykTestCommand snykTestCommand = new SnykTestCommand();
    steps.add(addCommand(snykTestCommand, Paths.get(buildTempDirectory, SNYK_REPORT_JSON_FILE)));

    return steps.iterator();
  }

  private CommandExecutionAdapter addCommand(CommandLineBuildService buildService, Path commandOutputPath) {
    try {
      buildService.initialize(buildRunnerContext.getBuild(), buildRunnerContext);
    } catch (RunBuildException ex) {
      throw new TeamCityRuntimeException(ex);
    }
    return new CommandExecutionAdapter(buildService, commandOutputPath);
  }
}
