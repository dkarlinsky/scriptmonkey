package com.boxysystems.scriptmonkey.intellij.ui;

import com.boxysystems.scriptmonkey.intellij.Constants;
import com.boxysystems.scriptmonkey.intellij.ScriptMonkeyPluginClassLoader;
import com.boxysystems.scriptmonkey.intellij.ScriptMonkeyApplicationComponent;
import com.boxysystems.scriptmonkey.intellij.ScriptMonkeyPlugin;
import com.boxysystems.scriptmonkey.intellij.action.ExtensionBasedFileFilter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: shameed
 * Date: Sep 30, 2008
 * Time: 5:10:21 PM
 */
public class ScriptCommandProcessor implements ShellCommandProcessor {

    private static final Logger logger = Logger.getLogger(ScriptCommandProcessor.class);

    private volatile Map<String , ScriptEngine> extensionEngineMap = new HashMap<String, ScriptEngine>();
    private volatile Set<ScriptEngine> engines = new HashSet<ScriptEngine>();

    private CountDownLatch engineReady = new CountDownLatch(1);

    private volatile String prompt;


    private boolean commandShell = true;
    private Application application;
    private Project project;
    private ScriptMonkeyPlugin plugin;
    private ScriptMonkeyPluginClassLoader pluginClassLoader;

    public ScriptCommandProcessor(Application application) {
        this.application = application;
        createScriptEngines(null);
    }


    public ScriptCommandProcessor(Application application, Project project, ScriptMonkeyPlugin scriptMonkeyPlugin) {
        this.application = application;
        this.project = project;
        this.plugin = scriptMonkeyPlugin;
        this.pluginClassLoader = new ScriptMonkeyPluginClassLoader(plugin);
        createScriptEngines(scriptMonkeyPlugin);
    }

    public ExecutorService processScriptFile(final File scriptFile, final ScriptProcessorCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            public void run() {
                evaluateScriptFile(scriptFile, callback);
            }
        });
        executor.shutdown();
        return executor;
    }

    public void processScriptFileSynchronously(final File scriptFile, final ScriptProcessorCallback callback) {
        ExecutorService executor = processScriptFile(scriptFile, callback);
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Error while processing script file synchronously", e);
        }
    }

    private void evaluateScriptFile(File scriptFile, ScriptProcessorCallback callback) {
        try {
            initScriptingEngineAndRunGlobalScripts();
            Object result = null;
            if (scriptFile != null) {
                logger.info("Evaluating script file '" + scriptFile + "' ...");
                result = getEngine(guessLanguage(scriptFile)).eval(new FileReader(scriptFile));
            }
            callback.success(result);
        } catch (Throwable e) {
            callback.failure(e);
        }
    }

    public ScriptEngine getEngine(String language)
    {
        ScriptEngine scriptEngine = extensionEngineMap.get(language.toLowerCase());
        if(scriptEngine == null)
        {
            throw new IllegalArgumentException("no script engine for language: "+language);
        }

        return scriptEngine;
    }

    public String guessLanguage(File scriptFile)
    {
        String[] parts = scriptFile.getName().split("\\.");
        return parts[parts.length-1].toLowerCase();
    }

    public ScriptRunningTask processScript(final String scriptContent, String language, final ScriptProcessorCallback callback) {
        if (project != null) {
            ScriptRunningTask task = new ScriptRunningTask("Running script...", scriptContent, language,  callback);
            task.queue();
            return task;
        }
        return null;
    }

    private void initScriptingEngineAndRunGlobalScripts() {

        for (ScriptEngine engine : engines)
        {
            initScriptEngine(engine);
        }
        //runGlobalScripts();
    }

    public void processCommandLine() {
        new Thread(new Runnable() {
            public void run() {
                initScriptingEngineAndRunGlobalScripts();
                engineReady.countDown();
            }
        }).start();
    }

    public boolean isCommandShell() {
        return commandShell;
    }

    public void setCommandShell(boolean commandShell) {
        this.commandShell = commandShell;
    }

    public String executeCommand(String cmd, String language) {
        String res;
        try {
            engineReady.await();
            Object tmp = getEngine(language).eval(cmd);
            res = (tmp == null) ? null : tmp.toString();
        } catch (InterruptedException ie) {
            res = ie.getMessage();
        } catch (ScriptException se) {
            res = se.getMessage();
        }
        return res;
    }

    private void createScriptEngines(ScriptMonkeyPlugin scriptMonkeyPlugin) {
        ScriptEngineManager manager;
        if (scriptMonkeyPlugin != null && pluginClassLoader != null) {
            ScriptMonkeyPluginClassLoader augmentedClassLoader = pluginClassLoader.getAugmentedClassLoader();
            if (augmentedClassLoader != null) {
                Thread.currentThread().setContextClassLoader(augmentedClassLoader);
            }
            manager = createScriptEngineManager();
        } else {
            manager = createScriptEngineManager();
        }

        createAndRegisterEngine(manager, "JavaScript");
        createAndRegisterEngine(manager, "groovy");

        //String extension = engine.getFactory().getExtensions().get(0);
        prompt = "js" + ">";
    }

    private ScriptEngine createAndRegisterEngine(ScriptEngineManager manager, String language)
    {
        ScriptEngine engine = manager.getEngineByName(language);
        if (engine == null) {
            throw new RuntimeException("cannot load " + language + " engine");
        }
        List<String> extensions = engine.getFactory().getExtensions();
        engines.add(engine);
        for (String extension : extensions)
        {
            extensionEngineMap.put(extension.toLowerCase(), engine);
        }
        engine.setBindings(createGlobalBindings(), ScriptContext.ENGINE_SCOPE);
        return engine;
    }

    private ScriptEngineManager createScriptEngineManager() {
        return new ScriptEngineManager();
    }

    private void runGlobalScripts() {
        try {
            ScriptMonkeyApplicationComponent applicationComponent = ApplicationManager.getApplication().getComponent(ScriptMonkeyApplicationComponent.class);
            File jsFolder = new File(applicationComponent.getSettings().getHomeFolder(), Constants.JS_FOLDER_NAME);
            File globalScripts = new File(jsFolder, "global");

            if (globalScripts.exists()) {
                File[] jsFiles = globalScripts.listFiles(new ExtensionBasedFileFilter("js", "groovy"));
                for (File scriptFile : jsFiles) {
                    try {
                        logger.info("Evaluating script '" + scriptFile + "' ...");
                        getEngine(guessLanguage(scriptFile)).eval(new FileReader(scriptFile));
                        logger.info("Script successfully processed !");
                    } catch (ScriptException e) {
                        logger.error("Error executing script '" + scriptFile + "'", e);
                    } catch (FileNotFoundException e) {
                        logger.error("Unable to find script file '" + scriptFile + "' !");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bindings createGlobalBindings() {
        Map<String, Object> map =
                Collections.synchronizedMap(new HashMap<String, Object>());
        return new SimpleBindings(map);
    }

    private void initScriptEngine(ScriptEngine engine)
    {

        addGlobalVariable(engine, "engine", engine);
        addGlobalVariable(engine, "application", application);
        addGlobalVariable(engine, "project", project);
        addGlobalVariable(engine, "plugin", plugin);
    }

    public void addGlobalVariable(String language, String name, Object globalObject) {
        addGlobalVariable(getEngine(language), name, globalObject);
    }

    public void addGlobalVariable(String name, Object globalObject) {
        for (ScriptEngine engine : engines)
        {
            addGlobalVariable(engine, name, globalObject);
        }

    }


    private void addGlobalVariable(ScriptEngine engine, String name, Object globalObject) {
        if (name != null && globalObject != null) {
            engine.put(name, globalObject);
        }
    }

    public class ScriptRunningTask extends Task.Backgroundable {
        private ScriptProcessorCallback callback;
        private String scriptContent;
        private ExecutorService executor;
        private String language;

        public ScriptRunningTask(@NotNull String title, String scriptContent, String language, ScriptProcessorCallback callback) {
            super(project, title, false);
            this.scriptContent = scriptContent;
            this.callback = callback;
            this.setCancelText("Stop running scripts");
            this.language = language;
        }

        public void cancel() {
            executor.shutdownNow();
        }

        public boolean isRunning() {
            return !executor.isTerminated();
        }


        public void run(ProgressIndicator indicator) {
            executor = Executors.newFixedThreadPool(1);
            executor.execute(new Runnable() {
                public void run() {
                    initScriptingEngineAndRunGlobalScripts();
                    try {
                        Object result = null;
                        if (scriptContent != null) {
                            logger.info("Evaluating script ...");
                            result = getEngine(language).eval(scriptContent);
                        }
                        callback.success(result);
                    } catch (Throwable e) {
                        callback.failure(e);
                    }

                }
            });
        }
    }
}
