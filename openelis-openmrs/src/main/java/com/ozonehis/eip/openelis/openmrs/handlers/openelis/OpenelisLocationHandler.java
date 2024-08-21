/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisLocationHandler {

    public Location sendLocation(ProducerTemplate producerTemplate, Location location) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_LOCATION_ID, location.getIdPart());

        return producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-resource-route", location, headers, Location.class);
    }

    public Location buildLocation(Reference locationReference) {
        Location location = new Location();
        location.setId(locationReference.getReference().split("/")[1]);
        location.setName(locationReference.getDisplay());
        location.setStatus(Location.LocationStatus.ACTIVE);

        return location;
    }
}
