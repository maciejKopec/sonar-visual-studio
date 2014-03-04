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
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class VisualStudioPlugin extends SonarPlugin {

  public static final String VISUAL_STUDIO_SOLUTION_PROPERTY_KEY = "sonar.visualstudio.solution";

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      PropertyDefinition
        .builder(VISUAL_STUDIO_SOLUTION_PROPERTY_KEY)
        .category("Visual Studio")
        .name("Solution file")
        .description("Absolute or relative path from the project folder to the solution file to use. If set to empty, a \"*.sln\" file will be looked up in the project folder.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      VisualStudioProjectBuilder.class);
  }

}
