/*
 * SonarQube Visual Studio Plugin
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

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Collection;

public class VisualStudioProjectBuilder extends ProjectBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioProjectBuilder.class);

  private final Settings settings;

  public VisualStudioProjectBuilder(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void build(Context context) {
    ProjectDefinition project = context.projectReactor().getRoot();

    File solutionFile = getSolutionFile(project.getBaseDir());
    if (solutionFile == null) {
      LOG.info("No Visual Studio solution file found.");
      return;
    }

    LOG.info("Using the following Visual Studio solution: " + solutionFile.getAbsolutePath());

    // Workaround http://jira.codehaus.org/browse/SONARPLUGINS-3501
    project.resetSourceDirs();

    VisualStudioSolution solution = new VisualStudioSolutionParser(solutionFile).parse();
    for (VisualStudioProject vsProject : solution.projects()) {
      System.out.println("GOT A PROJECT: " + vsProject.name());
    }
  }

  @Nullable
  private File getSolutionFile(File projectBaseDir) {
    File result;

    String solutionPath = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY);
    if (!Strings.nullToEmpty(solutionPath).isEmpty()) {
      result = new File(projectBaseDir, solutionPath);
    } else {
      Collection<File> solutionFiles = FileUtils.listFiles(projectBaseDir, new String[] {"sln"}, false);
      if (solutionFiles.size() == 0) {
        result = null;
      } else if (solutionFiles.size() == 1) {
        result = solutionFiles.iterator().next();
      } else {
        throw new SonarException("Found several .sln files in " + projectBaseDir.getAbsolutePath() +
          ". Please set \"" + VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY + "\" to explicitly tell which one to use.");
      }
    }

    return result;
  }

}
