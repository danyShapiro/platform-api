/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Attaches any point on backend filesystem some VirtualFile (root folder). Only children of root folder may be accessible through this
 * API.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface MountPoint {
    /**
     * Get root folder of virtual file system. Any files in higher level than root are not accessible through virtual file system API.
     *
     * @return root folder of virtual file system
     */
    VirtualFile getRoot();

    /**
     * Get VirtualFile by <code>path</code>.
     *
     * @param path
     *         path of virtual file
     * @return VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.ItemNotFoundException
     *         if <code>path</code> does not exist
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user which perform operation has no permissions to do it
     * @throws VirtualFileSystemException
     *         if any other errors occur
     */
    VirtualFile getVirtualFile(String path) throws VirtualFileSystemException;

    /**
     * Get VirtualFile by <code>id</code>.
     *
     * @param id
     *         id of virtual file
     * @return VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.ItemNotFoundException
     *         if <code>id</code> does not exist
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user which perform operation has no permissions to do it
     * @throws VirtualFileSystemException
     *         if any other errors occur
     */
    VirtualFile getVirtualFileById(String id) throws VirtualFileSystemException;

    /** Call after unmount this MountPoint, e.g. clear caches */
    void reset();
}