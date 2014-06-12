package com.boxysystems.scriptmonkey.intellij;

import com.boxysystems.scriptmonkey.intellij.action.ClearEditorAction;
import com.boxysystems.scriptmonkey.intellij.action.OpenHelpAction;
import com.boxysystems.scriptmonkey.intellij.action.ShowScriptMonkeyConfigurationAction;
import com.boxysystems.scriptmonkey.intellij.ui.ScriptCommandProcessor;
import com.boxysystems.scriptmonkey.intellij.ui.ScriptMonkeyToolWindow;
import com.boxysystems.scriptmonkey.intellij.ui.ScriptShellPanel;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: shameed
 * Date: Oct 6, 2008
 * Time: 2:23:29 PM
 */
public class ScriptMonkeyPlugin implements ProjectComponent {

    private Project project;

    private ScriptMonkeyToolWindow toolWindow = null;

    private IdeaPluginDescriptor pluginDescriptor;

    private Map<String, ScriptShellPanel> commandShellPanels  = new HashMap<String, ScriptShellPanel>();


    public ScriptMonkeyPlugin(Project project) throws MalformedURLException {
        this.project = project;
        initPluginDescriptor();
    }

    private void initPluginDescriptor() {
        pluginDescriptor = PluginManager.getPlugin(PluginId.getId(Constants.PLUGIN_ID));
    }

    public void projectOpened() {
        toolWindow = new ScriptMonkeyToolWindow(project);
        ScriptCommandProcessor commandProcessor = new ScriptCommandProcessor(ApplicationManager.getApplication(), project, this);

        ClearEditorAction clearEditorAction = new ClearEditorAction();
        ShowScriptMonkeyConfigurationAction showConfigurationAction = new ShowScriptMonkeyConfigurationAction();
        OpenHelpAction openHelpAction = new OpenHelpAction();

        AnAction commandShellActions[] = {clearEditorAction, showConfigurationAction, openHelpAction};

        createCommandShell(commandProcessor, clearEditorAction, commandShellActions, "js");
        createCommandShell(commandProcessor, clearEditorAction, commandShellActions, "groovy");

    }

    private void createCommandShell(ScriptCommandProcessor commandProcessor, ClearEditorAction clearEditorAction, AnAction[] commandShellActions, String language)
    {
        ScriptShellPanel commandShellPanel = new ScriptShellPanel(commandProcessor, language,  commandShellActions);
        commandShellPanel.applySettings(ScriptMonkeyApplicationComponent.getInstance().getSettings());
        clearEditorAction.setScriptShellPanel(commandShellPanel);
        commandProcessor.processCommandLine();
        commandProcessor.addGlobalVariable("window", commandShellPanel);
        commandShellPanels.put(language, commandShellPanel);
        toolWindow.addContentPanel(language.toUpperCase()+" Shell", commandShellPanel);
    }

    public Project getProject() {
        return project;
    }

    public ScriptMonkeyToolWindow getToolWindow() {
        return toolWindow;
    }

    public void projectClosed() {
        if (toolWindow != null) {
            toolWindow.unregisterToolWindow();
        }
    }

    public void initComponent() {
        // empty
    }

    public void disposeComponent() {
        // empty
    }

    @NotNull
    public String getComponentName() {
        return this.getClass().getName();
    }

    public static ScriptMonkeyPlugin getInstance(Project project) {
        return project.getComponent(ScriptMonkeyPlugin.class);
    }

    public String toString() {
        return "Name:" + pluginDescriptor.getName() + ",Version:" + pluginDescriptor.getVersion() + ",Vendor:" + pluginDescriptor.getVendor();
    }

    public ScriptShellPanel getCommandShellPanel(String language) {
        ScriptShellPanel scriptShellPanel = commandShellPanels.get(language);
        if(scriptShellPanel == null)
        {
            throw new IllegalArgumentException("no command shell for language: "+language);
        }
        return scriptShellPanel;
    }

    public Collection<ScriptShellPanel> getCommandShellPanels()
    {
        return commandShellPanels.values();
    }
}
