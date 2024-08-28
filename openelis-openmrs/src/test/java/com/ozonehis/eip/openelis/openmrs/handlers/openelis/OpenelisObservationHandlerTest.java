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

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenelisObservationHandlerTest {
    @Mock
    private IGenericClient openelisFhirClient;

    @Mock
    private IRead iRead;

    @Mock
    private IReadTyped<Observation> iReadTyped;

    @Mock
    private IReadExecutable<Observation> iReadExecutable;

    @InjectMocks
    private OpenelisObservationHandler openelisObservationHandler;

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
    void getObservationByObservationID() {
        // Setup
        String observationID = UUID.randomUUID().toString();
        Observation observation = new Observation();
        observation.setId(observationID);

        // Mock behavior
        when(openelisFhirClient.read()).thenReturn(iRead);
        when(iRead.resource(Observation.class)).thenReturn(iReadTyped);
        when(iReadTyped.withId(observationID)).thenReturn(iReadExecutable);
        when(iReadExecutable.execute()).thenReturn(observation);

        // Act
        Observation result = openelisObservationHandler.getObservationByObservationID(observationID);

        // Verify
        assertNotNull(result);
        assertEquals(observationID, result.getId());
    }
}
