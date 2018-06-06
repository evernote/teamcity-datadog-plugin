package com.evernote.teamcity.datadog;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;

/**
 * Connects {@link DataDogBuildFeature}'s parameters page to its URL.
 *
 * @author tbasanov
 */
public class DataDogPluginBuildFeatureController extends BaseController {

  public static final String EDIT_PARAMETERS_URL_PLUGIN_RELATIVE_PATH =
      "editParameters.jsp";

  @Nonnull private final PluginDescriptor pluginDescriptor;

  public DataDogPluginBuildFeatureController(
      @Nonnull WebControllerManager manager,
      @Nonnull PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
    manager.registerController(pluginDescriptor.getPluginResourcesPath(
        EDIT_PARAMETERS_URL_PLUGIN_RELATIVE_PATH), this);
  }

  /** Hardcode binding to a JSP page. */
  @Nullable
  @Override
  protected ModelAndView doHandle(
      @Nonnull HttpServletRequest httpServletRequest,
      @Nonnull HttpServletResponse httpServletResponse) {
    return new ModelAndView(pluginDescriptor.getPluginResourcesPath(
        EDIT_PARAMETERS_URL_PLUGIN_RELATIVE_PATH));
  }
}
