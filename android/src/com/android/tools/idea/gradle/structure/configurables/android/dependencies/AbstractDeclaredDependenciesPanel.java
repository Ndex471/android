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
package com.android.tools.idea.gradle.structure.configurables.android.dependencies;

import com.android.tools.idea.gradle.structure.configurables.PsContext;
import com.android.tools.idea.gradle.structure.configurables.android.dependencies.editor.DependencyEditor;
import com.android.tools.idea.gradle.structure.configurables.android.dependencies.editor.LibraryDependencyEditor;
import com.android.tools.idea.gradle.structure.model.PsProject;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidDependency;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule;
import com.android.tools.idea.structure.dialog.Header;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SideBorder;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import static com.intellij.ui.IdeBorderFactory.createEmptyBorder;
import static com.intellij.ui.ScrollPaneFactory.createScrollPane;
import static com.intellij.util.PlatformIcons.LIBRARY_ICON;
import static com.intellij.util.ui.UIUtil.getInactiveTextColor;

public abstract class AbstractDeclaredDependenciesPanel extends JPanel implements Disposable {
  @NotNull private final PsProject myProject;
  @NotNull private final EmptyEditorPanel myEmptyEditorPanel;
  @NotNull private final JScrollPane myEditorScrollPane;
  @NotNull private final JPanel myContentsPanel;

  @NotNull private final Map<Class<?>, DependencyEditor> myEditors = Maps.newHashMap();

  @Nullable private final PsAndroidModule myModule;

  private List<AbstractPopupAction> myPopupActions;

  protected AbstractDeclaredDependenciesPanel(@NotNull String title,
                                              @NotNull PsContext context,
                                              @NotNull PsProject project,
                                              @Nullable PsAndroidModule module) {
    super(new BorderLayout());
    myProject = project;
    myModule = module;

    initializeEditors(context);
    myEmptyEditorPanel = new EmptyEditorPanel();
    myEditorScrollPane = createScrollPane(myEmptyEditorPanel);
    myEditorScrollPane.setBorder(createEmptyBorder());

    Header header = new Header(title);
    add(header, BorderLayout.NORTH);

    OnePixelSplitter splitter = new OnePixelSplitter(true, "psd.editable.dependencies.main.horizontal.splitter.proportion", 0.75f);

    myContentsPanel = new JPanel(new BorderLayout());
    myContentsPanel.add(createActionsPanel(), BorderLayout.NORTH);

    splitter.setFirstComponent(myContentsPanel);
    splitter.setSecondComponent(myEditorScrollPane);

    add(splitter, BorderLayout.CENTER);
  }

  private void initializeEditors(@NotNull PsContext context) {
    addEditor(new LibraryDependencyEditor(context));
  }

  private void addEditor(@NotNull DependencyEditor<?> editor) {
    myEditors.put(editor.getSupportedModelType(), editor);
  }

  protected void updateEditor(@Nullable PsAndroidDependency selected) {
    if (selected != null) {
      DependencyEditor editor = myEditors.get(selected.getClass());
      if (editor != null) {
        myEditorScrollPane.setViewportView(editor.getPanel());
        //noinspection unchecked
        editor.display(selected);
        return;
      }
    }
    myEditorScrollPane.setViewportView(myEmptyEditorPanel);
  }

  @NotNull
  private JPanel createActionsPanel() {
    final JPanel actionsPanel = new JPanel(new BorderLayout());

    DefaultActionGroup actions = new DefaultActionGroup();

    AnAction addDependencyAction = new DumbAwareAction("Add Dependency", "", IconUtil.getAddIcon()) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        initPopupActions();
        JBPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<AbstractPopupAction>(null, myPopupActions) {
          @Override
          public Icon getIconFor(AbstractPopupAction action) {
            return action.icon;
          }

          @Override
          public boolean isMnemonicsNavigationEnabled() {
            return true;
          }

          @Override
          public PopupStep onChosen(final AbstractPopupAction action, boolean finalChoice) {
            return doFinalStep(new Runnable() {
              @Override
              public void run() {
                action.execute();
              }
            });
          }

          @Override
          @NotNull
          public String getTextFor(AbstractPopupAction action) {
            return "&" + action.index + "  " + action.text;
          }
        });
        popup.show(new RelativePoint(actionsPanel, new Point(0, actionsPanel.getHeight() - 1)));
      }
    };

    actions.add(addDependencyAction);

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TOP", actions, true);
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
    actionsPanel.add(toolbarComponent, BorderLayout.CENTER);

    return actionsPanel;
  }

  private void initPopupActions() {
    if (myPopupActions == null) {
      java.util.List<AbstractPopupAction> actions = Lists.newArrayList();
      actions.add(new AddDependencyAction());
      myPopupActions = actions;
    }
  }

  @NotNull
  protected JPanel getContentsPanel() {
    return myContentsPanel;
  }

  private class AddDependencyAction extends AbstractPopupAction {
    AddDependencyAction() {
      super("Artifact Dependency", LIBRARY_ICON, 1);
    }

    @Override
    void execute() {
      AddArtifactDependencyDialog dialog;
      if (myModule == null) {
        dialog = new AddArtifactDependencyDialog(myProject);
      }
      else {
        dialog = new AddArtifactDependencyDialog(myModule);
      }
      dialog.showAndGet();
    }
  }

  private static abstract class AbstractPopupAction implements ActionListener {
    @NotNull final String text;
    @NotNull final Icon icon;

    final int index;

    AbstractPopupAction(@NotNull String text, @NotNull Icon icon, int index) {
      this.text = text;
      this.icon = icon;
      this.index = index;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      execute();
    }

    abstract void execute();
  }

  private static class EmptyEditorPanel extends JPanel {
    EmptyEditorPanel() {
      super(new BorderLayout());
      JBLabel emptyText = new JBLabel("Please select a declared dependency");
      emptyText.setForeground(getInactiveTextColor());
      emptyText.setHorizontalAlignment(SwingConstants.CENTER);
      add(emptyText, BorderLayout.CENTER);
    }
  }
}
