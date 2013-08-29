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
package com.codenvy.api.vfs.server.impl.memory.context;

import com.codenvy.commons.lang.NameGenerator;

import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.exceptions.LockException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.PropertyFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class MemoryFile extends MemoryItem {
    private byte[] bytes;
    private final AtomicReference<LockHolder> lockHolder = new AtomicReference<>();
    private long contentLastModificationDate;
    private final Object contentLock = new Object();

    private static class LockHolder {
        final String lockToken;
        final long   expired;

        LockHolder(String lockToken, long timeout) {
            this.lockToken = lockToken;
            this.expired = timeout > 0 ? (System.currentTimeMillis() + timeout) : Long.MAX_VALUE;
        }
    }

    public MemoryFile(String name, String mediaType, InputStream content) throws IOException {
        this(ObjectIdGenerator.generateId(), name, mediaType, content == null ? null : readContent(content));
    }

    private static byte[] readContent(InputStream content) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = content.read(buf)) != -1) {
            bout.write(buf, 0, r);
        }
        return bout.toByteArray();
    }

    MemoryFile(String id, String name, String mediaType, byte[] bytes) {
        super(id, name);
        setMediaType(mediaType);
        this.bytes = bytes;
        this.contentLastModificationDate = System.currentTimeMillis();
    }

    public final String lock(long timeout) throws VirtualFileSystemException {
        final String lockToken = NameGenerator.generate(null, 32);
        final LockHolder lock = new LockHolder(lockToken, timeout);
        if (!lockHolder.compareAndSet(null, lock)) {
            throw new LockException("File already locked. ");
        }
        lastModificationDate = System.currentTimeMillis();
        return lockToken;
    }

    public final void unlock(String lockToken) throws VirtualFileSystemException {
        if (lockToken == null) {
            throw new LockException("Null lock token. ");
        }
        final LockHolder myLock = lockHolder.get();
        if (myLock == null) {
            throw new LockException("File is not locked. ");
        }
        if (myLock.lockToken.equals(lockToken)) {
            lockHolder.compareAndSet(myLock, null);
            lastModificationDate = System.currentTimeMillis();
        } else {
            throw new LockException("Unable remove lock from file. Lock token does not match. ");
        }
    }

    public final boolean isLocked() {
        final LockHolder myLock = lockHolder.get();
        if (myLock == null) {
            return false;
        }
        if (myLock.expired < System.currentTimeMillis()) {
            // try replace lock
            return !lockHolder.compareAndSet(myLock, null);
        }
        return true;
    }

    public boolean isLockTokenMatched(String lockToken) {
        final LockHolder myLock = lockHolder.get();
        return myLock == null || myLock.lockToken.equals(lockToken);
    }

    public final ContentStream getContent() throws VirtualFileSystemException {
        synchronized (contentLock) {
            byte[] bytes = this.bytes;
            if (bytes == null) {
                bytes = new byte[0];
            }
            return new ContentStream(getName(), new ByteArrayInputStream(bytes), getMediaType(), bytes.length,
                                     new Date(contentLastModificationDate));
        }
    }

    public final void setContent(InputStream content) throws IOException {
        synchronized (contentLock) {
            byte[] bytes = null;
            if (content != null) {
                bytes = readContent(content);
            }
            this.bytes = bytes;
        }
        lastModificationDate = contentLastModificationDate = System.currentTimeMillis();
    }

    @Override
    public final boolean isFile() {
        return true;
    }

    @Override
    public final boolean isFolder() {
        return false;
    }

    @Override
    public final boolean isProject() {
        return false;
    }

    @Override
    public MemoryItem copy(MemoryFolder parent) throws VirtualFileSystemException {
        byte[] bytes = this.bytes;
        MemoryFile copy = new MemoryFile(ObjectIdGenerator.generateId(), getName(), getMediaType(), Arrays.copyOf(bytes, bytes.length));
        copy.updateProperties(getProperties(PropertyFilter.ALL_FILTER));
        copy.updateACL(getACL(), true);
        parent.addChild(copy);
        return copy;
    }

    public final String getLatestVersionId() {
        return getId();
    }

    public final String getVersionId() {
        return "0";
    }

    @Override
    public String toString() {
        return "MemoryFile{" +
               "id='" + getId() + '\'' +
               ", path=" + getPath() +
               ", name='" + getName() + '\'' +
               ", isLocked='" + isLocked() + '\'' +
               '}';
    }
}