/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.run.ui;

import static com.android.tools.idea.run.tasks.AbstractDeployTask.MIN_API_VERSION;

import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.run.DeploymentService;
import com.android.tools.idea.run.deployable.Deployable;
import com.android.tools.idea.run.deployable.DeployableProvider;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import java.util.concurrent.Future;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(BaseAction.class);

  @NotNull
  protected final String myName;

  @NotNull
  protected final Icon myIcon;

  @NotNull
  private final Key<Boolean> myKey;

  public BaseAction(@NotNull String id,
                    @NotNull String name,
                    @NotNull Key<Boolean> key,
                    @NotNull Icon icon,
                    @NotNull Shortcut shortcut) {
    super(name, name, icon);
    myName = name;
    myKey = key;
    myIcon = icon;

    KeymapManager manager = KeymapManager.getInstance();
    if (manager != null) {
      final Keymap keymap = manager.getActiveKeymap();
      if (keymap != null) {
        keymap.addShortcut(id, shortcut);
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();

    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    // Check if any executors are starting up (e.g. if the user JUST clicked on an executor, and deployment hasn't finished).
    boolean canRun = true;
    RunnerAndConfigurationSettings configSettings = RunManager.getInstance(project).getSelectedConfiguration();
    if (configSettings == null) {
      canRun = false;
    }
    else {
      RunConfiguration config = configSettings.getConfiguration();
      Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
      for (Executor executor : executors) {
        ProgramRunner programRunner = ProgramRunner.getRunner(executor.getId(), config);
        if (programRunner == null) {
          continue;
        }
        canRun &= !ExecutorRegistry.getInstance().isStarting(project, executor.getId(), programRunner.getRunnerId());
      }
    }

    presentation.setEnabled(canRun && checkCompatibility(project));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      LOG.warn(myName + " action performed with no project");
      return;
    }

    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
    if (settings == null) {
      LOG.warn(myName + " action could not locate current run config settings");
      return;
    }

    // TODO: Figure out the debugger flow. For now always use the Run executor.
    Executor executor = getExecutor();
    if (executor == null) {
      LOG.warn(myName + " action could not identify executor");
      return;
    }

    ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(executor, settings.getConfiguration());
    ExecutionEnvironment env = builder.activeTarget().dataContext(e.getDataContext()).build();

    env.putCopyableUserData(myKey, true);
    ProgramRunnerUtil.executeConfiguration(env, false, true);
  }

  @Nullable
  private static Executor getExecutor() {
    for (Executor executor : Executor.EXECUTOR_EXTENSION_NAME.getExtensions()) {
      if (DefaultRunExecutor.EXECUTOR_ID.equals(executor.getId())) {
        return executor;
      }
    }

    return null;
  }

  private static boolean checkCompatibility(@NotNull Project project) {
    DeployableProvider deployableProvider = DeploymentService.getInstance(project).getDeployableProvider();
    if (deployableProvider == null) {
      return false;
    }

    if (deployableProvider.isDependentOnUserInput()) {
      return true;
    }

    Deployable deployable;
    try {
      deployable = deployableProvider.getDeployable();
      if (deployable == null) {
        return false;
      }
      Future<AndroidVersion> versionFuture = deployable.getVersion();
      if (!versionFuture.isDone()) {
        // Don't stall the EDT - if the Future isn't ready, just return false.
        return false;
      }
      return versionFuture.get().getApiLevel() >= MIN_API_VERSION && deployable.isApplicationRunningOnDeployable();
    }
    catch (Exception e) {
      return false;
    }
  }
}