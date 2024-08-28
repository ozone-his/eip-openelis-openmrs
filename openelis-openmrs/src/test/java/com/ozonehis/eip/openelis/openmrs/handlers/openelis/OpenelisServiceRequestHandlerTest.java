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
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenelisServiceRequestHandlerTest {

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
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

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
    void sendServiceRequest() {
        // Setup
        String serviceRequestID = UUID.randomUUID().toString();
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(serviceRequestID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(serviceRequest);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openelisFhirClient.update()).thenReturn(iUpdate);
        when(iUpdate.resource(serviceRequest)).thenReturn(iUpdateTyped);
        when(iUpdateTyped.encodedJson()).thenReturn(iUpdateExecutable);
        when(iUpdateExecutable.execute()).thenReturn(methodOutcome);

        // Act
        ServiceRequest result = openelisServiceRequestHandler.sendServiceRequest(serviceRequest);

        // Verify
        assertNotNull(result);
        assertEquals(result.getResourceType(), ResourceType.ServiceRequest);
        assertEquals(result.getId(), serviceRequestID);
    }

    @Test
    void deleteServiceRequest() {
        // Setup
        String serviceRequestID = UUID.randomUUID().toString();
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openelisFhirClient.delete()).thenReturn(iDelete);
        when(iDelete.resourceById(new IdType("ServiceRequest", serviceRequestID)))
                .thenReturn(iDeleteTyped);
        when(iDeleteTyped.execute()).thenReturn(methodOutcome);

        // Act
        openelisServiceRequestHandler.deleteServiceRequest(serviceRequestID);

        // Verify
        verify(openelisFhirClient, times(1)).delete();
    }
}
