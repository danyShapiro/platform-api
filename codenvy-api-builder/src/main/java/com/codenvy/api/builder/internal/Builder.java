/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.LifecycleException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.util.CancellableProcessWrapper;
import com.codenvy.api.core.util.CommandLine;
import com.codenvy.api.core.util.ProcessUtil;
import com.codenvy.api.core.util.StreamPump;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super-class for all implementation of Builder.
 *
 * @author andrew00x
 */
public abstract class Builder implements Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    /**
     * Name of configuration parameter that points to the directory where all builds stored. Is such parameter is not specified then
     * 'java.io.tmpdir' used.
     */
    public static final String REPOSITORY              = "builder.build_repository";
    /**
     * Name of configuration parameter that sets the number of build workers. In other words it set the number of build
     * process that can be run at the same time. If this parameter is not set then the number of available processors
     * used, e.g. {@code Runtime.getRuntime().availableProcessors();}
     */
    public static final String NUMBER_OF_WORKERS       = "builder.workers_number";
    /**
     * Name of configuration parameter that sets time (in seconds) of keeping the results (artifact and logs) of build (by default 3600
     * seconds or 1 hour). After this time the results of build may be removed.
     */
    public static final String CLEAN_RESULT_DELAY_TIME = "builder.clean_result_delay_time";
    /**
     * Name of parameter that set the max size of build queue (by default 100). The number of build task in queue may not be greater than
     * provided by this parameter.
     */
    public static final String INTERNAL_QUEUE_SIZE     = "builder.internal_queue_size";

    private static final AtomicLong buildIdSequence = new AtomicLong(1);

    private final ConcurrentMap<Long, BuildTaskEntry>   tasks;
    private final ConcurrentLinkedQueue<BuildTaskEntry> tasksFIFO;
    private final ConcurrentLinkedQueue<java.io.File>   cleanerQueue;
    private final java.io.File                          rootDirectory;
    private final Set<BuildListener>                    buildListeners;
    private final int                                   cleanBuildResultDelay;
    private final int                                   queueSize;
    private final int                                   numberOfWorkers;

    private boolean                  started;
    private ScheduledExecutorService cleaner;

    private ThreadPoolExecutor executor;
    private java.io.File       repository;
    private java.io.File       builds;
    private SourcesManager     sourcesManager;

    public Builder(java.io.File rootDirectory, int numberOfWorkers, int queueSize, int cleanBuildResultDelay) {
        this.rootDirectory = rootDirectory;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSize = queueSize;
        this.cleanBuildResultDelay = cleanBuildResultDelay;
        buildListeners = new LinkedHashSet<>();
        tasks = new ConcurrentHashMap<>();
        tasksFIFO = new ConcurrentLinkedQueue<>();
        cleanerQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Returns the name of the builder. All registered builders should have unique name.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Returns the description of builder. Description should help client to recognize correct type of builder for an application.
     *
     * @return the description of builder
     */
    public abstract String getDescription();

    /**
     * Get result of FutureBuildTask. Getting result is implementation specific and mostly depends to build system, e.g. maven usually
     * stores build result in directory 'target' but it is not rule for ant. Regular users are not expected to use this method directly.
     * They should always use method {@link BuildTask#getResult()} instead.
     *
     * @param task
     *         task
     * @param successful
     *         reports whether build process terminated normally or not.
     *         Note: {@code true} is not indicated successful build but only normal process termination. Build itself may be unsuccessful
     *         because to compilation error, failed tests, etc.
     * @return BuildResult
     * @throws BuilderException
     *         if an error occurs when try to get result
     * @see BuildTask#getResult()
     */
    protected abstract BuildResult getTaskResult(FutureBuildTask task, boolean successful) throws BuilderException;

    protected abstract CommandLine createCommandLine(BuilderConfiguration config) throws BuilderException;

    public ExecutorService getExecutor() {
        return executor;
    }

    protected BuildLogger createBuildLogger(BuilderConfiguration buildConfiguration, java.io.File logFile) throws BuilderException {
        try {
            return new DefaultBuildLogger(logFile, "text/plain");
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }


    /** Initialize Builder. Sub-classes should invoke {@code super.start} at the begin of this method. */
    @PostConstruct
    @Override
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        repository = new java.io.File(rootDirectory, getName());
        if (!(repository.exists() || repository.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", repository.getAbsolutePath()));
        }
        final java.io.File sources = new java.io.File(repository, "sources");
        if (!(sources.exists() || sources.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", sources.getAbsolutePath()));
        }
        builds = new java.io.File(repository, "builds");
        if (!(builds.exists() || builds.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", builds.getAbsolutePath()));
        }
        sourcesManager = new SourcesManagerImpl(sources);
        executor = new MyThreadPoolExecutor(numberOfWorkers, queueSize);
        cleaner = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName() + "-BuilderCleaner-", true));
        cleaner.scheduleAtFixedRate(new CleanTask(), cleanBuildResultDelay, cleanBuildResultDelay, TimeUnit.SECONDS);
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalArgumentException("Lifecycle instance is not started yet.");
        }
    }

    /**
     * Stops builder and releases any resources associated with the Builder.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
    @PreDestroy
    @Override
    public synchronized void stop() {
        checkStarted();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            cleaner.shutdownNow();

            // Remove all build results.
            final java.io.File[] files = getRepository().listFiles();
            if (files != null && files.length > 0) {
                for (java.io.File f : files) {
                    boolean deleted;
                    if (f.isDirectory()) {
                        deleted = IoUtil.deleteRecursive(f);
                    } else {
                        deleted = f.delete();
                    }
                    if (!deleted) {
                        LOG.warn("Failed delete {}", f);
                    }
                }
            }
        }
        tasks.clear();
        tasksFIFO.clear();
        cleanerQueue.clear();
        buildListeners.clear();
        started = false;
    }

    public java.io.File getRepository() {
        checkStarted();
        return repository;
    }

    public java.io.File getBuildDirectory() {
        checkStarted();
        return builds;
    }

    public SourcesManager getSourcesManager() {
        checkStarted();
        return sourcesManager;
    }

    public java.io.File getSourcesDirectory() {
        checkStarted();
        return getSourcesManager().getDirectory();
    }

    /**
     * Add new BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was added
     */
    public boolean addBuildListener(BuildListener listener) {
        synchronized (buildListeners) {
            return buildListeners.add(listener);
        }
    }

    /**
     * Remove BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was removed
     */
    public boolean removeBuildListener(BuildListener listener) {
        synchronized (buildListeners) {
            return buildListeners.remove(listener);
        }
    }

    /**
     * Get all registered build listeners. Modifications to the returned {@code Set} will not affect the internal {@code Set}.
     *
     * @return all available download plugins
     */
    public Set<BuildListener> getBuildListeners() {
        synchronized (buildListeners) {
            return new LinkedHashSet<>(buildListeners);
        }
    }

    public BuilderConfigurationFactory getBuilderConfigurationFactory() {
        return new DefaultBuilderConfigurationFactory(this);
    }

    /**
     * Starts new build process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(BuildRequest request) throws BuilderException {
        checkStarted();
        final BuilderConfiguration configuration = getBuilderConfigurationFactory().createBuilderConfiguration(request);
        final java.io.File workDir = configuration.getWorkDir();
        final java.io.File logFile = new java.io.File(workDir.getParentFile(), workDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(configuration, logFile);
        final String webHookUrl = request.getWebHookUrl();
        return execute(configuration, webHookUrl == null ? null : new WebHookCallback(webHookUrl), logger);
    }

    /**
     * Starts new process of analysis dependencies.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(DependencyRequest request) throws BuilderException {
        checkStarted();
        final BuilderConfiguration configuration = getBuilderConfigurationFactory().createBuilderConfiguration(request);
        final java.io.File workDir = configuration.getWorkDir();
        final java.io.File logFile = new java.io.File(workDir.getParentFile(), workDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(configuration, logFile);
        final String webHookUrl = request.getWebHookUrl();
        return execute(configuration, webHookUrl == null ? null : new WebHookCallback(webHookUrl), logger);
    }

    protected BuildTask execute(BuilderConfiguration configuration, BuildTask.Callback callback, BuildLogger logger)
            throws BuilderException {
        final CommandLine commandLine = createCommandLine(configuration);
        final Callable<Boolean> callable = createTaskFor(commandLine, logger, configuration.getRequest().getTimeout(), configuration);
        final FutureBuildTask task =
                new FutureBuildTask(callable, buildIdSequence.getAndIncrement(), commandLine, getName(), configuration, logger, callback);
        final long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cleanBuildResultDelay);
        final BuildTaskEntry cachedTask = new BuildTaskEntry(task, expirationTime);
        purgeExpiredTasks();
        tasks.put(task.getId(), cachedTask);
        tasksFIFO.offer(cachedTask);
        executor.execute(task);
        return task;
    }

    protected Callable<Boolean> createTaskFor(final CommandLine commandLine,
                                              final BuildLogger logger,
                                              final long timeout,
                                              final BuilderConfiguration configuration) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                BaseBuilderRequest request = configuration.getRequest();
                getSourcesManager()
                        .getSources(request.getWorkspace(), request.getProject(), request.getSourcesUrl(), configuration.getWorkDir());
                StreamPump output = null;
                Watchdog watcher = null;
                int result = -1;
                try {
                    final Process process = Runtime.getRuntime().exec(commandLine.toShellCommand(), null, configuration.getWorkDir());
                    if (timeout > 0) {
                        watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", timeout, TimeUnit.SECONDS);
                        watcher.start(new CancellableProcessWrapper(process));
                    }
                    output = new StreamPump();
                    output.start(process, logger);
                    try {
                        result = process.waitFor();
                    } catch (InterruptedException e) {
                        Thread.interrupted(); // we interrupt thread when cancel task
                        ProcessUtil.kill(process);
                    }
                } finally {
                    if (watcher != null) {
                        watcher.stop();
                    }
                    if (output != null) {
                        output.stop();
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.info("Done: {}, exit code: {}", commandLine, result);
                }
                return result == 0;
            }
        };
    }

    public int getNumberOfWorkers() {
        checkStarted();
        return executor.getCorePoolSize();
    }

    public int getNumberOfActiveWorkers() {
        checkStarted();
        return executor.getActiveCount();
    }

    public int getInternalQueueSize() {
        checkStarted();
        return executor.getQueue().size();
    }

    public int getMaxInternalQueueSize() {
        checkStarted();
        return queueSize;
    }

    /** Removes expired tasks. */
    private void purgeExpiredTasks() {
        int num = 0;
        for (Iterator<BuildTaskEntry> i = tasksFIFO.iterator(); i.hasNext(); ) {
            final BuildTaskEntry next = i.next();
            if (!next.isExpired()) {
                // Don't need to check other tasks if find first one that is not expired yet.
                break;
            }
            if (!next.task.isDone()) {
                try {
                    next.task.cancel();
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                    continue; // try next time
                }
            }
            i.remove();
            tasks.remove(next.task.getId());
            try {
                cleanup(next.task);
            } catch (RuntimeException e) {
                LOG.error(e.getMessage(), e);
            }
            num++;
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks", num);
        }
    }

    /**
     * Cleanup task. Cleanup means removing all local files which were created by build process, e.g logs, sources, build reports, etc.
     * <p/>
     * Sub-classes should invoke {@code super.cleanup} at the start of this method.
     *
     * @param task
     *         build task
     */
    protected void cleanup(BuildTask task) {
        final java.io.File workDir = task.getConfiguration().getWorkDir();
        if (workDir != null && workDir.exists()) {
            cleanerQueue.offer(workDir);
        }
        final java.io.File log = task.getBuildLogger().getFile();
        if (log != null && log.exists()) {
            cleanerQueue.offer(log);
        }
        BuildResult result = null;
        try {
            result = task.getResult();
        } catch (BuilderException e) {
            LOG.error("Skip cleanup of the task {}. Unable get task result.", task);
        }
        if (result != null) {
            List<java.io.File> artifacts = result.getResults();
            if (!artifacts.isEmpty()) {
                for (java.io.File artifact : artifacts) {
                    if (artifact.exists()) {
                        cleanerQueue.offer(artifact);
                    }
                }
            }
            if (result.hasBuildReport()) {
                java.io.File report = result.getBuildReport();
                if (report != null && report.exists()) {
                    cleanerQueue.offer(report);
                }
            }
        }
    }

    /**
     * Get build task by its {@code id}. Typically build process takes some time, so client start process of build or analyze dependencies
     * and periodically check is process already done. Client also may use {@link BuildListener} to be notified when build process starts
     * or
     * ends.
     *
     * @param id
     *         id of BuildTask
     * @return BuildTask
     * @throws NoSuchBuildTaskException
     *         if id of BuildTask is invalid
     * @see #addBuildListener(BuildListener)
     * @see #removeBuildListener(BuildListener)
     */
    public final BuildTask getBuildTask(Long id) throws NoSuchBuildTaskException {
        checkStarted();
        final BuildTaskEntry e = tasks.get(id);
        if (e == null) {
            throw new NoSuchBuildTaskException(id);
        }
        return e.task;
    }

    protected class FutureBuildTask extends FutureTask<Boolean> implements BuildTask {
        private final Long                 id;
        private final CommandLine          commandLine;
        private final String               builder;
        private final BuilderConfiguration configuration;
        private final BuildLogger          buildLogger;
        private final Callback             callback;

        private BuildResult result;
        private long        startTime;

        protected FutureBuildTask(Callable<Boolean> callable,
                                  Long id,
                                  CommandLine commandLine,
                                  String builder,
                                  BuilderConfiguration configuration,
                                  BuildLogger buildLogger,
                                  Callback callback) {
            super(callable);
            this.id = id;
            this.commandLine = commandLine;
            this.builder = builder;
            this.configuration = configuration;
            this.buildLogger = buildLogger;
            this.callback = callback;
            startTime = -1L;
        }

        @Override
        public final Long getId() {
            return id;
        }

        @Override
        public String getBuilder() {
            return builder;
        }

        public CommandLine getCommandLine() {
            return commandLine;
        }

        @Override
        public BuildLogger getBuildLogger() {
            return buildLogger;
        }

        @Override
        public void cancel() {
            super.cancel(true);
        }

        @Override
        protected void done() {
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.done(FutureBuildTask.this);
                    }
                });
            }
        }

        @Override
        public final BuildResult getResult() throws BuilderException {
            if (!isDone()) {
                return null;
            }
            if (result == null) {
                boolean successful;
                try {
                    successful = super.get();
                } catch (InterruptedException e) {
                    // Should not happen since we checked is task done or not.
                    Thread.currentThread().interrupt();
                    successful = false;
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof Error) {
                        throw (Error)cause; // lets caller to get Error as is
                    } else if (cause instanceof BuilderException) {
                        throw (BuilderException)cause;
                    } else {
                        throw new BuilderException(cause.getMessage(), cause);
                    }
                } catch (CancellationException ce) {
                    successful = false;
                }

                result = Builder.this.getTaskResult(this, successful);
            }
            return result;
        }

        @Override
        public BuilderConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public final synchronized boolean isStarted() {
            return startTime > 0;
        }

        @Override
        public final synchronized long getStartTime() {
            return startTime;
        }

        final synchronized void started() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "FutureBuildTask{" +
                   "id=" + id +
                   ", builder='" + builder + '\'' +
                   ", workDir=" + configuration.getWorkDir() +
                   '}';
        }
    }

    private class MyThreadPoolExecutor extends ThreadPoolExecutor {
        private MyThreadPoolExecutor(int workerNumber, int queueSize) {
            super(workerNumber, workerNumber, 0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>(queueSize),
                  new NamedThreadFactory(Builder.this.getName() + "-Builder-", true),
                  new ManyBuildTasksRejectedExecutionPolicy(new AbortPolicy()));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (r instanceof FutureBuildTask) {
                final FutureBuildTask futureBuildTask = (FutureBuildTask)r;
                for (BuildListener buildListener : getBuildListeners()) {
                    try {
                        buildListener.begin(futureBuildTask);
                    } catch (RuntimeException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                futureBuildTask.started();
            }
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (r instanceof FutureBuildTask) {
                final FutureBuildTask futureBuildTask = (FutureBuildTask)r; // We know it is FutureBuildTask
                for (BuildListener buildListener : getBuildListeners()) {
                    try {
                        buildListener.end(futureBuildTask);
                    } catch (RuntimeException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private class CleanTask implements Runnable {
        public void run() {
            LOG.debug("clean {}: remove {} files", new java.util.Date(), cleanerQueue.size());
            Set<java.io.File> failToDelete = new LinkedHashSet<>();
            java.io.File f;
            while ((f = cleanerQueue.poll()) != null) {
                if (f.isDirectory()) {
                    if (!IoUtil.deleteRecursive(f)) {
                        if (f.exists()) {
                            failToDelete.add(f);
                        }
                    }
                } else {
                    if (!f.delete()) {
                        if (f.exists()) {
                            failToDelete.add(f);
                        }
                    }
                }
            }
            if (!failToDelete.isEmpty()) {
                LOG.debug("clean: could remove {} files, try next time", failToDelete.size());
                cleanerQueue.addAll(failToDelete);
            }
        }
    }

    private static final class BuildTaskEntry {
        private final long            expirationTime;
        private final int             hash;
        private final FutureBuildTask task;

        private BuildTaskEntry(FutureBuildTask task, long expirationTime) {
            this.task = task;
            this.expirationTime = expirationTime;
            this.hash = 7 * 31 + task.getId().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BuildTaskEntry) {
                return task.getId().equals(((BuildTaskEntry)o).task.getId());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "BuildTaskEntry{" +
                   "task=" + task +
                   '}';
        }
    }

    private static class WebHookCallback implements BuildTask.Callback {
        final String url;

        WebHookCallback(String url) {
            this.url = url;
        }

        @Override
        public void done(BuildTask task) {
            try {
                HttpJsonHelper.post(null, url, null);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
