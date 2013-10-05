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
package com.codenvy.api.builder;

import java.util.List;

/**
 * Sorts List of remote builder according to some order with depends on implementation. BuildQueue uses implementation of this interface fo
 * find the 'best' slave-builder for processing incoming build request. If more then one slave-builder available then BuildQueue collects
 * them (their front-ends which are represented by RemoteBuilder) and passes to implementation of this interface. After sort BuildQueue get
 * first one from the list and send build request to it.
 * <p/>
 * FQN of implementation of this interface must be placed in file META-INF/services/com.codenvy.api.builder.BuilderListSorter
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface BuilderListSorter {
    void sort(List<RemoteBuilder> remoteBuilders);
}
