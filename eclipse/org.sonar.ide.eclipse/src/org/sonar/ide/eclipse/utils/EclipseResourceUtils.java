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

package org.sonar.ide.eclipse.utils;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.shared.AbstractResourceUtils;

/**
 * @author Jérémie Lagarde
 * 
 */
public class EclipseResourceUtils extends AbstractResourceUtils<IResource> {

  private static EclipseResourceUtils instance;

  public static EclipseResourceUtils getInstance() {
    if (instance == null) {
      instance = new EclipseResourceUtils();
    }
    return instance;
  }

  private EclipseResourceUtils() {
  }

  @Override
  protected boolean isJavaFile(IResource file) {
    return (JavaCore.create(file) instanceof ICompilationUnit);
  }

  @Override
  public String getFileName(IResource file) {
    return isJavaFile(file) ? StringUtils.substringBeforeLast(file.getName(), ".") : file.getName();
  }

  @Override
  protected String getDirectoryPath(IResource file) {
    throw new NotImplementedException("Currently only java files supported");
  }

  @Override
  protected String getPackageName(IResource file) {
    try {
      if (isJavaFile(file)) {
        IJavaElement element = JavaCore.create(file);
        if ( !(element instanceof ICompilationUnit)) {
          return "";
        }
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(file);
        IPackageDeclaration[] packages = compilationUnit.getPackageDeclarations();
        StringBuilder name = null;
        for (IPackageDeclaration packageDeclaration : packages) {
          if (name == null) {
            name = new StringBuilder(packageDeclaration.getElementName());
          } else {
            name.append(".").append(packageDeclaration.getElementName());
          }
        }
        if (name == null) {
          return "";
        }
        return name.toString();
      }
    } catch (JavaModelException e) {
      // TODO Add exception management.
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getProjectKey(IResource file) {
    ProjectProperties properties = ProjectProperties.getInstance(file);
    return getProjectKey(properties.getGroupId(), properties.getArtifactId(), properties.getBranch());
  }
}
