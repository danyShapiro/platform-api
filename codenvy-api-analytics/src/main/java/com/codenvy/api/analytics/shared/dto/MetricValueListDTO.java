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
package com.codenvy.api.analytics.shared.dto;

import java.util.List;

import com.codenvy.dto.shared.DTO;

/** @author Alexander Reshetnyak */
@DTO
public interface MetricValueListDTO {
    List<MetricValueDTO> getMetrics();

    void setMetrics(List<MetricValueDTO> metricValueDTOs);
}