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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.plugins.visualstudio.VisualStudioAssemblyLocator.FileLastModifiedComparator;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisualStudioAssemblyLocatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test() {

  }

  @Test
  public void supported_extensions() {
    assertThat(new VisualStudioAssemblyLocator().extension(mock(File.class), "Library")).isEqualTo("dll");
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
