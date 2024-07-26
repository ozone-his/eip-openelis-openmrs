/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class PatientHandler {

    public Patient sendPatient(ProducerTemplate producerTemplate, Patient patient) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PATIENT_ID, patient.getIdPart());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-patient-route", patient, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Patient savedPatient = ctx.newJsonParser().parseResource(Patient.class, response);
        return savedPatient;
    }
}
