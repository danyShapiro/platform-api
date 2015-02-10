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
package com.codenvy.api.machine.v2.server.spi;

import java.util.Map;

/**
 * Describes set of keys that uniquely identifies in implementation specific way.
 *
 * @author andrew00x
 */
public interface ImageKey {
    Map<String, String> getFields();

    boolean equals(Object o);

    int hashCode();

    /** Serializes this {@code ImageKey} in JSON format. */
    String toJson();
}