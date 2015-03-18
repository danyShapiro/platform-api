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
package com.codenvy.api.project.server.handlers;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.server.FolderEntry;

import java.io.IOException;
import java.util.List;

/**
 * @author Vitaly Parfonov
 */
public interface GetModulesHandler extends ProjectHandler {

    void onGetModules(FolderEntry parentProjectFolder, List<String> modulesPath)
            throws ForbiddenException, ServerException, NotFoundException, IOException;

}