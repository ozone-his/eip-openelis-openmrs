/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPatientHandler;
import java.util.Collections;
import java.util.Date;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class PatientProcessorTest extends BaseProcessorTest {

    @Mock
    private OpenelisPatientHandler openelisPatientHandler;

    @InjectMocks
    private PatientProcessor patientProcessor;

    private static AutoCloseable mocksCloser;

    private static final String ADDRESS_ID = "12377e18-a051-487b-8dd3-4cffcddb2a9c";

    private static final String PATIENT_ID = "866f25bf-d930-4886-9332-75443047e38e";

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldSavePatientInOpenelisGivenPatient() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setId(patient.getIdPart());
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setIdentifier(Collections.singletonList(new Identifier().setValue("10000Y")));
        patient.setName(Collections.singletonList(
                new HumanName().setFamily("Doe").setGiven(Collections.singletonList(new StringType("John")))));
        patient.setTelecom(Collections.singletonList(new ContactPoint().setValue("9876543210")));
        patient.setActive(true);
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(new Date());
        patient.setAddress(Collections.singletonList(address));

        Patient openelisPatient = patient;

        Exchange exchange = createExchange(patient, "c");

        // Mock behavior
        when(openelisPatientHandler.buildPatient(patient)).thenReturn(openelisPatient);
        when(openelisPatientHandler.sendPatient(openelisPatient)).thenReturn(openelisPatient);

        // Act
        patientProcessor.process(exchange);

        // Assert
        verify(openelisPatientHandler, times(1)).buildPatient(patient);
        verify(openelisPatientHandler, times(1)).sendPatient(openelisPatient);
    }

    @Test
    void shouldNotSavePatientInOpenelisWhenPatientIsNull() {
        // Arrange
        Patient patient = null;

        Exchange exchange = createExchange(patient, "c");

        // Act
        patientProcessor.process(exchange);

        // Assert
        verify(openelisPatientHandler, times(0)).buildPatient(patient);
    }
}
