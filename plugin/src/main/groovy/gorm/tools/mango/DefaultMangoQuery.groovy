/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import groovy.transform.CompileStatic

import org.springframework.beans.factory.annotation.Value

import gorm.tools.Pager
import gorm.tools.mango.api.MangoQuery
import grails.converters.JSON
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional

/**
 * Default implementation of MangoQuery. Setup as spring bean that is used by all the repos
 *
 * @author Joshua Burnett (@basejump)
 * @since 6.1
 */
@CompileStatic
class DefaultMangoQuery implements MangoQuery {

    @Value('${gorm.tools.mango.criteriaKeyName:criteria}')
    //gets criteria keyword from config, if there is no, then uses 'criteria'
    String criteriaKeyName

    /**
     * Builds detached criteria for repository's domain based on mango criteria language and additional criteria
     *
     * @param params mango language criteria map
     * @param closure additional restriction for criteria
     * @return Detached criteria build based on mango language params and criteria closure
     */
    public <D> DetachedCriteria<D> buildCriteria(Class<D> domainClass, Map criteria = [:], Closure closure = null) {
        MangoBuilder.build(domainClass, criteria, closure)
    }

    /**
     * List of entities restricted by mango map and criteria closure
     *
     * @param params mango language criteria map
     * @param closure additional restriction for criteria
     * @return query of entities restricted by mango params
     */
    public <D> List<D> query(Class<D> domainClass, Map params = [:], Closure closure = null) {
        Map<String, Map> p = parseParams(params)
        DetachedCriteria<D> dcrit = buildCriteria(domainClass, p['criteria'], closure)
        query(dcrit, p['pager'])
    }

    /**
     * List of entities restricted by mango map and criteria closure
     *
     * @param params mango language criteria map
     * @param closure additional restriction for criteria
     * @return query of entities restricted by mango params
     */
    @Transactional(readOnly = true)
    public <D> List<D> query(DetachedCriteria<D> criteria, Map pagerParams = [:], Closure closure = null) {
        Pager pager = new Pager(pagerParams)
        criteria.list(max: pager.max, offset: pager.offset)
    }

    /**
     *  Calculates sums for specified properties in enities query restricted by mango criteria
     *
     * @param params mango language criteria map
     * @param sums query of properties names that sums should be calculated for
     * @param closure additional restriction for criteria
     * @return map where keys are names of fields and value - sum for restricted entities
     */
    @Transactional(readOnly = true)
    Map countTotals(Class domainClass, Map params = [:], List<String> sums, Closure closure = null) {

        DetachedCriteria mangoCriteria = buildCriteria(domainClass, params, closure)

        List totalList
        totalList = mangoCriteria.list {
            projections {
                for (String sumField : sums) {
                    sum(sumField)
                }
            }
        }

        List totalsData = (List) totalList[0]
        Map result = [:]
        sums.eachWithIndex { String name, Integer i ->
            result[name] = totalsData[i]
        }
        return result
    }

    /**
     * returns a Map with a criteria key and a pager key containing maps for those.
     */
    Map<String, Map> parseParams(Map<String, ?> params){
        def result = [criteria: [:], pager: [:]] as Map<String, Map>
        Map paramCopy = [:]
        paramCopy.putAll(params)

        if(params[criteriaKeyName]) {
            if (params[criteriaKeyName] instanceof String) {
                JSON.use('deep')
                result['criteria'] = JSON.parse(params[criteriaKeyName] as String) as Map
            } else {
                result['criteria'] = params[criteriaKeyName] as Map
            }
            paramCopy.remove(criteriaKeyName)
            result['pager'] = paramCopy
        }
        else {
            //doesn't have a criteria. pull out the max, page and offset and assume the rest is

            Map pager = [:]
            ['max', 'offset', 'page'].each{ String k ->
                if(paramCopy[k]) pager[k] = paramCopy.remove(k)
            }
            result['criteria'] = paramCopy
            result['pager'] = pager
        }
        if (paramCopy.containsKey('sort')) {
            Object sort = paramCopy.remove('sort')
            if(sort) result['criteria']['$sort'] = sort // for the case when we have sort property but it is empty
        }
        return result
    }
}
