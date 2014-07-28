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
package com.codenvy.api.account.shared.dto;

import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Describes subscription - a link between {@link com.codenvy.api.account.server.SubscriptionService} and {@link
 * com.codenvy.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 */
@DTO
public interface SubscriptionDescriptor {
    String getId();

    void setId(String id);

    SubscriptionDescriptor withId(String id);

    String getAccountId();

    void setAccountId(String orgId);

    SubscriptionDescriptor withAccountId(String orgId);

    String getServiceId();

    void setServiceId(String id);

    SubscriptionDescriptor withServiceId(String id);

    long getStartDate();

    void setStartDate(long date);

    SubscriptionDescriptor withStartDate(long date);

    long getEndDate();

    void setEndDate(long date);

    SubscriptionDescriptor withEndDate(long date);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    SubscriptionDescriptor withProperties(Map<String, String> properties);

    Subscription.State getState();

    void setState(Subscription.State state);

    SubscriptionDescriptor withState(Subscription.State state);

    void setLinks(List<Link> links);

    List<Link> getLinks();

    SubscriptionDescriptor withLinks(List<Link> links);
}