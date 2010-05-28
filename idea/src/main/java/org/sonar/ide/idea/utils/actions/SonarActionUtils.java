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

package org.sonar.ide.idea.utils.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.sonar.ide.idea.utils.IdeaResourceUtils;
import org.sonar.ide.idea.utils.SonarUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

/**
 * @author Evgeny Mandrikov
 */
public final class SonarActionUtils {
  public static VirtualFile getVirtualFile(AnActionEvent event) {
    return event.getData(PlatformDataKeys.VIRTUAL_FILE);
  }

  public static String getResourceKey(AnActionEvent event) {
    VirtualFile virtualFile = getVirtualFile(event);
    PsiFile psiFile = PsiManager.getInstance(getProject(event)).findFile(virtualFile);
    return IdeaResourceUtils.getInstance().getFileKey(psiFile);
  }

  public static Sonar getSonar(AnActionEvent event) {
    return SonarUtils.getSonar(getProject(event));
  }

  public static Host getSonarServer(AnActionEvent event) {
    return SonarUtils.getSonarSettings(getProject(event)).getServer();
  }

  public static Project getProject(AnActionEvent event) {
    return event.getData(PlatformDataKeys.PROJECT);
  }

  /**
   * Hide utility-class constructor.
   */
  private SonarActionUtils() {
  }
}
