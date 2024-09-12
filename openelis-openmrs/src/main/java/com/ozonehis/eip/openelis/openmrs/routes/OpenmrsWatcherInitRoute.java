/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * This route is used to initialize the OpenMRS watcher.
 * It is only used once when the application starts up.
 */
@Component
public class OpenmrsWatcherInitRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("scheduler:openmrs-watcher?initialDelay=500&repeatCount=1")
                .routeId("openmrs-watcher-init-route")
                .to("openmrs-watcher:init");
    }
}
