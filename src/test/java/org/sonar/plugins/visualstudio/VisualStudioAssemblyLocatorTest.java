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

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.plugins.visualstudio.VisualStudioAssemblyLocator.FileLastModifiedComparator;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisualStudioAssemblyLocatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void test() throws Exception {
    VisualStudioAssemblyLocator locator = new VisualStudioAssemblyLocator();

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

    File assemblyFile1 = tmp.newFile("outputPath1/MyLibrary.dll");

    when(project.outputType()).thenReturn("Library");
    when(project.assemblyName()).thenReturn("MyLibrary");

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath1"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile1.getCanonicalPath());

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath2"));
    assertThat(locator.locateAssembly(projectFile, project)).isNull();

    Thread.sleep(1500L);

    File assemblyFile3 = tmp.newFile("outputPath3/MyLibrary.dll");

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath1", "outputPath3"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());

    when(project.outputPaths()).thenReturn(ImmutableList.of("outputPath3", "outputPath1"));
    assertThat(locator.locateAssembly(projectFile, project).getCanonicalPath()).isEqualTo(assemblyFile3.getCanonicalPath());
  }

  @Test
  public void supported_extensions() {
    assertThat(new VisualStudioAssemblyLocator().extension(mock(File.class), "Library")).isEqualTo("dll");
    assertThat(new VisualStudioAssemblyLocator().extension(mock(File.class), "Exe")).isEqualTo("exe");
    assertThat(new VisualStudioAssemblyLocator().extension(mock(File.class), "WinExe")).isEqualTo("exe");
  }

  @Test
  public void unsupported_extension() {
    File projectFile = mock(File.class);
    when(projectFile.getAbsolutePath()).thenReturn("c:/foo.txt");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unsupported output type \"SomeUnsupportedOutputType\" for project c:/foo.txt");

    new VisualStudioAssemblyLocator().extension(projectFile, "SomeUnsupportedOutputType");
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
