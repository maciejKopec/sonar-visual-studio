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
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class VisualStudioPlugin extends SonarPlugin {

  public static final String VISUAL_STUDIO_SOLUTION_PROPERTY_KEY = "sonar.visualstudio.solution";
  public static final String VISUAL_STUDIO_SKIP_PROPERTY_KEY = "sonar.visualstudio.skip";
  public static final String VISUAL_STUDIO_BUILD_CONFIGURATION = "sonar.dotnet.buildConfiguration";
  public static final String VISUAL_STUDIO_BUILD_PLATFORM = "sonar.dotnet.buildPlatform";
  public static final String VISUAL_STUDIO_OUTPUT_PATH_PROPERTY_KEY = "sonar.visualstudio.outputPath";
  public static final String VISUAL_STUDIO_OLD_SOLUTION_PROPERTY_KEY = "sonar.dotnet.visualstudio.solution.file";
  public static final String VISUAL_STUDIO_OLD_DOTNET_ASSEMBLIES_PROPERTY_KEY = "sonar.dotnet.assemblies";

  private static final String CATEGORY = "Visual Studio Bootstrapper";

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      PropertyDefinition
        .builder(VISUAL_STUDIO_SOLUTION_PROPERTY_KEY)
        .deprecatedKey(VISUAL_STUDIO_OLD_SOLUTION_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Solution file")
        .description("Absolute or relative path from the project folder to the solution file to use. If set to empty, a \"*.sln\" file will be looked up in the project folder.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_SKIP_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Skip the analysis bootstrapping")
        .type(PropertyType.BOOLEAN)
        .description("Whether or not the analysis should be bootstrapped from Visual Studio files.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_OUTPUT_PATH_PROPERTY_KEY)
        .deprecatedKey(VISUAL_STUDIO_OLD_DOTNET_ASSEMBLIES_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Assemblies output path")
        .description("Overrides the assemblies output path, useful for Team Foundation Server builds.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),

      VisualStudioProjectBuilder.class);
  }

}
