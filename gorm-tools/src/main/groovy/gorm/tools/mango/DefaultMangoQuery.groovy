/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import gorm.tools.beans.Pager
import gorm.tools.mango.api.MangoQuery
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional

import static gorm.tools.mango.MangoOps.CRITERIA

/**
 * Default implementation of MangoQuery. Setup as spring bean that is used by all the repos
 *
 * @author Joshua Burnett (@basejump)
 * @since 6.1
 */
@CompileStatic
class DefaultMangoQuery implements MangoQuery {

    @Autowired
    MangoBuilder mangoBuilder

    @Value('${gorm.tools.mango.criteriaKeyName:criteria}')
    //gets criteria keyword from config, if there is no, then uses 'criteria'
    String criteriaKeyName

    JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.LAX)

    /**
     * Builds detached criteria for repository's domain based on mango criteria language and additional criteria
     *
     * @param params mango language criteria map
     * @param closure additional restriction for criteria
     * @return Detached criteria build based on mango language params and criteria closure
     */
    public <D> MangoDetachedCriteria<D> query(Class<D> domainClass, Map criteria = [:],
                                              @DelegatesTo(MangoDetachedCriteria) Closure closure = null) {
        mangoBuilder.build(domainClass, criteria, closure)
    }

    /**
     * List of entities restricted by mango map and criteria closure
     *
     * @param params mango language criteria map
     * @param closure additional restriction for criteria
     * @return query of entities restricted by mango params
     */
    public <D> List<D> queryList(Class<D> domainClass, Map params = [:], @DelegatesTo(MangoDetachedCriteria) Closure closure = null) {
        Map<String, Object> p = parseParams(params)
        DetachedCriteria<D> dcrit = query(domainClass, p.criteria as Map, closure)
        list(dcrit, p.pager as Pager)
    }

    /**
     * call list on the criteria with the pager params inside a readOnly transaction
     *
     * @param criteria the built detached criteria
     * @param pagerParams the map with max, offset and page
     * @return list of entities
     */
    @Transactional(readOnly = true)
    public <D> List<D> list(DetachedCriteria<D> criteria, Pager pager) {
        criteria.list(max: pager.max, offset: pager.offset)
    }

    /**
     * returns a Map with a criteria key and a pager key containing maps for those.
     */
    Map<String, Object> parseParams(Map<String, ?> params){
        def pager
        //def result = [criteria: [:], pager: [:]] as Map<String, Map>

        Map criteria = [:]
        criteria.putAll(params) //copies params into criteria

        //pull out the max, page and offset and assume the rest is criteria,
        // these should not be here if pager is already set but do anyway to make sure they are removed
        Map pagerMap = [:]
        ['max', 'offset', 'page'].each{ String k ->
            if(criteria.containsKey(k)) pagerMap[k] = criteria.remove(k)
        }
        //see if it already has a pager
        pager = criteria.remove('pager')
        //if not then assign pagerMap which will either have the params or be empty
        if(!pager) pager = pagerMap

        Pager pagerObj = (pager instanceof Pager ) ? pager as Pager : new Pager(pager as Map)

        //pull out the sort if its there
        //clean up sort
        def sort
        if (criteria['sort']) {
            sort = criteria.remove('sort')
            if(criteria['order']) {
                Map newSort = [:]
                newSort[sort] = criteria.remove('order')
                sort = newSort
            }
        }

        // check for q param or citeria, if it has it then it overrides whats in the map
        def qCriteria = criteria.q ? criteria.remove('q') : criteria.remove(CRITERIA)
        String qString

        if(qCriteria && qCriteria instanceof String) {
            qString = qCriteria as String
            //if it start with { then assume its json and parse it
            if (qString.trim().startsWith('{')) {
                // parseText returns LazyValueMap which will throw `Not that kind of map` when trying to add new key
                criteria = new HashMap(jsonSlurper.parseText(qString) as Map)
                // JSON.use('deep')
                // result['criteria'] = JSON.parse(params[criteriaKeyName] as String) as Map
            } else if (params.containsKey('qSearchFields')) {
                //if it has a qsFields then set up the map
                Map qMap = ['text': qString, 'fields': criteria.remove('qSearchFields')]
                criteria['$q'] = qMap
            } else {
                criteria['$q'] = qString
            }
        }
        else if(qCriteria && qCriteria instanceof Map){
            criteria = qCriteria as Map
        }
        // if sort is populated
        if(sort) criteria['$sort'] = sort

        return [criteria: criteria, pager: pagerObj]
    }


}
