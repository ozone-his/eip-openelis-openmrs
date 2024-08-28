/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDelete;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenelisTaskHandlerTest {

    @Mock
    private IGenericClient openelisFhirClient;

    @Mock
    private IUpdate iUpdate;

    @Mock
    private IUpdateTyped iUpdateTyped;

    @Mock
    private IUpdateExecutable iUpdateExecutable;

    @Mock
    private IDelete iDelete;

    @Mock
    private IDeleteTyped iDeleteTyped;

    @InjectMocks
    private OpenelisTaskHandler openelisTaskHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    void sendTask() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setId(taskID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(task);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openelisFhirClient.update()).thenReturn(iUpdate);
        when(iUpdate.resource(task)).thenReturn(iUpdateTyped);
        when(iUpdateTyped.encodedJson()).thenReturn(iUpdateExecutable);
        when(iUpdateExecutable.execute()).thenReturn(methodOutcome);

        // Act
        Task result = openelisTaskHandler.sendTask(task);

        // Verify
        assertNotNull(result);
        assertEquals(result.getResourceType(), ResourceType.Task);
        assertEquals(result.getId(), taskID);
    }

    @Test
    void getTaskByServiceRequestID() {}

    @Test
    void deleteTask() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openelisFhirClient.delete()).thenReturn(iDelete);
        when(iDelete.resourceById(new IdType("Task", taskID))).thenReturn(iDeleteTyped);
        when(iDeleteTyped.execute()).thenReturn(methodOutcome);

        // Act
        openelisTaskHandler.deleteTask(taskID);

        // Verify
        verify(openelisFhirClient, times(1)).delete();
    }
}
