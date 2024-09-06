/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenelisLocationHandlerTest {

    @Mock
    private IGenericClient openelisFhirClient;

    @Mock
    private IUpdate iUpdate;

    @Mock
    private IUpdateTyped iUpdateTyped;

    @Mock
    private IUpdateExecutable iUpdateExecutable;

    @InjectMocks
    private OpenelisLocationHandler openelisLocationHandler;

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
    void shouldSaveLocationInOpenelisGivenLocation() {
        // Setup
        String locationID = UUID.randomUUID().toString();
        Location location = new Location();
        location.setId(locationID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(location);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openelisFhirClient.update()).thenReturn(iUpdate);
        when(iUpdate.resource(location)).thenReturn(iUpdateTyped);
        when(iUpdateTyped.encodedJson()).thenReturn(iUpdateExecutable);
        when(iUpdateExecutable.execute()).thenReturn(methodOutcome);

        // Act
        Location result = openelisLocationHandler.sendLocation(location);

        // Verify
        assertNotNull(result);
        assertEquals(result.getResourceType(), ResourceType.Location);
        assertEquals(result.getId(), locationID);
    }
}
