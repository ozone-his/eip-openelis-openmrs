/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPatientHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class PatientProcessor implements Processor {
    @Autowired
    private OpenelisPatientHandler openelisPatientHandler;

    @Override
    public void process(Exchange exchange) {
        try {
            Message message = exchange.getMessage();
            Patient patient = message.getBody(Patient.class);

            if (patient == null) {
                return;
            }

            Patient openelisPatient = openelisPatientHandler.buildPatient(patient);
            Patient savedOpenelisPatient = openelisPatientHandler.sendPatient(openelisPatient);
            log.debug("Patient saved in Openelis with id {}", savedOpenelisPatient.getId());
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Patient", exchange, e);
        }
    }
}
