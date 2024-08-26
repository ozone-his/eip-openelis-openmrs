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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisServiceRequestHandler {

    @Autowired
    @Qualifier("openelisFhirClient") private IGenericClient openelisFhirClient;

    public ServiceRequest sendServiceRequest(ProducerTemplate producerTemplate, ServiceRequest serviceRequest) {
        MethodOutcome methodOutcome = openelisFhirClient
                .update()
                .resource(serviceRequest)
                .prettyPrint()
                .encodedJson()
                .execute();

        log.debug("OpenelisServiceRequestHandler: ServiceRequest created {}", methodOutcome.getCreated());

        return (ServiceRequest) methodOutcome.getResource();
    }

    public void deleteServiceRequest(ProducerTemplate producerTemplate, String serviceRequestID) {
        MethodOutcome methodOutcome = openelisFhirClient
                .delete()
                .resourceById(new IdType("ServiceRequest", serviceRequestID))
                .execute();

        log.debug("OpenelisServiceRequestHandler: deleteServiceRequest {}", methodOutcome.getCreated());
    }
}
