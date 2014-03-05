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

  private static final String SONAR_MODULES_PROPERTY_KEY = "sonar.modules";
  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioProjectBuilder.class);

  private final Settings settings;

  public VisualStudioProjectBuilder(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void build(Context context) {
    build(context, new VisualStudioAssemblyLocator());
  }

  public void build(Context context, VisualStudioAssemblyLocator assemblyLocator) {
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    File solutionFile = getSolutionFile(solutionProject.getBaseDir());
    if (solutionFile == null) {
      LOG.info("No Visual Studio solution file found.");
      return;
    }

    LOG.info("Using the following Visual Studio solution: " + solutionFile.getAbsolutePath());

    if (settings.hasKey(SONAR_MODULES_PROPERTY_KEY)) {
      throw new SonarException("Do not use the Visual Studio bootstrapper and set the \"" + SONAR_MODULES_PROPERTY_KEY + "\" property at the same time.");
    }

    // Workaround http://jira.codehaus.org/browse/SONARPLUGINS-3501
    solutionProject.resetSourceDirs();

    VisualStudioSolution solution = new VisualStudioSolutionParser().parse(solutionFile);

    VisualStudioProjectParser projectParser = new VisualStudioProjectParser();
    for (VisualStudioSolutionProject project : solution.projects()) {
      File projectFile = relativePathFile(solutionFile.getParentFile(), project.path());
      if (!projectFile.isFile()) {
        LOG.warn("Unable to find the Visual Studio project file " + projectFile.getAbsolutePath());
      } else {
        buildModule(solutionProject, project.name(), projectFile, projectParser.parse(projectFile), assemblyLocator, solutionFile);
      }
    }
  }

  private void buildModule(ProjectDefinition solutionProject, String projectName, File projectFile, VisualStudioProject project, VisualStudioAssemblyLocator assemblyLocator,
    File solutionFile) {
    ProjectDefinition module = ProjectDefinition.create()
      .setKey(solutionProject.getKey() + ":" + projectName)
      .setName(projectName);
    solutionProject.addSubProject(module);

    module.setBaseDir(projectFile.getParentFile());
    module.setSourceDirs(projectFile.getParentFile());

    for (String filePath : project.files()) {
      File file = relativePathFile(projectFile.getParentFile(), filePath);
      if (!file.isFile()) {
        LOG.warn("Cannot find the file " + file.getAbsolutePath() + " of project " + projectName);
      } else {
        module.addSourceFiles(file);
      }
    }

    setFxCopProperties(module, projectFile, project, assemblyLocator);
    setReSharperProperties(module, projectName, solutionFile);
  }

  private void setFxCopProperties(ProjectDefinition module, File projectFile, VisualStudioProject project, VisualStudioAssemblyLocator assemblyLocator) {
    File assembly = assemblyLocator.locateAssembly(projectFile, project);
    if (assembly == null) {
      LOG.warn("Unable to locate the assembly of project " + projectFile.getAbsolutePath());
      return;
    }

    LOG.info("Setting the Code Analysis / FxCop assembly property to " + assembly.getAbsolutePath() + " for project " + projectFile.getAbsolutePath());

    module.setProperty("sonar.csharp.fxcop.assemblies", assembly.getAbsolutePath());
    module.setProperty("sonar.vbnet.fxcop.assemblies", assembly.getAbsolutePath());
  }

  private void setReSharperProperties(ProjectDefinition module, String projectName, File solutionFile) {
    module.setProperty("sonar.csharp.resharper.solution.file", solutionFile.getAbsolutePath());
    module.setProperty("sonar.csharp.resharper.project.name", projectName);

    module.setProperty("sonar.vbnet.resharper.solution.file", solutionFile.getAbsolutePath());
    module.setProperty("sonar.vbnet.resharper.project.name", projectName);
  }

  @Nullable
  private File getSolutionFile(File projectBaseDir) {
    File result;

    String solutionPath = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY);
    if (!Strings.nullToEmpty(solutionPath).isEmpty()) {
      result = new File(projectBaseDir, solutionPath);
    } else {
      Collection<File> solutionFiles = FileUtils.listFiles(projectBaseDir, new String[] {"sln"}, false);
      if (solutionFiles.isEmpty()) {
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

  private static File relativePathFile(File file, String relativePath) {
    return new File(file, relativePath.replace('\\', '/'));
  }

}
