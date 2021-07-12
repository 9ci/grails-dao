/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import spock.lang.Specification
import testing.Address

import static gorm.tools.mango.MangoTidyMap.tidy


class MangoTidyMapSpec extends Specification {

    void "test pathToMap"() {
        expect:
        [a: [b: [c: 1]]] == MangoTidyMap.pathToMap("a.b.c", 1, [:])
        flatten([a: [b: [c: 1]], d: 2]) == flatten(MangoTidyMap.pathToMap("a.b.c", 1, [d: 2]))
        flatten([a: 1, d: 2]) == flatten(MangoTidyMap.pathToMap("a", 1, [d: 2]))
    }

    void "test tidy method for equal"() {
        expect:
        [a: [b: [c: ['$eq': 1]]]] == tidy(["a.b.c": 1])
        [a: [b: [c: ['$eq': 1]]], d: ['$eq': 2]] == tidy(["a.b.c": 1, d: ['$eq': 2]])
        [a: ['$eq': 1], d: ['$eq': 2]] == tidy(["a": 1, d: 2])
        [a: [b: [c: ['$eq': 1]]]] == tidy([a: [b: [c: 1]]])
    }

    void "test in"() {

        when:
        def mmap = tidy([
            'foo.id'     : [1, 2, 3],
            'customer.id': ['$in': [1, 2, 3]]
        ])

        then:
        mmap == [
            'foo.id' : [
                '$in': [1, 2, 3]
            ],
            'customer.id': [
                '$in': [1, 2, 3]
            ]
        ]

        when:
        mmap = tidy([
            "customer": [["id": 1], ["id": 2], ["id": 3]]
        ])

        then:
        flatten(mmap) == flatten([
            customer: [
                id: [
                    '$in': [1, 2, 3]
                ]
            ]
        ])
    }

    void "test like"() {
        when:
        def mmap = tidy([
            'foo.name': "Name%"
        ])

        then:
        mmap == [foo: [name: ['$ilike': "Name%"]]]

        when:
        mmap = tidy(
            [foo:
                 [name: "Name%"]
            ])

        then:
        mmap == [foo: [name: ['$ilike': "Name%"]]]
    }

    void "test eq"() {

        when: "object is assigned"
        def loc = new Address()
        def mmap = tidy('location': loc)

        then:
        mmap == [location: ['$eq': loc]]

        when:
        mmap = tidy([
            'foo.name': "Name"
        ])

        then:

        mmap == [foo: [name: ['$eq': "Name"]]]

        when:
        mmap = tidy([
            foo: [name: "Name"]
        ])

        then:

        mmap == [foo: [name: ['$eq': "Name"]]]
    }

    void "test combined methods"() {
        when:
        def mmap = tidy([
            "customer": [
                "id"  : 101,
                "name": "Wal%"
            ]
        ])

        then:

        flatten(mmap) == flatten([customer: [id: ['$eq': 101], name: ['$ilike': 'Wal%']]])

    }

    void "test \$or"() {
        when:
        def mmap = tidy([
            '$or': [
                "id"  : 101,
                "name": "Wal%"
            ]
        ])

        then:

        mmap == ['$or': [[id: ['$eq': 101]], [name: ['$ilike': 'Wal%']]]]

    }

    void "test \$or with and"() {
        when:
        def mmap = tidy(['$or': [["address.id": 5], ["name": "Org#1", "address.id": 4]]])

        then:

        flatten(mmap) == flatten([
            '$or': [
                [
                    '$and': [
                        [ 'address.id': ['$eq': 5] ]
                    ]
                ],
                [
                    '$and': [
                        [name: ['$eq': "Org#1"] ],
                        ['address.id': ['$eq': 4] ]
                    ]
                ]
            ]
        ])
    }


    void "test if map has Mango method "() {
        when:
        def mmap = tidy([
            'foo.name.$eq': "Name"
        ])

        then:
        mmap == [foo: [name: ['$eq': "Name"]]]

        when:
        mmap = tidy([
            'foo.name.$like': "Name"
        ])

        then:
        mmap == [foo: [name: ['$like': "Name"]]]

        when:
        mmap = tidy([
            foo: ['name.$like': "Name"]
        ])

        then:
        mmap == [foo: [name: ['$like': "Name"]]]
    }

    void "test sort"() {
        // when:
        // def mmap = tidy(
        //     location: [
        //         '$sort':['address': "desc"]
        //     ]
        // )
        //
        // then:
        // mmap == [location: [$sort: ['address': "desc"]]]

        when:
        def mmap = tidy('$sort':['location.address': "desc"])

        then:
        mmap == ['$sort':['location.address': "desc"]]

        when:
        mmap = tidy('$sort':'location.address')

        then:
        mmap == ['$sort':'location.address']

        when:
        mmap = tidy('$sort':'name desc')

        then:
        mmap == ['$sort':[name: 'desc']]

        when:
        mmap = tidy('$sort':'name asc, foo desc')

        then:
        mmap == ['$sort':[name: 'asc', foo: 'desc']]
    }

    void 'test projections'(){
        when:
        def mmap = tidy($projections: [ [$sum: 'amount'], [$sum: 'origAmount']])
        then:
        mmap == [$projections:[[$sum:'amount'], [$sum:'origAmount']]]
    }

    Map flatten(Map m, String separator = '.') {
        m.collectEntries { k, v -> v instanceof Map ? flatten(v, separator).collectEntries { q, r -> [(k + separator + q): r] } : [(k): v] }
    }

}
