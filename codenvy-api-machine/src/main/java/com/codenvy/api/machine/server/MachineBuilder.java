/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.server;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.dto.MachineMetadata;
import com.codenvy.dto.server.DtoFactory;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class for machine implementation builders.
 *
 * @author andrew00x
 */
public abstract class MachineBuilder {
    private String              machineId;
    private String              machineType;
    private MachineMetadataDao  machineMetadataDao;
    private MachineRecipe       recipe;
    private Set<File>           files;
    private Map<String, String> machineEnvironmentVariables;
    private Map<String, Object> buildOptions;
    private String              workspaceId;
    private String              displayName;
    private String              createdBy;
    private LineConsumer outputConsumer = LineConsumer.DEV_NULL;

    /**
     * Builds machine using supplied configuration.
     *
     * @throws ForbiddenException
     *         if machine can't be built due to misconfiguration
     * @throws ServerException
     *         if internal error occurs
     */
    public final Machine build() throws ServerException, ForbiddenException {
        if (machineId == null) {
            throw new ForbiddenException("Machine id is required");
        }
        if (workspaceId == null) {
            throw new ForbiddenException("Workspace id is required");
        }
        final Machine machine = doBuild();
        if (machineMetadataDao != null) {
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            machineMetadataDao.add(dtoFactory.createDto(MachineMetadata.class)
                                             .withId(machineId)
                                             .withCreatedBy(createdBy)
                                             .withWorkspaceId(workspaceId)
                                             .withDisplayName(displayName)
                                             .withType(machineType));
            machine.setMachineMetadataDao(machineMetadataDao);
        }
        machine.setOutputConsumer(outputConsumer);
        return machine;
    }

    protected abstract Machine doBuild() throws ServerException, ForbiddenException;

    //

    public final MachineBuilder setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public final MachineBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public final MachineBuilder setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /** Sets output consumer for machine output, including build machine output. */
    public final MachineBuilder setOutputConsumer(LineConsumer outputConsumer) throws IllegalArgumentException {
        if (outputConsumer == null) {
            throw new IllegalArgumentException("Output consumer can't be null");
        }
        this.outputConsumer = outputConsumer;
        return this;
    }

    public final MachineBuilder setRecipe(MachineRecipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public final MachineBuilder addFile(File file) {
        getFiles().add(file);
        return this;
    }

    public final MachineBuilder setMachineEnvironmentVariables(Map<String, String> machineEnvironmentVariables) {
        getMachineEnvironmentVariables().putAll(machineEnvironmentVariables);
        return this;
    }

    public final MachineBuilder setMachineEnvironmentVariables(String name, String value) {
        getMachineEnvironmentVariables().put(name, value);
        return this;
    }

    public final MachineBuilder setBuildOptions(Map<String, Object> parameters) {
        getBuildOptions().putAll(parameters);
        return this;
    }

    public final MachineBuilder setBuildOption(String name, Object value) {
        getBuildOptions().put(name, value);
        return this;
    }

    //

    protected final LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    protected final MachineRecipe getRecipe() {
        return recipe;
    }

    protected final Set<File> getFiles() {
        if (files == null) {
            files = new LinkedHashSet<>();
        }
        return this.files;
    }

    protected final Map<String, String> getMachineEnvironmentVariables() {
        if (this.machineEnvironmentVariables == null) {
            this.machineEnvironmentVariables = new HashMap<>();
        }
        return machineEnvironmentVariables;
    }

    protected final Map<String, Object> getBuildOptions() {
        if (buildOptions == null) {
            buildOptions = new HashMap<>();
        }
        return buildOptions;
    }

    protected final String getMachineId() {
        return machineId;
    }

    //

    final MachineBuilder setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    final MachineBuilder setMachineType(String machineType) {
        this.machineType = machineType;
        return this;
    }

    final MachineBuilder setMachineMetadataDao(MachineMetadataDao machineMetadataDao) {
        this.machineMetadataDao = machineMetadataDao;
        return this;
    }
}