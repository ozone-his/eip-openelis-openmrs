/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.PatientHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class PatientProcessor implements Processor {
    @Autowired
    private PatientHandler patientHandler;

    @Override
    public void process(Exchange exchange) {
        try {
            Message message = exchange.getMessage();
            Patient patient = message.getBody(Patient.class);

            if (patient == null) {
                return;
            }

            Identifier nationalID = new Identifier();
            nationalID.setSystem("http://openelis-global.org/pat_nationalId");
            nationalID.setValue(patient.getIdentifier().get(0).getValue());

            Identifier patGuid = new Identifier();
            patGuid.setSystem("http://openelis-global.org/pat_guid");
            patGuid.setValue(patient.getIdPart());

            Identifier patUuid = new Identifier();
            patUuid.setSystem("http://openelis-global.org/pat_uuid");
            patUuid.setValue(patient.getIdPart());

            List<Identifier> identifierList = new ArrayList<>();
            identifierList.add(nationalID);
            identifierList.add(patGuid);
            identifierList.add(patUuid);

            Patient openelisPatient = new Patient();
            openelisPatient.setId(patient.getIdPart());
            openelisPatient.setName(patient.getName());
            openelisPatient.setIdentifier(identifierList);
            openelisPatient.setTelecom(patient.getTelecom());
            openelisPatient.setActive(patient.getActive());
            openelisPatient.setAddress(patient.getAddress());
            openelisPatient.setBirthDate(patient.getBirthDate());
            openelisPatient.setGender(patient.getGender());

            exchange.getMessage().setBody(openelisPatient, Patient.class);

            // TODO: Implement PatientProcessor
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Patient", exchange, e);
        }
    }
}
