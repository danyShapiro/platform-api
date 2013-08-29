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
package com.codenvy.api.vfs.server.exceptions;

/**
 * This exception occurs in case LocalPathResolver in some reason can't resolve the path.
 * <p/>
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:vparfonov@exoplatform.com">Vitaly Parfonov</a>
 */
@SuppressWarnings("serial")
public class GitUrlResolveException extends VirtualFileSystemException {

    public GitUrlResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitUrlResolveException(String message) {
        super(message);
    }

}