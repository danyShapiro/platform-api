/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.newproj.server.event;

import com.codenvy.api.project.newproj.ProjectType;
import com.codenvy.api.project.server.FileEntry;

/**
 * @author gazarenkov
 */
public class GetFileEvent {

    private ProjectType projectType;

    private FileEntry file;

    public GetFileEvent(ProjectType projectType, FileEntry file) {
        this.projectType = projectType;
        this.file = file;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public FileEntry getFile() {
        return file;
    }

}
