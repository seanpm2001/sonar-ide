/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.views.MeasuresView;

/**
 * @author Evgeny Mandrikov
 */
@Ignore("Godin: configureDefaultSonarServer doesn't work")
public class MeasuresTest extends UITestCase {

  @Before
  public void setUp() throws Exception {
    configureDefaultSonarServer();
  }

  @Test
  public void testShowMeasures() throws Exception {
    // Import project
    final String projectName = "measures";
    importAndConfigureNonMavenProject(projectName);

    // Open measures view
    openPerspective(ISonarConstants.PERSPECTIVE_ID);
    bot.viewById(MeasuresView.ID).setFocus();

    SWTBotTable table = bot.table();
    assertThat(table.rowCount(), is(0));

    // Open file
    SWTBotTree tree = selectProject(projectName);
    SWTBotTreeItem treeItem = tree.expandNode(projectName, "src/main/java", "(default package)");
    treeItem.getNode("Measures.java").doubleClick();

    bot.sleep(1000 * 20);

    assertThat(table.rowCount(), greaterThan(0));
  }

}
