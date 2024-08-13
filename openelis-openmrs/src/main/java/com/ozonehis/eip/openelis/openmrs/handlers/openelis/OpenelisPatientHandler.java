/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisPatientHandler {

    public Patient sendPatient(ProducerTemplate producerTemplate, Patient patient) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PATIENT_ID, patient.getIdPart());
        return producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-resource-route", patient, headers, Patient.class);
    }

    public Patient buildPatient(Patient patient) {
        Patient openelisPatient = new Patient();
        Identifier nationalID = new Identifier();
        nationalID.setSystem("http://openelis-global.org/pat_nationalId");
        nationalID.setValue(patient.getIdentifier().get(0).getValue());

        Identifier patGuid = new Identifier();
        patGuid.setSystem("http://openelis-global.org/pat_guid");
        patGuid.setValue(patient.getIdPart());

        Identifier patUuid = new Identifier();
        patUuid.setSystem("http://openelis-global.org/pat_uuid");
        patUuid.setValue(patient.getIdPart());

        Identifier subjectNumber = new Identifier();
        subjectNumber.setSystem("http://openelis-global.org/pat_subjectNumber");
        subjectNumber.setValue(String.valueOf(Math.round(Math.random() * 10000)));

        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(nationalID);
        identifierList.add(patGuid);
        identifierList.add(patUuid);
        identifierList.add(subjectNumber);

        openelisPatient.setId(patient.getIdPart());
        openelisPatient.setIdentifier(identifierList);
        openelisPatient.setName(patient.getName());
        openelisPatient.setTelecom(patient.getTelecom());
        openelisPatient.setActive(true);
        openelisPatient.setGender(patient.getGender());
        openelisPatient.setBirthDate(patient.getBirthDate());
        openelisPatient.setAddress(patient.getAddress());

        return openelisPatient;
    }
}
