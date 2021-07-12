/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import java.time.LocalDate

import gorm.tools.testing.hibernate.GormToolsHibernateSpec
import testing.Address
import testing.Nested
import testing.Cust
import testing.TestIdent
import testing.TestSeedData

class MangoCriteriaSpec extends GormToolsHibernateSpec {

    MangoBuilder mangoBuilder

    List<Class> getDomainClasses() { [Cust, Address, Nested] }

    MangoDetachedCriteria build(map, Closure closure = null) {
        //DetachedCriteria detachedCriteria = new DetachedCriteria(Org)
        return mangoBuilder.build(Cust, map, closure)
    }

    void setupSpec() {
        //super.setupSpec()
        Cust.withTransaction {
            TestSeedData.buildCustomers(10)
        }
    }

    //@spock.lang.Ignore
    def "test field that does not exist"() {
        when:
        List res = build([nonExistingFooBar: true]).list()

        then: "Its ignores the bad field and move on"
        // FIXME, this should error I think
        res.size() > 1
    }

    def "test detached isActive"() {
        when:
        List res = build([inactive: true]).list()

        then:
        res.size() == 5
    }

    def "test detached string"() {
        when:
        List res = build([name: "Name1"]).list()

        then:
        res.size() == 1
    }

    def "test enum kind"() {
        when:
        List res = build([kind: "CLIENT"]).list()

        then:
        res.size() == 5

        when:
        res = build([kind: ['CLIENT', 'COMPANY']]).list()

        then:
        res.size() == 10
    }

    def "test enum testIdent"() {
        when:
        List res = build(['testIdent': 'Num2']).list()

        then:
        res.size() == 5

    }

    def "test enum testIdent with id"() {
        when: 'sanity check'
        List res = build(['testIdent': TestIdent.Num2]).list()

        then:
        res.size() == 5

        when:
        res = build(['testIdent': 2]).list()

        then:
        res.size() == 5

        when:
        res = build(['testIdent': [2,4]]).list()

        then:
        res.size() == 10

    }

    def "test enum testIdent with Map"() {
        when: 'sanity check'
        List res = build(['testIdent': TestIdent.Num2]).list()

        then:
        res.size() == 5

        when:
        res = build(['testIdent': [id:2]]).list()

        then:
        res.size() == 5

        when:
        res = build(['testIdent': [[id:2]]]).list()

        then:
        res.size() == 5

    }

    def "test closure"() {
        when:
        List res = build([:],{
            location{
                eq 'address', 'City1'
            }
        }).list()

        then:
        res.size() == 1
    }

    def "test detached like"() {
        when:

        List res = build([name: "Name%"]).list()

        then:
        res.size() == 10
    }

    def "test combined"() {
        when:

        List res = build(([amount: [1 * 1.34, 2 * 1.34, 3 * 1.34, 4 * 1.34], inactive: true])).list()

        then:
        res.size() == 2
    }

    def "test detached BigDecimal"() {
        when:


        List res = build(([amount: 1.34])).list()

        then:
        res.size() == 1

        when:
        res = build(([amount: ['$gt': 6.0]])).list()

        then:
        res.size() == 5
    }

    def "test detached Date"() {
        when:


        List res = build(([date: new Date().clearTime() + 2])).list()

        then:
        res.size() == 1

        when:
        res = build(([date: ['$gt': new Date().clearTime() + 7]])).list()

        then:
        res.size() == 3
    }

    def "test LocalDate"() {
        when:

        List res = build(([locDate: LocalDate.now().plusDays(2)])).list()

        then:
        res.size() == 1

        when:
        res = build(([locDate: ['$gt': LocalDate.now().plusDays(7)]])).list()

        then:
        res.size() == 3
    }

    def "test LocalDate from string"() {
        when:

        List res = build(([locDate: LocalDate.now().plusDays(2).toString() ])).list()

        then:
        res.size() == 1

        when:
        res = build(([locDate: ['$gt': LocalDate.now().plusDays(7).toString() ]])).list()

        then:
        res.size() == 3
    }

    def "test LocalDateTime"() {
        when:

        List res = build(([locDateTime: LocalDate.now().plusDays(2).atStartOfDay()])).list()

        then:
        res.size() == 1

        when:
        res = build(([locDateTime: ['$gt': LocalDate.now().plusDays(7).atStartOfDay()]])).list()

        then:
        res.size() == 3
    }

    def "test LocalDateTime from string"() {
        when:

        List res = build(([locDateTime: LocalDate.now().plusDays(2).atStartOfDay().toString() ])).list()

        then:
        res.size() == 1

        when:
        res = build(([locDateTime: ['$gt': LocalDate.now().plusDays(7).atStartOfDay().toString() ]])).list()

        then:
        res.size() == 3
    }

    def "test gt"() {
        when:


        List res = build(([id: ['$gt': 4]])).list()

        then:
        res.size() == 6
    }

    def "test ne"() {
        when:


        List res = build(([id: ['$ne': 4]])).list()

        then:
        res.size() == 9
    }

    def "test nested obj"() {
        // when:
        // def loc = Location.get(6)
        // List res = build(locationId: loc.id).list()
        //
        // then:
        // res.size() == 1

        when:
        def loc = Address.get(6)
        def res = build(location: loc).list()

        then:
        res.size() == 1

    }

    def "test nested"() {
        when:

        List res = build("location.id": ['$eq': 6]).list()

        then:
        res.size() == 1
    }

    def "test nested String"() {
        when:

        List res = build((["location.address": "City4"])).list()

        then:
        res.size() == 1
    }

    def "test nested location city"() {
        when:

        List res = build(([
            location: [
                '$or': [
                    address: "City#4",
                    id  : 4
                ]
            ]
        ])).list()

        then:
        res.size() == 1
    }

    def "test nestedId"() {
        when:

        List res = build(locationId: 6 ).list()

        then:
        res.size() == 1
    }

    def "test nestedId when vale is String"() {
        when:

        List res = build(locationId: "6" ).list()

        then:
        res.size() == 1
    }

    def "test invalid field"() {
        when:

        List res = build((["xxx": ['$eq': 6]])).list()

        then:
        noExceptionThrown()
    }

    def "test or"() {
        when:

        List res = build([
            '$or': [
                [name: "Name7"],
                [id: 2]
            ]
        ]).list()

        then:
        res.size() == 2
    }

    def "test not in list"() {
        when:


        List res = build(([amount: ['$nin': [1 * 1.34, 2 * 1.34, 3 * 1.34, 4 * 1.34]]])).list()

        then:
        res.size() == 6
    }

    def "test in list"() {
        when:


        List res = build(([id: [1, 2, 3, 4]])).list()

        then:
        res.size() == 4
    }

    def "test not"() {
        when:


        List res = build((['$not': [[id: ['$eq': 1]]]])).list()

        then:
        res.size() == 9
    }


    def "test between"() {
        when:


        List res = build(([amount: ['$between': [1 * 1.34, 4 * 1.34]]])).list()

        then:
        res.size() == 4
    }

    def "test isNull/ isNotNull"() {
        when:


        List res = build(([name2: ['$isNull': true]])).list()

        then:
        res.size() == 5

        when:

        res = build(([name2: '$isNull'])).list()

        then:
        res.size() == 5
    }

    def "test fields comparison"() {
        when:

        List res = build(([amount: ['$gtef': "amount2"]])).list()

        then:
        res.size() == 5

        when:
        res = build(([amount: ['$gtf': "amount2"]])).list()

        then:
        res.size() == 4

        when:
        res = build(([amount: ['$ltf': "amount2"]])).list()

        then:
        res.size() == 5

        when:
        res = build(([amount: ['$eqf': "amount2"]])).list()

        then:
        res.size() == 1

        when:
        res = build(([amount: ['$nef': "amount2"]])).list()

        then:
        res.size() == 9
    }

    def "test qSearch"() {
        when:

        List res = build((['$qSearch': "Na"])).list()

        then:
        res.size() == 10

        when:

        res = build((['$q': "Name3"])).list()

        then:
        res.size() == 1

        when: "quick search is combined with another field"
        res = build((['$qSearch': "Nam%", inactive: true])).list()

        then:
        res.size() == 5

        when: 'quickserach has fields under $q'
        res = build((['$q': ['text': "Nam", 'fields': ['name']], inactive: true])).list()

        then:
        res.size() == 5

        when: 'quickserach has fields under $qSearch'
        res = build((['$qSearch': ['text': "Nam", 'fields': ['name']], inactive: true])).list()

        then:
        res.size() == 5

    }

    def "test with closure"() {
        when:


        List res = build([name: "Name%"]) { gt "id", 5 }.list()

        then:
        res.size() == 5
    }


    def "test with deep nested"() {
        when:
        List res = build((["location.nested.name": "Nested4"])).list()

        then:
        res.size() == 1
    }

    def "test with `or` on one level"() {
        when:
        List res = build((['$or': [["location.id": 5], ["name": "Name1", "location.id": 4]]])).list()

        then:
        res.size() == 1
    }

    def "test order with closure"() {
        when:
        List res = build([:],{
            // location {
            //     order("address", "desc")
            // }
            order("location.address", "desc")
        }).list()

        then:
        res[0].location.address > res[1].location.address
    }

    def "test order simple"() {
        when:
        List res = build(['$sort': 'descId']).list()

        then: 'sanity check first few'
        res[0].descId < res[1].descId
        res[1].descId < res[2].descId
    }

    def "test order desc"() {
        when:
        List res = build('$sort': [id: "desc"]).list()

        then:
        res[0].id > res[1].id
        res[1].id > res[2].id
    }

    def "test order association"() {
        when:
        List res = build('$sort':['location.address': 'desc'] ).list()

        then:
        res[0].location.address > res[1].location.address
        // res[1].id > res[2].id
    }

    def "test order multi"() {
        when:
        List res = build('$sort': [inactive: "desc", id: "desc"]).list()

        then:
        // inactive is split 50/50 true false in test data, first 5 should be true
        res[0].inactive == res[4].inactive
        res[0].id > res[1].id
        res[4].inactive > res[5].inactive
        res[5].id > res[6].id
    }

    def "test multisort string"() {
        when:
        List res = build('$sort': 'inactive desc, id desc').list()
        then:
        res[0].inactive == res[4].inactive
        res[0].id > res[1].id
        res[4].inactive > res[5].inactive
        res[5].id > res[6].id
    }


    def "test projections"() {
        when:
        List res = build($projections:[[$sum:'amount']]).list()

        then:
        res == [60.30]

        when:
        res = build($projections:[[$sum:'amount'], [$sum:'id']]).list()

        then:
        res == [[60.30, 55]]

        when:
        res = build([inactive: false, $projections:[[$sum:'amount']]]).list()

        then:
        res == [26.80]

        when:
        res = build([$projections:[[$sum:'location.nested.value']]]).list()

        then:
        res == [[55.00]]

        when:
        res = build([inactive: false, $projections:[[$sum:'location.nested.value']]]).list()

        then:
        res == [[55.00]]
    }
}
