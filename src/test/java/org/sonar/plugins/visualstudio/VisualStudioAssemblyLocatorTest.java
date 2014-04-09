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

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.plugins.visualstudio.VisualStudioAssemblyLocator.FileLastModifiedComparator;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisualStudioAssemblyLocatorTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void test() throws Exception {
    Settings settings = mock(Settings.class);
    VisualStudioAssemblyLocator locator = new VisualStudioAssemblyLocator(settings);

    VisualStudioProject project = mock(VisualStudioProject.class);

    when(project.outputType()).thenReturn(null);
    assertThat(locator.locateAssembly(mock(File.class), project)).isNull();

    when(project.outputType()).thenReturn("Library");
    when(project.assemblyName()).thenReturn(null);
    assertThat(locator.locateAssembly(mock(File.class), project)).isNull();

    when(project.outputType()).thenReturn("Library");
    when(project.assemblyName()).thenReturn("MyLibrary");
    when(project.outputPaths()).thenReturn(Collections.EMPTY_LIST);
    assertThat(locator.locateAssembly(mock(File.class), project)).isNull();

    File projectFile = tmp.newFile("Solution.sln");
    tmp.newFolder("outputPath1");
    tmp.newFolder("outputPath2");
    tmp.newFolder("outputPath3");

    tmp.newFolder("other_outputPath");

    File assemblyFile1 = tmp.newFile("outputPath1/MyLibrary.dll");

    when(project.outputType()).thenReturn("Library");
    when(project.assemblyName()).thenReturn("MyLibrary");

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of(""));
    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath1"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile1.getCanonicalPath());

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath2"));
    assertThat(locator.locateAssembly(projectFile, project)).isNull();

    Thread.sleep(1500L);

    File assemblyFile3 = tmp.newFile("outputPath3/MyLibrary.dll");

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of("", ""));
    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath1", "outputPath3"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of("", ""));
    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath3", "outputPath1"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());

    // Build configuration and build platform tests

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath1", "outputPath3"));

    when(settings.getString("sonar.dotnet.buildConfiguration")).thenReturn("Debug");
    when(settings.getString("sonar.dotnet.buildPlatform")).thenReturn("AnyCPU");

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of("Debug", "AnyCPU"));
    assertThat(locator.locateAssembly(projectFile, project)).isNull();

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of("Debug AnyCPU", "AnyCPU"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile1.getCanonicalPath());

    when(project.propertyGroupConditions()).thenReturn(ImmutableList.of("Debug", "AnyCPU_Debug"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());

    when(settings.getString("sonar.dotnet.buildPlatform")).thenReturn(null);
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());

    // Assembly output path

    when(settings.getString("sonar.visualstudio.outputPath")).thenReturn("");
  }

  @Test
  public void extensions() {
    assertThat(new VisualStudioAssemblyLocator(mock(Settings.class)).extension(mock(File.class), "Library")).isEqualTo("dll");
    assertThat(new VisualStudioAssemblyLocator(mock(Settings.class)).extension(mock(File.class), "Exe")).isEqualTo("exe");
    assertThat(new VisualStudioAssemblyLocator(mock(Settings.class)).extension(mock(File.class), "WinExe")).isEqualTo("exe");

    assertThat(new VisualStudioAssemblyLocator(mock(Settings.class)).extension(mock(File.class), "Database")).isNull();
  }

  @Test
  public void unsupported_extension() {
    VisualStudioProject project = mock(VisualStudioProject.class);
    when(project.outputType()).thenReturn("Database");
    when(project.assemblyName()).thenReturn("foo");
    when(project.outputPaths()).thenReturn(ImmutableList.of("bin/Debug"));

    VisualStudioAssemblyLocator locator = new VisualStudioAssemblyLocator(mock(Settings.class));
    assertThat(locator.locateAssembly(mock(File.class), project)).isNull();
  }

  @Test
  public void last_modified_date_comparator() {
    FileLastModifiedComparator comparator = new FileLastModifiedComparator();

    File file1 = mock(File.class);
    File file2 = mock(File.class);

    when(file1.lastModified()).thenReturn(0L);
    when(file2.lastModified()).thenReturn(0L);
    assertThat(comparator.compare(file1, file2)).isEqualTo(0);

    when(file1.lastModified()).thenReturn(1L);
    when(file2.lastModified()).thenReturn(0L);
    assertThat(comparator.compare(file1, file2)).isEqualTo(-1);

    when(file1.lastModified()).thenReturn(0L);
    when(file2.lastModified()).thenReturn(1L);
    assertThat(comparator.compare(file1, file2)).isEqualTo(1);
  }

}
