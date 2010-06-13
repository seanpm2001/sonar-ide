/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;

public class ViolationsTest extends UITestCase {

  @Test
  public void testRefreshViolations() throws Exception {
    configureDefaultSonarServer(getTestServer().getBaseUrl());

    final String projectName = "SimpleProject";
    importNonMavenProject(projectName);

    final SWTBotShell shell = showSonarPropertiesPage(projectName);
    shell.bot().textWithLabel("GroupId :").setText(getGroupId(projectName));

    shell.bot().button("Apply").click();
    shell.bot().button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(shell));

    final SWTBotTree tree = selectProject(projectName);

    assertThat(getMarkers(projectName).length, is(0));

    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 10); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work

    assertThat(getMarkers(projectName).length, greaterThan(0));
  }

  private IMarker[] getMarkers(final String projectName) throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    return project.findMarkers(SonarPlugin.MARKER_VIOLATION_ID, true, IResource.DEPTH_INFINITE);
  }
}
