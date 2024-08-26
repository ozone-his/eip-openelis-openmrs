/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Collections;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisPractitionerHandler {

    @Autowired
    @Qualifier("openelisFhirClient") private IGenericClient openelisFhirClient;

    public Practitioner sendPractitioner(ProducerTemplate producerTemplate, Practitioner practitioner) {
        MethodOutcome methodOutcome = openelisFhirClient
                .update()
                .resource(practitioner)
                .prettyPrint()
                .encodedJson()
                .execute();

        log.debug("OpenelisPractitionerHandler: Practitioner created {}", methodOutcome.getCreated());

        return (Practitioner) methodOutcome.getResource();
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
