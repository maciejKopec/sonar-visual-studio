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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.batch.InstantiationStrategy;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class VisualStudioAssemblyLocator {

  private static final Comparator<File> FILE_LAST_MODIFIED_COMPARATOR = new FileLastModifiedComparator();

  public File locateAssembly(File projectFile, VisualStudioProject project) {
    if (project.outputType() == null || project.assemblyName() == null || project.outputPaths().isEmpty()) {
      return null;
    }

    String extension = extension(projectFile, project.outputType());
    String assemblyFileName = project.assemblyName() + "." + extension;

    List<File> candidates = Lists.newArrayList();
    for (String outputPath : project.outputPaths()) {
      File candidate = new File(projectFile.getParentFile(), outputPath.replace('\\', '/') + '/' + assemblyFileName);
      if (candidate.isFile()) {
        candidates.add(candidate);
      }
    }

    if (candidates.isEmpty()) {
      return null;
    }

    Collections.sort(candidates, FILE_LAST_MODIFIED_COMPARATOR);

    return candidates.get(0);
  }

  @VisibleForTesting
  String extension(File projectFile, String outputType) {
    if ("Library".equals(outputType)) {
      return "dll";
    }

    throw new IllegalArgumentException("Unsupported output type \"" + outputType + "\" for project " + projectFile.getAbsolutePath());
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
