/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.tests.gui.framework.fixture;

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.Wait;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBList;
import org.fest.swing.cell.JListCellReader;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.ContainerFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;

public class ProjectStructureDialogFixture implements ContainerFixture<JDialog> {

  private final JDialog myDialog;
  private final IdeFrameFixture myIdeFrameFixture;
  private final Robot myRobot;

  ProjectStructureDialogFixture(@Nonnull JDialog dialog, @NotNull IdeFrameFixture ideFrameFixture) {
    myDialog = dialog;
    myIdeFrameFixture = ideFrameFixture;
    myRobot = ideFrameFixture.robot();
  }

  @NotNull
  public static ProjectStructureDialogFixture find(@NotNull IdeFrameFixture ideFrameFixture) {
    JDialog dialog = GuiTests.waitUntilShowing(ideFrameFixture.robot(), Matchers.byTitle(JDialog.class, "Project Structure"));
    return new ProjectStructureDialogFixture(dialog, ideFrameFixture);
  }

  @NotNull
  public FlavorsTabFixture selectFlavorsTab() {
    selectTab("Flavors");
    return new FlavorsTabFixture(myDialog, myIdeFrameFixture);
  }

  private static final JListCellReader CONFIGURATION_CELL_READER = (jList, index) ->
    ((Configurable)jList.getModel().getElementAt(index)).getDisplayName();

  @NotNull
  public ProjectStructureDialogFixture selectConfigurable(@NotNull String item) {
    JBList list = myRobot.finder().findByType(myDialog, JBList.class);
    JListFixture jListFixture = new JListFixture(robot(), list);
    jListFixture.replaceCellReader(CONFIGURATION_CELL_READER);
    jListFixture.selectItem(item);
    return this;
  }

  @NotNull
  public IdeFrameFixture clickOk() {
    GuiTests.findAndClickOkButton(this);
    Wait.seconds(5).expecting("dialog to disappear").until(() -> !target().isShowing());
    return myIdeFrameFixture;
  }

  @Nonnull
  @Override
  public JDialog target() {
    return myDialog;
  }

  @Nonnull
  @Override
  public Robot robot() {
    return myRobot;
  }

  @NotNull
  public BuildTypesTabFixture selectBuildTypesTab() {
    selectTab("Build Types");
    return new BuildTypesTabFixture(myDialog, myIdeFrameFixture);
  }

  private void selectTab(@NotNull String tabName) {
    JTabbedPane tabbedPane = myRobot.finder().findByType(myDialog, JTabbedPane.class);
    new JTabbedPaneFixture(myRobot, tabbedPane).selectTab(tabName);
  }
}
