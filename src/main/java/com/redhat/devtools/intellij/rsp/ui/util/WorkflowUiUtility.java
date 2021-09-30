/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.rsp.ui.util;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import com.redhat.devtools.intellij.common.utils.ExecHelper;
import com.redhat.devtools.intellij.rsp.ui.dialogs.WorkflowDialog;
import com.redhat.devtools.intellij.rsp.util.CommandLineUtils;
import java.nio.charset.Charset;
import java.util.Collections;
import org.jboss.tools.rsp.api.dao.Status;
import org.jboss.tools.rsp.api.dao.WorkflowResponse;
import org.jboss.tools.rsp.api.dao.WorkflowResponseItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Workflow Utility
 * Some RSP calls (like download runtimes or server actions) use a workflow
 * which requires multiple rounds of back-and-forth for a multi-step process to
 * complete.
 *
 * This utility will display all prompts in dialogs or perform other workflow steps
 * like open editors or terminals as requested.
 */
public class WorkflowUiUtility {

    public static Map<String, Object> displayPromptsSeekWorkflowInput(WorkflowResponse resp) {
        List<WorkflowResponseItem> items = resp.getItems();
        List<WorkflowResponseItem> prompts = new ArrayList<>();
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        if( items == null ) {
            return new HashMap<>();
        }
        for( WorkflowResponseItem i : items ) {
            String type = i.getItemType();
            if( type == null )
                type = "workflow.prompt.small";
            if( type.equals("workflow.browser.open")) {
                String urlString = i.getContent();
                UIHelper.executeInUI(() -> {
                    BrowserUtil.open(urlString);
                });
            } else if( type.equals("workflow.editor.open")) {
                UIHelper.executeInUI(() -> {
                    if (i.getProperties().get("workflow.editor.file.path") != null) {
                        EditorUtil.openFileInEditor(project, new File(i.getProperties().get("workflow.editor.file.path")));
                    } else if (i.getProperties().get("workflow.editor.file.content") != null) {
                        EditorUtil.createAndOpenVirtualFile(i.getId(), i.getProperties().get("workflow.editor.file.content"), project);
                    }
                });
            } else if( type.equals("workflow.terminal.open")) {
                UIHelper.executeInUI(() -> {
                    try {
                        String cmd = i.getProperties().get("workflow.terminal.cmd");
                        String[] asArr = CommandLineUtils.translateCommandline(cmd);
                        File wd = new File(System.getProperty("user.home"));
                        ExecHelper.executeWithTerminal(project, i.getId(), wd, false, Collections.emptyMap(), asArr);
                    } catch (IOException | CommandLineUtils.CommandLineException e) {
                        Messages.showErrorDialog(e.getMessage(), "Error running command in terminal");
                    }
                });
            } else if( type.equals("workflow.prompt.small") || type.equals("workflow.prompt.large")) {
                prompts.add(i);
            }
        }
        if( prompts.size() > 0 ) {
            final Boolean[] isOk = new Boolean[1];
            final Map<String, Object> values = new HashMap<String, Object>();
            UIHelper.executeInUI(() -> {
                WorkflowDialog wd = new WorkflowDialog(prompts.toArray(new WorkflowResponseItem[0]));
                boolean ok = wd.showAndGet();
                isOk[0] = ok;
                if(  ok ) {
                    values.putAll(wd.getAttributes());
                }
            });
            return isOk[0] ? values : null;
        }
        // Fallback impl
        return new HashMap<String, Object>();
    }


    public  static boolean workflowComplete(WorkflowResponse resp) {
        if( resp == null || resp.getStatus() == null) {
            return true;
        }
        int statusSev = resp.getStatus().getSeverity();
        if( statusSev == Status.CANCEL || statusSev == Status.ERROR ) {
            return true;
        }
        if( statusSev == Status.OK) {
            return true;
        }
        return false;
    }
}
