/*
 * Copyright 2018 Evernote Corporation. All rights reserved.
 */
package com.evernote.teamcity.datadog;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * All parameters that our build feature may have for a particular build configuration.
 *
 * @author @tbasanov
 */
public class DataDogBuildFeatureParameters {

  /** Used from JSP. */
  public static final String DATADOG_AGENT_ADDRESS_AND_PORT =
      "DATADOG_AGENT_ADDRESS_AND_PORT";

  @Nonnull private String dataDogAgentAddress = "localhost";
  @Nullable private Integer dataDogAgentPort;

  public String getDatadogAgentAddressAndPort() {
    StringBuilder builder = new StringBuilder();
    builder.append(dataDogAgentAddress);
    if (dataDogAgentPort != null) {
      builder.append(':').append(dataDogAgentPort);
    }
    return builder.toString();
  }

  public void setDatadogAgentAddressAndPort(@Nonnull String datadogAgentAddressAndPort) {
    Matcher matcher = Pattern.compile("(.*?)(?::(\\d{1,5}))?")
        .matcher(datadogAgentAddressAndPort);
    if (!matcher.matches()) {
      // should not happen given a regexp expression above
      throw new IllegalArgumentException(
          "Invalid address and port: " + datadogAgentAddressAndPort);
    }
    dataDogAgentAddress = matcher.group(1);
    if (matcher.group(2) != null) {
      dataDogAgentPort = Integer.valueOf(matcher.group(2));
    } else {
      dataDogAgentPort = null;
    }
  }

  @Nonnull public String getDataDogAgentAddress() {
    return dataDogAgentAddress;
  }

  public void setDataDogAgentAddress(@Nonnull String dataDogAgentAddress) {
    this.dataDogAgentAddress = dataDogAgentAddress;
  }

  @Nullable public Integer getDataDogAgentPort() {
    return dataDogAgentPort;
  }

  public void setDataDogAgentPort(@Nullable Integer dataDogAgentPort) {
    this.dataDogAgentPort = dataDogAgentPort;
  }

  /** Convert parameters to a map to pass to TeamCity APIs. */
  public Map<String, String> toMap() {
    return ImmutableMap.of(
        DATADOG_AGENT_ADDRESS_AND_PORT, getDatadogAgentAddressAndPort());
  }

  /** Get parameters back from a map passed by TeamCity APIs. */
  public static DataDogBuildFeatureParameters fromMap(
      @Nullable Map<String, String> parametersMap) {
    DataDogBuildFeatureParameters parameters = new DataDogBuildFeatureParameters();
    if (parametersMap == null) {
      return parameters;
    }
    String datadogAgentAddressAndPort =
        parametersMap.get(DATADOG_AGENT_ADDRESS_AND_PORT);
    if (!Strings.isNullOrEmpty(datadogAgentAddressAndPort)) {
      parameters.setDatadogAgentAddressAndPort(datadogAgentAddressAndPort);
    }
    return parameters;
  }
}
