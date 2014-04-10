/*
 * Analysis Bootstrapper for Visual Studio Projects
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.visualstudio;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;

import javax.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VisualStudioAssemblyLocator {

  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioProjectBuilder.class);
  private static final Comparator<File> FILE_LAST_MODIFIED_COMPARATOR = new FileLastModifiedComparator();

  private final Settings settings;

  public VisualStudioAssemblyLocator(Settings settings) {
    this.settings = settings;
  }

  public File locateAssembly(File projectFile, VisualStudioProject project) {
    if (project.outputType() == null || project.assemblyName() == null || project.outputPaths().isEmpty()) {
      return null;
    }

    String extension = extension(projectFile, project.outputType());
    if (extension == null) {
      return null;
    }

    String assemblyFileName = project.assemblyName() + "." + extension;
    List<File> candidates = candidates(assemblyFileName, projectFile, project);

    if (candidates.isEmpty()) {
      return null;
    }

    Collections.sort(candidates, FILE_LAST_MODIFIED_COMPARATOR);

    return candidates.get(0);
  }

  @VisibleForTesting
  @Nullable
  String extension(File projectFile, String outputType) {
    String result;

    if ("Library".equals(outputType)) {
      result = "dll";
    } else if ("Exe".equals(outputType)) {
      result = "exe";
    } else if ("WinExe".equals(outputType)) {
      result = "exe";
    } else {
      result = null;
    }

    return result;
  }

  private List<File> candidates(String assemblyFileName, File projectFile, VisualStudioProject project) {
    String outputPath = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_OUTPUT_PATH_PROPERTY_KEY);
    if (outputPath != null) {
      File candidate = new File(new File(outputPath.replace('\\', '/')), assemblyFileName);
      LOG.info("Using the assembly output path specified using the property \"" + VisualStudioPlugin.VISUAL_STUDIO_OUTPUT_PATH_PROPERTY_KEY + "\" set to: " + outputPath);
      return Lists.newArrayList(candidate);
    }

    List<File> candidates = Lists.newArrayList();
    for (int i = 0; i < project.outputPaths().size(); i++) {
      String condition = project.propertyGroupConditions().get(i);
      if (matchesBuildConfigurationAndPlatform(condition)) {
        outputPath = project.outputPaths().get(i);

        File candidate = new File(projectFile.getParentFile(), outputPath.replace('\\', '/') + '/' + assemblyFileName);
        if (candidate.isFile()) {
          candidates.add(candidate);
        }
      }
    }
    return candidates;
  }

  private boolean matchesBuildConfigurationAndPlatform(String condition) {
    String buildConfiguration = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_BUILD_CONFIGURATION);
    String buildPlatform = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_BUILD_PLATFORM);

    if (buildConfiguration != null && buildPlatform != null) {
      LOG.warn("The properties \"" + VisualStudioPlugin.VISUAL_STUDIO_BUILD_CONFIGURATION
        + "\" and \"" + VisualStudioPlugin.VISUAL_STUDIO_BUILD_PLATFORM
        + "\" are deprecated. The latest generated assembly is now picked up for analysis by default instead.");
      return condition.contains(buildConfiguration) && condition.contains(buildPlatform);
    }

    return true;
  }

  public static class FileLastModifiedComparator implements Comparator<File>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(File o1, File o2) {
      if (o1.lastModified() == o2.lastModified()) {
        return 0;
      }

      return o1.lastModified() > o2.lastModified() ? -1 : 1;
    }

  }

}
