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
package com.codenvy.api.runner;

import com.codenvy.api.project.shared.dto.RunnerEnvironment;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentLeaf;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentTree;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunnerServiceTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    @Mock
    private RunQueue runQueue;
    @InjectMocks
    private RunnerService service;

    @BeforeMethod
    public void beforeMethod() {
        doNothing().when(runQueue).checkStarted();
    }

    @AfterMethod
    public void afterMethod() {
    }

    @Test
    public void testGetRunnerEnvironments() throws Exception {
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();
        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        RunnerEnvironmentTree system = service.getRunnerEnvironments("my_ws", null);

        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);

        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        assertEquals(java.getDisplayName(), "java");
        nodes = java.getNodes();
        assertEquals(nodes.size(), 1);
        assertEquals(java.getLeaves().size(), 0);

        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        assertEquals(web.getLeaves().size(), 2);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
        RunnerEnvironment tomcat7Environment = tomcat7.getEnvironment();
        RunnerEnvironment jboss7Environment = jboss7.getEnvironment();
        assertNotNull(tomcat7Environment);
        assertNotNull(jboss7Environment);
        assertEquals(tomcat7Environment.getId(), "system:/java/web/tomcat7");
        assertEquals(jboss7Environment.getId(), "system:/java/web/jboss7");
    }

    @Test
    public void testGetRunnerEnvironmentsIfOneServerUnavailable() throws Exception {
        List<RemoteRunnerServer> servers = new ArrayList<>(2);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        doThrow(new RunnerException("Connection refused")).when(server2).getRunnerDescriptors();
        servers.add(server2);

        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        RunnerEnvironmentTree system = service.getRunnerEnvironments("my_ws", null);

        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);

        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        assertEquals(java.getDisplayName(), "java");
        nodes = java.getNodes();
        assertEquals(nodes.size(), 1);
        assertEquals(java.getLeaves().size(), 0);

        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        assertEquals(web.getLeaves().size(), 2);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
        RunnerEnvironment tomcat7Environment = tomcat7.getEnvironment();
        RunnerEnvironment jboss7Environment = jboss7.getEnvironment();
        assertNotNull(tomcat7Environment);
        assertNotNull(jboss7Environment);
        assertEquals(tomcat7Environment.getId(), "system:/java/web/tomcat7");
        assertEquals(jboss7Environment.getId(), "system:/java/web/jboss7");
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}
