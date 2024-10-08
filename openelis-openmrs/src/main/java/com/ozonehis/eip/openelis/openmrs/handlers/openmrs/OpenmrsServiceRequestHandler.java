/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsServiceRequestHandler {

    @Autowired
    @Qualifier("openmrsFhirClient") private IGenericClient openmrsFhirClient;

    public ServiceRequest getServiceRequestByID(String serviceRequestID) {
        ServiceRequest serviceRequest = openmrsFhirClient
                .read()
                .resource(ServiceRequest.class)
                .withId(serviceRequestID)
                .execute();

        // TODO: Check if ServiceRequest is cancelled or deleted
        log.debug("ServiceRequestHandler: ServiceRequest getServiceRequestByID {}", serviceRequest.getId());

        return serviceRequest;
    }
}
