/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisObservationHandler {

    public Observation getObservationByObservationID(ProducerTemplate producerTemplate, String observationID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_OBSERVATION_ID, observationID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-get-observation-route", null, headers, String.class);
        log.info("getObservationByObservationID: response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Observation fetchedObservation = ctx.newJsonParser().parseResource(Observation.class, response);
        return fetchedObservation;
    }
}
