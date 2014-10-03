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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.bootstrap.ProjectBuilder.Context;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

import java.io.File;
import java.util.regex.PatternSyntaxException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VisualStudioProjectBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    File workingDir = new File("target/VisualStudioProjectBuilderTest/.sonar");
    when(solutionProject.getWorkDir()).thenReturn(workingDir);

    final File assemblyFile = mock(File.class);
    when(assemblyFile.getAbsolutePath()).thenReturn("c:/MyLibrary.dll");
    VisualStudioAssemblyLocator assemblyLocator = mock(VisualStudioAssemblyLocator.class);
    when(assemblyLocator.locateAssembly(Mockito.anyString(), Mockito.any(File.class), Mockito.any(VisualStudioProject.class))).thenAnswer(new Answer<File>() {

      @Override
      public File answer(InvocationOnMock invocation) throws Throwable {
        File projectFile = (File) invocation.getArguments()[1];

        return "MyLibrary.csproj".equals(projectFile.getName()) ? assemblyFile : null;
      }

    });

    Settings settings = new Settings();
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY, true);
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_TEST_PROJECT_PATTERN, ".*Test");

    // This property must be forwarded
    settings.setProperty("MyLibrary.sonar.something", "foobar");
    // This property must be overridden
    settings.setProperty("MyLibrary.sonar.cs.fxcop.assembly", "foobar");

    new VisualStudioProjectBuilder(settings).build(context, assemblyLocator);

    verify(solutionProject).resetSources();
    verify(solutionProject).resetTests();

    ArgumentCaptor<ProjectDefinition> subModules = ArgumentCaptor.forClass(ProjectDefinition.class);
    verify(solutionProject, Mockito.times(2)).addSubProject(subModules.capture());

    ProjectDefinition libraryProject = subModules.getAllValues().get(0);
    assertThat(libraryProject.getKey()).isEqualTo("solution:key:MyLibrary");
    assertThat(libraryProject.getName()).isEqualTo("MyLibrary");
    // Inherit the version
    assertThat(libraryProject.getVersion()).isNull();

    assertThat(libraryProject.getBaseDir().getAbsoluteFile()).isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibrary/").getAbsoluteFile());
    assertThat(libraryProject.getWorkDir()).isEqualTo(new File(workingDir, "solution_key_MyLibrary"));

    assertThat(libraryProject.sources()).hasSize(1);
    assertThat(new File(libraryProject.sources().get(0)).getAbsoluteFile())
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibrary/Adder.cs").getAbsoluteFile());
    assertThat(libraryProject.tests()).isEmpty();

    assertThat(libraryProject.getProperties().get("sonar.something")).isEqualTo("foobar");

    assertThat(libraryProject.getProperties().get("sonar.cs.fxcop.assembly")).isEqualTo("c:/MyLibrary.dll");

    assertThat(libraryProject.getProperties().get("sonar.cs.fxcop.assembly")).isEqualTo("c:/MyLibrary.dll");
    assertThat(libraryProject.getProperties().get("sonar.vbnet.fxcop.assembly")).isEqualTo("c:/MyLibrary.dll");

    assertThat(libraryProject.getProperties().get("sonar.resharper.solutionFile"))
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/solution.sln").getAbsolutePath());
    assertThat(libraryProject.getProperties().get("sonar.resharper.projectName")).isEqualTo("MyLibrary");

    assertThat(libraryProject.getProperties().get("sonar.stylecop.projectFilePath"))
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibrary/MyLibrary.csproj").getAbsolutePath());

    ProjectDefinition libraryTestProject = subModules.getAllValues().get(1);
    assertThat(libraryTestProject.getKey()).isEqualTo("solution:key:MyLibraryTest");
    assertThat(libraryTestProject.getName()).isEqualTo("MyLibraryTest");
    // Inherit the version
    assertThat(libraryTestProject.getVersion()).isNull();

    assertThat(libraryTestProject.getBaseDir().getAbsoluteFile())
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibraryTest/").getAbsoluteFile());
    assertThat(libraryTestProject.getWorkDir()).isEqualTo(new File(workingDir, "solution_key_MyLibraryTest"));

    assertThat(libraryTestProject.sources()).isEmpty();
    assertThat(libraryTestProject.tests()).hasSize(1);
    assertThat(new File(libraryTestProject.tests().get(0)).getAbsoluteFile())
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibraryTest/AdderTest.cs").getAbsoluteFile());

    assertThat(libraryTestProject.getProperties().get("sonar.cs.fxcop.assembly")).isNull();
    assertThat(libraryTestProject.getProperties().get("sonar.vbnet.fxcop.assembly")).isNull();

    assertThat(libraryTestProject.getProperties().get("sonar.resharper.solutionFile"))
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/solution.sln").getAbsolutePath());
    assertThat(libraryTestProject.getProperties().get("sonar.resharper.projectName")).isEqualTo("MyLibraryTest");

    assertThat(libraryTestProject.getProperties().get("sonar.stylecop.projectFilePath"))
      .isEqualTo(new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/MyLibraryTest/MyLibraryTest.csproj").getAbsolutePath());
  }

  @Test
  public void should_support_the_unsafe_project_key_strategy() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);
    when(settings.getString("sonar.visualstudio.projectKeyStrategy")).thenReturn("unsafe");
    new VisualStudioProjectBuilder(settings).build(context, mock(VisualStudioAssemblyLocator.class));

    ArgumentCaptor<ProjectDefinition> subModules = ArgumentCaptor.forClass(ProjectDefinition.class);
    verify(solutionProject, Mockito.times(2)).addSubProject(subModules.capture());

    ProjectDefinition libraryProject = subModules.getAllValues().get(0);
    assertThat(libraryProject.getKey()).isEqualTo("solution:MyLibrary");

    ProjectDefinition libraryTestProject = subModules.getAllValues().get(1);
    assertThat(libraryTestProject.getKey()).isEqualTo("solution:MyLibraryTest");
  }

  @Test
  public void should_use_safe_project_key_strategy_when_no_colon_present_in_original_project_key() {
    Context context = mockContext("solution_key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);
    when(settings.getString("sonar.visualstudio.projectKeyStrategy")).thenReturn("unsafe");
    new VisualStudioProjectBuilder(settings).build(context, mock(VisualStudioAssemblyLocator.class));

    ArgumentCaptor<ProjectDefinition> subModules = ArgumentCaptor.forClass(ProjectDefinition.class);
    verify(solutionProject, Mockito.times(2)).addSubProject(subModules.capture());

    ProjectDefinition libraryProject = subModules.getAllValues().get(0);
    assertThat(libraryProject.getKey()).isEqualTo("solution_key:MyLibrary");

    ProjectDefinition libraryTestProject = subModules.getAllValues().get(1);
    assertThat(libraryTestProject.getKey()).isEqualTo("solution_key:MyLibraryTest");
  }

  @Test
  public void should_fail_with_several_solutions() {
    thrown.expectMessage("Found several .sln files in ");

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);

    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/several_sln/"));
    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void should_pick_chosen_solution() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/several_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);
    when(settings.getString(VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY)).thenReturn("solution_without_tests.sln");

    new VisualStudioProjectBuilder(settings).build(context);

    verify(solutionProject, Mockito.times(1)).addSubProject(Mockito.any(ProjectDefinition.class));
  }

  @Test
  public void should_pick_explicit_solution_over_detected_one() {
    thrown.expectMessage("java.io.FileNotFoundException");
    thrown.expectMessage("non_existing.sln");

    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);
    when(settings.getString(VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY)).thenReturn("non_existing.sln");

    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void no_solution() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/no_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);

    new VisualStudioProjectBuilder(settings).build(context);

    verify(solutionProject, Mockito.never()).addSubProject(Mockito.any(ProjectDefinition.class));
  }

  @Test
  public void should_not_run_when_enable_property_is_set_to_false() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(false);

    new VisualStudioProjectBuilder(settings).build(context);

    verify(solutionProject, Mockito.never()).addSubProject(Mockito.any(ProjectDefinition.class));
  }

  @Test
  public void should_fail_when_sonar_modules_property_is_set() {
    thrown.expectMessage("Do not use the Visual Studio bootstrapper and set the \"sonar.modules\" property at the same time.");

    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);
    when(settings.hasKey("sonar.modules")).thenReturn(true);

    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void should_fail_when_no_visual_studio_projects_are_found() {
    thrown.expectMessage("No Visual Studio projects were found.");

    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/no_projects/"));

    Settings settings = mock(Settings.class);
    when(settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)).thenReturn(true);

    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void should_escape_accents_and_whitespaces() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/accents/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    File workingDir = new File("target/VisualStudioProjectBuilderTest/.sonar");
    when(solutionProject.getWorkDir()).thenReturn(workingDir);

    Settings settings = new Settings();
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY, true);
    // These properties must be forwarded
    settings.setProperty("uber.sonar.something", "foobar");
    settings.setProperty("foo_bar.sonar.somethingElse", "foobar2");

    new VisualStudioProjectBuilder(settings).build(context);

    ArgumentCaptor<ProjectDefinition> subModules = ArgumentCaptor.forClass(ProjectDefinition.class);
    verify(solutionProject, Mockito.times(2)).addSubProject(subModules.capture());

    ProjectDefinition uberProject = subModules.getAllValues().get(0);
    assertThat(uberProject.getKey()).isEqualTo("solution:key:uber");
    assertThat(uberProject.getName()).isEqualTo("über");
    assertThat(uberProject.getWorkDir()).isEqualTo(new File(workingDir, "solution_key_uber"));
    assertThat(uberProject.getProperties().getProperty("sonar.something")).isEqualTo("foobar");

    ProjectDefinition foobarProject = subModules.getAllValues().get(1);
    assertThat(foobarProject.getKey()).isEqualTo("solution:key:foo_bar");
    assertThat(foobarProject.getName()).isEqualTo("foo bar");
    assertThat(foobarProject.getWorkDir()).isEqualTo(new File(workingDir, "solution_key_foo_bar"));
    assertThat(foobarProject.getProperties().getProperty("sonar.somethingElse")).isEqualTo("foobar2");
  }

  @Test
  public void escapeProjectName() {
    assertThat(VisualStudioProjectBuilder.escapeProjectName("foo")).isEqualTo("foo");
    assertThat(VisualStudioProjectBuilder.escapeProjectName("héhé")).isEqualTo("hehe");
    assertThat(VisualStudioProjectBuilder.escapeProjectName("über")).isEqualTo("uber");
  }

  @Test
  public void invalid_test_project_pattern() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/single_sln/"));

    Settings settings = new Settings();
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY, true);
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_TEST_PROJECT_PATTERN, "?");

    thrown.expect(PatternSyntaxException.class);

    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void should_skip_projects() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/accents/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = new Settings();
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY, true);
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_SKIPPED_PROJECTS, ",,uber,");

    new VisualStudioProjectBuilder(settings).build(context);

    verify(solutionProject, Mockito.times(1)).addSubProject(Mockito.any(ProjectDefinition.class));

    context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/accents/"));
    solutionProject = context.projectReactor().getRoot();

    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_SKIPPED_PROJECTS, ",,uber,foo_bar");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No Visual Studio projects were found.");

    new VisualStudioProjectBuilder(settings).build(context);
  }

  @Test
  public void should_only_import_supported_project_types() {
    Context context = mockContext("solution:key", new File("src/test/resources/VisualStudioProjectBuilderTest/project_types/"));
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    Settings settings = new Settings();
    settings.setProperty(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY, true);

    new VisualStudioProjectBuilder(settings).build(context);

    verify(solutionProject, Mockito.times(2)).addSubProject(Mockito.any(ProjectDefinition.class));
  }

  private static Context mockContext(String key, File baseDir) {
    ProjectDefinition project = mock(ProjectDefinition.class);
    when(project.getKey()).thenReturn(key);
    when(project.getBaseDir()).thenReturn(baseDir);

    ProjectReactor reactor = mock(ProjectReactor.class);
    when(reactor.getRoot()).thenReturn(project);

    Context context = mock(Context.class);
    when(context.projectReactor()).thenReturn(reactor);

    return context;
  }

}
