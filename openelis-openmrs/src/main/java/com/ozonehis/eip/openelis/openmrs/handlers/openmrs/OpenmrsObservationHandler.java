/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsObservationHandler {

    public Observation sendObservation(ProducerTemplate producerTemplate, Observation observation) {

        return producerTemplate.requestBody("direct:openmrs-create-resource-route", observation, Observation.class);
    }

    public Observation buildObservation(ServiceRequest openmrsServiceRequest, Observation openelisObservation) {
        Observation openmrsObservation = new Observation();

        openmrsObservation
                .addBasedOn()
                .setReference(openmrsServiceRequest.getIdPart())
                .setType("ServiceRequest");
        openmrsObservation.setStatus(Observation.ObservationStatus.FINAL);
        openmrsObservation.setCode(openelisObservation.getCode());
        openmrsObservation.setSubject(openmrsServiceRequest.getSubject());
        openmrsObservation.setEffective(openelisObservation.getEffective());
        openmrsObservation.setIssued(openelisObservation.getIssued());
        openmrsObservation.setValue(openelisObservation.getValue());
        openmrsObservation.setReferenceRange(openelisObservation.getReferenceRange());

        return openmrsObservation;
    }
}
