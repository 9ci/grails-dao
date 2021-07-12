/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import groovy.transform.CompileDynamic

import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria

/**
 * This is here to make it easier to build criteria with domain bean paths
 * allows
 * order('invoice.customer.name')
 *
 * instead of
 * invoice {
 *    customer {
 *       order(name)
 *    }
 * }
 *
 * simliar with eq, like and in
 *
 * ilike('invoice.customer.name', 'foo')
 */
@GrailsCompileStatic
class MangoDetachedCriteria<T> extends DetachedCriteria<T> {


    /**
     * Constructs a DetachedCriteria instance target the given class and alias for the name
     * @param targetClass The target class
     * @param alias The root alias to be used in queries
     */
    MangoDetachedCriteria(Class<T> targetClass, String alias = null) {
        super(targetClass, alias)
    }

    @Override
    protected MangoDetachedCriteria newInstance() {
        new MangoDetachedCriteria(targetClass, alias)
    }

    @CompileDynamic
    @Override
    List<T> list(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        if (!this.associationCriteriaMap?.isEmpty()) {
            return this.associationCriteriaMap.collect { k, v ->
                v.list([:], additionalCriteria)
            }

        }
        super.list(args, additionalCriteria)
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param propertyName The property name to order by
     * @return A Order instance
     */
    // @Override
    // MangoDetachedCriteria<T> order(String propertyName) {
    //     return order(propertyName, "asc")
    // }

    /**
     * Orders by the specified property name and direction
     * takes invoice.customer.name and builds a closure that looks like
     *
     * invoice {
     *    customer {
     *       order(name)
     *    }
     * }
     * and then calls the that closure on this.
     *
     * @param propertyName The property name to order by
     * @param direction Either "asc" for ascending or "desc" for descending
     * @param forceSuper use original order(...) from HibernateCriteriaBuilder
     *
     * @return A Order instance
     */
    // @CompileDynamic
    // MangoDetachedCriteria<T> order(String propertyName, String direction, boolean forceSuper = false) {
    //     if (forceSuper || !propertyName.contains('.')) {
    //         return super.order(propertyName, direction)
    //     }
    //     List props = propertyName.split(/\./) as List
    //     String last = props.pop()
    //     Closure toDo = { order(last, direction) }
    //     Closure newOrderBy = props.reverse().inject(toDo) { acc, prop ->
    //         { -> "$prop"(acc) }
    //     }
    //     newOrderBy.call()
    //     return this
    // }

    @Override
    MangoDetachedCriteria<T> eq(String propertyName, Object propertyValue) {
        nestedPathPropCall(propertyName, propertyValue, "eq")
    }

    @Override
    MangoDetachedCriteria<T> inList(String propertyName, Collection values) {
        nestedPathPropCall(propertyName, values, "inList")
    }

    @CompileDynamic
    MangoDetachedCriteria<T> nestedPathPropCall(String propertyName, Object propertyValue, String critName) {
        if (!propertyName.contains('.') || propertyName.endsWith('.id')) {
            return super."$critName"(propertyName, propertyValue)
        }
        List props = propertyName.split(/\./) as List
        String last = props.removeLast()
        Closure toDo = { "$critName"(last, propertyValue) }
        Closure newCall = props.reverse().inject(toDo) { Closure acc, String prop ->
            { -> "$prop"(acc) }
        }
        newCall.call()
        return this
    }

    @CompileDynamic
    def assoc(String assoc, Closure args) {
        super.invokeMethod(assoc, args)
    }

    // /**
    //  * Dynamic method dispatch fail!
    //  */
    // Object methodMissing(String name, Object args) {
    //     return super.invokeMethod(name, args)
    // }

    @Override
    protected MangoDetachedCriteria<T> clone() {
        return (MangoDetachedCriteria)super.clone()
    }

    /**
     * Enable the builder syntax for constructing Criteria
     *
     * @param callable The callable closure
     * @return A new criteria instance
     */
    @Override
    MangoDetachedCriteria<T> build(@DelegatesTo(MangoDetachedCriteria) Closure callable) {
        (MangoDetachedCriteria<T>)super.build(callable)
    }

}

/**
 * This class exists solely to circumvent the "protected" visibility of the org.hibernate.criterion.Order class constructor.
 */
// @CompileDynamic
// class OrderCheater extends Query.Order {
//     OrderCheater(String propertyName, boolean ascending) {
//         super(propertyName, ascending)
//     }
// }
