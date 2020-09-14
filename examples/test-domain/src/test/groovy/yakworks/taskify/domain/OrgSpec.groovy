/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.taskify.domain

import gorm.tools.repository.errors.EntityValidationException
import gorm.tools.security.testing.SecurityTest
import gorm.tools.testing.unit.DomainRepoTest
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Specification

class OrgSpec extends Specification implements DomainRepoTest<Org>, SecurityTest {

    void "CRUD tests"() {
        expect:
        createEntity().id
        persistEntity().id
        updateEntity().version > 0
        removeEntity()
    }

    def "test Org create fail"(){
        when:
        Map invalidData2 = [num:'foo1', name: "foo", type: [id: 1], link: ["name": "", num:'foo2', type: [id: 1]]]
        Org.create(invalidData2)

        then:
        def ex = thrown(EntityValidationException)
        ex.errors.getErrorCount() == 2
    }

    def "test validate"(){
        when:
        def o = build()

        then: "beforeValidate was called in repo"
        o.beforeValidateCheck == "got it"
    }

}
