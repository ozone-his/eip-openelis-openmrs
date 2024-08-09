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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisPractitionerHandler {

    public Practitioner sendPractitioner(ProducerTemplate producerTemplate, Practitioner practitioner) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PRACTITIONER_ID, practitioner.getIdPart());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-resource-route", practitioner, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Practitioner savedPractitioner = ctx.newJsonParser().parseResource(Practitioner.class, response);
        return savedPractitioner;
    }

    public Practitioner buildPractitioner(ServiceRequest serviceRequest) {
        String[] nameSplit = serviceRequest.getRequester().getDisplay().split(" ");

        Practitioner practitioner = new Practitioner();
        practitioner.setActive(true);
        practitioner.setId(serviceRequest.getRequester().getReference().split("/")[1]);
        practitioner.setName(Collections.singletonList(new HumanName()
                .setFamily(nameSplit[1])
                .setGiven(Collections.singletonList(new StringType(nameSplit[0])))));

        return practitioner;
    }
}
