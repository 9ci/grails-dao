/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package restify

import groovy.transform.CompileStatic

import gorm.tools.repository.DefaultGormRepo
import gorm.tools.repository.GormRepo
import grails.gorm.transactions.Transactional

@Transactional
@CompileStatic
class BookRepo implements GormRepo<Book> {

}
