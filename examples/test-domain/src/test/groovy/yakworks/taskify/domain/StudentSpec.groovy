/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.taskify.domain

import gorm.tools.security.testing.SecurityTest
import gorm.tools.testing.unit.DomainRepoTest
import skydive.Student
import spock.lang.Specification

class StudentSpec extends Specification implements DomainRepoTest<Student>, SecurityTest {

    void "CRUD tests"() {
        expect:
        createEntity().id
        Student.list().size() == 1 //XXX Fails creates duplicate records
        persistEntity().id
        def updated = updateEntity([studentId: 'T01'])
        updated.version > 0
        Student.list().size() == 2
        removeEntity(updated.id)
    }

}
