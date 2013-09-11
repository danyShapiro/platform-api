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

import com.codenvy.api.vfs.shared.ExitCodes;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/** @author <a href="mailto:vparfonov@exoplatform.com">Vitaly Parfonov</a> */
@Provider
public class GitUrlResolveExceptionMapper implements ExceptionMapper<GitUrlResolveException> {
    /** {@inheritDoc} */
    @Override
    public Response toResponse(GitUrlResolveException exception) {
        return Response.status(Response.Status.NOT_FOUND).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN)
                       .header("X-Exit-Code", Integer.toString(ExitCodes.ITEM_NOT_FOUND)).build();
    }
}
