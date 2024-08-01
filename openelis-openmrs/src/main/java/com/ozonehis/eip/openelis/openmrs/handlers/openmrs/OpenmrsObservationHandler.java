/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsObservationHandler {

    public Observation sendObservation(ProducerTemplate producerTemplate, Observation observation) {
        String response =
                producerTemplate.requestBody("direct:openmrs-create-observation-route", observation, String.class);
        FhirContext ctx = FhirContext.forR4();
        Observation savedObservation = ctx.newJsonParser().parseResource(Observation.class, response);
        return savedObservation;
    }
}
