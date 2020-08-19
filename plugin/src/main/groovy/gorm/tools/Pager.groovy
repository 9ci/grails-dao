/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import gorm.tools.beans.BeanPathTools
import gorm.tools.beans.EntityMapFactory

/**
 * a holder object for paged data
 */
@Slf4j
@CompileStatic
@SuppressWarnings('ConfusingMethodName') //for max and page
class Pager {

    /**
     * The page we are on
     */
    Integer page = 1

    /**
     * Max rows to show
     */
    Integer max = 10

    /**
     * the max rows the user can set it to
     */
    Integer allowedMax = 10000

    /**
     * The total record count. This is used to calculate the number of pages
     */
    Integer recordCount = 0

    /**
     * Offset max * (page - 1)
     */
    Integer offset

    /**
     * List of elements
     */
    List data

    /**
     * Parameters
     */
    Map params

    /**
     * Constructor without params
     */
    Pager() {}

    /**
     * Constructor with params
     *
     * @param params
     */
    Pager(Map params) {
        setParams(params)
    }

    /**
     * Computes max value, if max not specified, then 10
     * Default max allowed value is 100
     *
     * @param p map of params that may contain max value
     * @param defaultMax default max value if max in params isnt set
     * @return max value for pagination
     */
    static Integer max(Map p, Integer defaultMax = 100) {
        Integer defmin = p.max ? toInteger(p.max) : 10
        p.max = Math.min(defmin, defaultMax)
        return p.max as Integer
    }

    /**
     * Computes page number, if not passed then 1
     *
     * @param p map of params
     * @return page number
     */
    static Integer page(Map p) {
        p.page = p.page ? toInteger(p.page) : 1
        return p.page as Integer
    }

    /**
     * Set params for Pager
     *
     * @param params map of params
     */
    void setParams(Map params) {
        page = params.page = params.page ? toInteger(params.page) : 1
        max = params.max = Math.min(params.max ? toInteger(params.max) : 10, allowedMax)
        this.params = params
    }

    /**
     *  Converts object to integer
     *
     * @param v value
     * @return value as integer
     */
    @CompileDynamic
    static Integer toInteger(Object v) {
        return v.toInteger()
    }

    /**
     * Calculates offset based on page number and max value
     *
     * @return offset value
     */
    Integer getOffset() {
        if (!offset) {
            return (max * (page - 1))
        }
        return offset
    }

    /**
     * Calculates total number of pages
     *
     * @return total number of pages
     */
    Integer getPageCount() {
        return Math.ceil((Double) (recordCount / max)).intValue()
    }

    /**
     * Calls passed closure for each page
     *
     * @param c closure that should be called for pages
     */
    void eachPage(Closure c) {
        if (pageCount < 1) return
        log.debug "eachPage total pages : pageCount"

        (1..pageCount).each { Integer pageNum ->
            page = pageNum
            offset = (max * (page - 1))
            try {
                log.debug "Executing eachPage closer with [max:$max, offset:$offset]"
                c.call(max, offset)
            } catch (e) {
                log.error "Error encountered while calling closure in eachPage [max:$max, offset:$offset]}]", e
                throw e
            }
        }

    }

    /**
     * Returns formatted page with next values
     *  page is the page we are on,
     *  total is the total number f pages based on max per page setting
     *  records is the total # of records we have
     *  rows are the data
     *
     * @return Map with pager values
     */
    @Deprecated
    Map getJsonData() {
        return [
            page   : this.page,
            total  : this.getPageCount(),
            records: this.recordCount,
            rows   : data
        ]
    }

    /**
     * Setup totalCount property for list, if it absent and fill values that are listed in fieldList
     *
     * @param dlist list of entities
     * @param includes list of fields names which values should be in the result list based on dlist
     * @return new list with values selected from dlist based on fieldLists field names
     */
    @Deprecated
    Pager setupData(List dlist, List includes = null) {
        setData(dlist)
        if (dlist?.size() > 0) {
            if (dlist.hasProperty('totalCount')) {
                setRecordCount(dlist.getProperties().totalCount as Integer)
            } else {
                log.warn("Cannot get totalCount for ${dlist.class}")
                setRecordCount(dlist.size())
            }
        }

        if (includes) {
            this.data = dlist.collect { obj ->
                return BeanPathTools.buildMapFromPaths(obj, includes, true)
            }
        }
        return this
    }

    /**
     * Setup the list as a EntityMapList passing in the includes which prepares it to be ready for
     * an export to json
     *
     * @param dlist list of entities
     * @param includes list of fields names which values should be in the result list based on dlist
     * @return new list with values selected from dlist based on fieldLists field names
     */
    Pager setupList(List dlist, List includes = null) {
        def entityMapList = EntityMapFactory.createEntityMapList(dlist, includes)
        if(entityMapList){
            setRecordCount(entityMapList.getTotalCount())
            setData(entityMapList)
        } else {
            setRecordCount(0)
            setData([])
        }
        return this
    }
}
