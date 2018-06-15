/*
 * Copyright 2018 Evernote Corporation. All rights reserved.
 */
package com.evernote.teamcity.datadog;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildRevision;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.CompilationBlockBean;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

/**
 * Listens for build events and exports them to DataDog as events and metrics.
 * <p>
 * Metrics:
 * <pre>
 * teamcity.build.started_count
 * teamcity.build.finished_count
 * teamcity.build.success_count
 * teamcity.build.failed_count
 * teamcity.build.duration
 * </pre>
 * <p>
 * Tags:
 * <pre>
 * build_project_id
 * build_type_id
 * build_branch
 * build_status
 * </pre>
 *
 * @author @tbasanov
 */
public class DataDogExportingBuildServerListener extends BuildServerAdapter {

  @Nonnull private final PluginDescriptor pluginDescriptor;
  @Nonnull private final ServerSettings serverSettings;

  public DataDogExportingBuildServerListener(
      @Nonnull PluginDescriptor pluginDescriptor,
      @Nonnull ServerSettings serverSettings,
      @Nonnull EventDispatcher<BuildServerListener> buildServerListenerEventDispatcher) {
    this.pluginDescriptor = pluginDescriptor;
    this.serverSettings = serverSettings;
    buildServerListenerEventDispatcher.addListener(this);
  }

  @Override public void buildStarted(@NotNull SRunningBuild build) {
    buildEventHappened(build);
  }
  
  @Override public void buildFinished(@NotNull SRunningBuild build) {
    buildEventHappened(build);
  }

  @Override public void buildInterrupted(@NotNull SRunningBuild build) {
    buildEventHappened(build);
  }

  /** Is triggered for all build lifecycle events. */
  private void buildEventHappened(@Nonnull SRunningBuild build) {
    Collection<SBuildFeatureDescriptor> dataDogBuildFeatures =
        build.getBuildFeaturesOfType(DataDogBuildFeature.DATADOG_BUILD_FEATURE_TYPE);
    for (SBuildFeatureDescriptor dataDogBuildFeature : dataDogBuildFeatures) {
      exportBuildEventToDataDog(
          createStatsDClient(
              DataDogBuildFeatureParameters.fromMap(dataDogBuildFeature.getParameters())),
          build
      );
    }
  }

  /** Export all build metrics and events of interest to DataDog. */
  private void exportBuildEventToDataDog(
      StatsDClient statsDClient, SRunningBuild build) {
    try {
      if (build.isPersonal()) {
        // skipping all personal builds
        return;
      }

      BuildStatistics buildStatistics = build.getBuildStatistics(
          BuildStatisticsOptions.ALL_TESTS_NO_DETAILS);
      boolean buildSuccess = build.getFailureReasons().isEmpty()
          && !build.isInterrupted() && build.isFinished();
      String branchTag = build.getBranch() != null ? build.getBranch().getName() : "N/A";
      String statusTag =
          !build.isFinished()
              ? "started"
              : build.isInterrupted()
                  ? "interrupted"
                  : build.getFailureReasons().isEmpty()
                      ? "success"
                      : "failed";
      String[] tags = {
          "build_project_id:" + build.getProjectExternalId(),
          "build_type_id:" + build.getBuildTypeExternalId(),
          "build_branch:" + branchTag,
          "build_status:" + statusTag,
      };

      if (build.isFinished()) {
        statsDClient.count("teamcity.build.finished_count", 1, tags);
        if (buildSuccess) {
          statsDClient.count("teamcity.build.success_count", 1, tags);
        } else {
          statsDClient.count("teamcity.build.failed_count", 1, tags);
        }

        statsDClient.time("teamcity.build.duration",
            TimeUnit.SECONDS.toMillis(build.getDuration()), tags);
        statsDClient.histogram("teamcity.build.log_size",
            TimeUnit.SECONDS.toMillis(
                build.getBuildLog().getSizeEstimateAsLong()), tags);

        statsDClient.histogram("teamcity.build.compilation_error_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getCompilationErrorsCount()), tags);

        statsDClient.histogram("teamcity.build.tests.run_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getAllTestRunCount()), tags);
        statsDClient.histogram("teamcity.build.tests.failed_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getFailedTestCount()), tags);
        statsDClient.histogram("teamcity.build.tests.all_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getAllTestCount()), tags);
        statsDClient.histogram("teamcity.build.tests.ignored_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getIgnoredTestCount()), tags);
        statsDClient.histogram("teamcity.build.tests.new_failed_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getNewFailedCount()), tags);
        statsDClient.histogram("teamcity.build.tests.passed_count",
            TimeUnit.SECONDS.toMillis(
                buildStatistics.getPassedTestCount()), tags);
      } else {
        statsDClient.count("teamcity.build.started_count", 1, tags);
      }

      // Gradually build event text and tags
      // https://docs.datadoghq.com/graphing/event_stream/#markdown-events
      final String eventTitle;
      final StringBuilder eventText = new StringBuilder("%%% \n");
      final List<String> eventTags = Lists.newArrayList(tags);

      eventTitle = String.format(
          "TeamCity build %s: %s #%s",
          statusTag,
          build.getFullName(), build.getBuildNumber());
      eventText.append(String.format(
          "TeamCity build %s: [%s #%s](%s/viewLog.html?buildId=%s)\n",
          build.isFinished() ? "finished" : "started",
          build.getFullName(), build.getBuildNumber(),
          serverSettings.getRootUrl(), build.getBuildId()));
      if (build.isFinished()) {
        eventTags.add("build_finished");
      } else {
        eventTags.add("build_started");
      }
      eventTags.add("build_number:" + build.getBuildNumber());
      eventTags.add("build_id:" + build.getBuildId());

      eventText.append(String.format("Triggered by: %s\n",
          build.getTriggeredBy().getAsString()));
      eventTags.add("build_triggered_by:" + build.getTriggeredBy().getRawTriggeredBy());

      if (!build.getRevisions().isEmpty()) {
        eventText.append("Source code VCS roots:\n```\n");
        for (BuildRevision buildRevision : build.getRevisions()) {
          eventText.append(String.format("VCS root: %s   Revision: %s\n",
              buildRevision.getRoot().getName(),
              buildRevision.getRevisionDisplayName()));
          eventTags.add("vcs_root:" + buildRevision.getRoot().getDescription());
          eventTags.add("vcs_revision:" + buildRevision.getRevision());
        }
        eventText.append("```\n");
      } else {
        eventText.append("No source code VCS roots\n");
      }

      eventText.append(String.format(
          "Build agent: %s\n", build.getAgent().getName()));
      eventTags.add("build_agent_name:" + build.getAgent().getName());
      eventTags.add("build_agent_ip_address:" + build.getAgent().getHostAddress());
      eventTags.add("build_agent_hostname:" + build.getAgent().getHostName());

      if (build.isFinished()) {
        if (!build.getFailureReasons().isEmpty()) {
          eventText.append("Build failures:\n```\n");
          for (BuildProblemData buildProblemData : build.getFailureReasons()) {
            eventText.append(String.format("%s\n",
                buildProblemData.getDescription()));
            eventTags.add("build_failure_reason:" + buildProblemData.getType());
          }
          eventText.append("```\n");

          if (buildStatistics.getFailedTestCount() > 0) {
            STestRun sTestRun = buildStatistics.getFailedTests().get(0);
            TestName testName = sTestRun.getTest().getName();
            eventText.append(String.format(
                "Failed test(s) %d (%d new) from %d, Example: `%s`\n",
                buildStatistics.getFailedTestCount(),
                buildStatistics.getNewFailedCount(),
                buildStatistics.getAllTestCount(),
                testName.getAsString()));
            eventTags.add("failed_test_example:" + testName.getShortName());
          }
        } else {
          eventText.append("Build was successful!\n");
          eventTags.add("build_success");
        }
        eventTags.add("build_internal_status:" + build.getBuildStatus());

        eventText.append(String.format("Build length: %s\n",
            Period.seconds((int) build.getDuration())
                .toString(PeriodFormat.getDefault())));

        eventText.append(String.format("Generated: %s build logs\n",
            build.getBuildLog().getSizeEstimate()));

        final long[] totalArtifactsSize = {0};
        final long[] totalArtifactsCount = {0};
        build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT).iterateArtifacts(
            new BuildArtifacts.BuildArtifactsProcessor() {
              @NotNull @Override public Continuation processBuildArtifact(
                  @NotNull BuildArtifact artifact) {
                if (!artifact.getName().isEmpty() && !artifact.isContainer()) {
                  if (totalArtifactsCount[0] == 0) {
                    eventText.append("Generated build artifacts:\n```\n");
                  }
                  totalArtifactsSize[0] += artifact.getSize();
                  totalArtifactsCount[0] += 1;
                  eventText.append(String.format("%s (%s)\n",
                      artifact.getRelativePath(),
                      FileUtils.byteCountToDisplaySize(artifact.getSize())));
                  eventTags.add("build_artifact:" + artifact.getRelativePath());
                }
                return Continuation.CONTINUE;
              }
            });
        if (totalArtifactsCount[0] != 0) {
          eventText.append(String.format("```\nTotal %s artifacts size: %s\n",
              totalArtifactsCount[0],
              FileUtils.byteCountToDisplaySize(totalArtifactsSize[0])));
        } else {
          eventText.append("No build artifacts generated\n");
        }
      }

      eventText.append("\n %%%");
      statsDClient.recordEvent(
          Event.builder()
              .withTitle(eventTitle)
              .withText(eventText.toString())
              .withHostname(build.getAgent().getHostName())
              .build(),
          eventTags.toArray(new String[0]));
    } catch (RuntimeException e) {
      Loggers.SERVER.warn("Failed to export to DataDog, ignoring", e);
    }
  }

  /** Reuse StatsDClient instances where possible. */
  private final static Cache<String, StatsDClient> STATSDCLIENT_CACHE =
      CacheBuilder.newBuilder()
          .expireAfterAccess(1, TimeUnit.MINUTES)
          .build();

  /** Create an appropriate instance based on parameters passed. */
  public static StatsDClient createStatsDClient(
      @Nonnull final DataDogBuildFeatureParameters parameters) {
    try {
      return STATSDCLIENT_CACHE.get(parameters.getDataDogAgentAddress(),
          new Callable<StatsDClient>() {
            @Override public StatsDClient call() {
              return new NonBlockingStatsDClient(
                  null,
                  parameters.getDataDogAgentAddress(),
                  parameters.getDataDogAgentPort() != null
                      ? parameters.getDataDogAgentPort()
                      : 8125);
            }
          });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
