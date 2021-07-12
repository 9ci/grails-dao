/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package restify


import groovy.transform.CompileStatic

import gorm.tools.beans.Pager
import gorm.tools.mango.DefaultMangoQuery
import gorm.tools.rest.controller.RestRepositoryApi
import yakworks.testify.model.Project
import yakworks.testify.model.Taskify

import static org.springframework.http.HttpStatus.CREATED

@CompileStatic
class ProjectController implements RestRepositoryApi<Project> {

    @Override
    def post() {
        try {
            Map q = parseJson(request)
            String comments = q.comments ?: ""
            q.comments = "$comments - post was here"
            Project instance = getRepo().create(q)
            def entityMap = createEntityMap(instance)
            respondWithEntityMap(entityMap, [status: CREATED])
            // respond instance, [status: CREATED] //201
        } catch (RuntimeException e) {
            handleException(e)
        }
    }

}
