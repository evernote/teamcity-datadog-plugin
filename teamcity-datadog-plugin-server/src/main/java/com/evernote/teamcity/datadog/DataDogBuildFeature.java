/*
 * Copyright 2018 Evernote Corporation. All rights reserved.
 */
package com.evernote.teamcity.datadog;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

/**
 * Build feature that allow to configure DataDog export.
 *
 * @author @tbasanov
 */
public class DataDogBuildFeature extends BuildFeature {

  /** Any unique string would do. */
  public static final String DATADOG_BUILD_FEATURE_TYPE =
      DataDogBuildFeature.class.getName();

  @NotNull private final PluginDescriptor pluginDescriptor;

  public DataDogBuildFeature(@NotNull final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  @NotNull @Override public String getType() {
    return DATADOG_BUILD_FEATURE_TYPE;
  }

  @NotNull @Override public String getDisplayName() {
    return "DataDog Exporter";
  }

  @Nullable @Override public String getEditParametersUrl() {
    return pluginDescriptor.getPluginResourcesPath(
        DataDogPluginBuildFeatureController.EDIT_PARAMETERS_URL_PLUGIN_RELATIVE_PATH);
  }

  @NotNull @Override public String describeParameters(
      @NotNull Map<String, String> params) {
    DataDogBuildFeatureParameters parameters =
        DataDogBuildFeatureParameters.fromMap(params);
    return String.format("Send metrics to %s",
        StringUtil.escapeHTML(parameters.getDataDogAgentAddress(), true));
  }

  @Nullable @Override public Map<String, String> getDefaultParameters() {
    return new DataDogBuildFeatureParameters().toMap();
  }
}
