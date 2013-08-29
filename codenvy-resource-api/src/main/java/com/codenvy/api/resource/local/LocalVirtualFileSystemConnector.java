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
 * patents in process, and are public by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.resource.local;

import com.codenvy.api.resource.File;
import com.codenvy.api.resource.Folder;
import com.codenvy.api.resource.Project;
import com.codenvy.api.resource.Resource;
import com.codenvy.api.resource.VirtualFileSystemConnector;
import com.codenvy.api.resource.attribute.Attributes;

import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.Lock;

import java.io.IOException;
import java.io.InputStream;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class LocalVirtualFileSystemConnector extends VirtualFileSystemConnector {
    private final VirtualFileSystem vfs;

    public LocalVirtualFileSystemConnector(String name, VirtualFileSystem vfs) {
        super(name);
        this.vfs = vfs;
    }

    @Override
    public Folder getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(Folder parent, String relPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource[] getChildResources(Folder parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File createFile(Folder parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder createFolder(Folder parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project createProject(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project createProject(Project parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getContentStream(File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateContentStream(File file, InputStream data, String contentType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Attributes getAttributes(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAttributes(Resource resource, Attributes attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource rename(Resource resource, String newname, String contentType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource move(Resource resource, Folder newparent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock(File file, String lockToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item getVfsItem(Resource resource) {
        throw new UnsupportedOperationException();
    }
}