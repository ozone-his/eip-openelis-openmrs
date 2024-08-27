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
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisLocationHandler {

    @Autowired
    @Qualifier("openelisFhirClient") private IGenericClient openelisFhirClient;

    public Location sendLocation(Location location) {
        MethodOutcome methodOutcome = openelisFhirClient
                .update()
                .resource(location)
                .prettyPrint()
                .encodedJson()
                .execute();

        log.debug("OpenelisLocationHandler: Location created {}", methodOutcome.getCreated());
        return (Location) methodOutcome.getResource();
    }

    public Location buildLocation(Reference locationReference) {
        Location location = new Location();
        location.setId(locationReference.getReference().split("/")[1]);
        location.setName(locationReference.getDisplay());
        location.setStatus(Location.LocationStatus.ACTIVE);

        return location;
    }
}
