/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package restify

import groovy.transform.CompileStatic

import gorm.tools.beans.Pager
import gorm.tools.rest.controller.RestRepositoryApi
import yakworks.rally.orgs.model.Location

import static org.springframework.http.HttpStatus.CREATED

@CompileStatic
class LocationController implements RestRepositoryApi<Location> {

    def post() {
        Map q = new LinkedHashMap(parseJson(request))
        q.street = q.street == null ? null : "foo street"
        Location instance = getRepo().create(q)
        respond instance, [status: CREATED] //201
    }

    //TODO: remove it added just for debugging
    def countTotals() {
        Pager pager = pagedQuery([/*id: ['$gt': 1010],*/ '$projections': [['$sum': 'contact.id']]], 'list')
        // passing renderArgs args would be usefull for 'renderNulls' if we want to include/exclude
        respond([view: '/object/_pagedList'], [pager: pager, renderArgs: [:]])
    }

}
