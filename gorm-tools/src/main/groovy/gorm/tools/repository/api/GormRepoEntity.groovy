/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.repository.api

import groovy.transform.CompileStatic

import gorm.tools.mango.api.QueryMangoEntity
import gorm.tools.model.Persistable
import gorm.tools.repository.GormRepo

/**
 * Trait where second generic is the repository spring bean and will create a typed getRepo
 */
@CompileStatic
trait GormRepoEntity<D, R extends GormRepo> implements BaseRepoEntity<D>, QueryMangoEntity<D>, Persistable<Long> { //PersistableTrait<Long> {

    static R getRepo() {
        return findRepo() as R
    }
}
