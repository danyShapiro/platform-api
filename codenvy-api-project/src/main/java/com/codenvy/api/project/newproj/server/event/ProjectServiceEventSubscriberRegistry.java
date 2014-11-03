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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectServiceEventSubscriberRegistry {

    @Inject
    @Nullable
    private Set<GetFileEventSubcriber> getFileEventSubcribers;

    public Set<GetFileEventSubcriber> getGetFileEventSubcribers() {

        return this.getFileEventSubcribers == null ? new HashSet<GetFileEventSubcriber>() : getFileEventSubcribers;
    }

}
