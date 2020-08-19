/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.rest.controller


import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.core.GenericTypeResolver

import gorm.tools.Pager
import gorm.tools.beans.BeanPathTools
import gorm.tools.beans.EntityMapFactory
import gorm.tools.repository.GormRepoEntity
import gorm.tools.repository.api.RepositoryApi
import grails.artefact.controller.RestResponder
import grails.artefact.controller.support.ResponseRenderer
import grails.databinding.SimpleMapDataBindingSource
import grails.util.GrailsClassUtils
import grails.util.GrailsNameUtils
import grails.web.Action
import grails.web.api.ServletAttributes
import grails.web.databinding.DataBindingUtils

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK

@CompileStatic
@SuppressWarnings(['CatchRuntimeException', 'NoDef'])
trait RestRepositoryApi<D extends GormRepoEntity> implements RestResponder, ServletAttributes, MangoControllerApi, RestControllerErrorHandling {

    Map includes = [:]
    List qSearchIncludes = []
    boolean _qSearchConfigChecked = false

    /**
     * The java class for the Gorm domain (persistence entity). will generally get set in constructor or using the generic as
     * done in {@link gorm.tools.repository.GormRepo#getEntityClass}
     * using the {@link org.springframework.core.GenericTypeResolver}
     * @see org.grails.datastore.mapping.model.PersistentEntity#getJavaClass() .
     */
    Class<D> entityClass // the domain class this is for

    /**
     * The gorm domain class. uses the {@link org.springframework.core.GenericTypeResolver} is not set during contruction
     */
    Class<D> getEntityClass() {
        if (!entityClass) this.entityClass = (Class<D>) GenericTypeResolver.resolveTypeArgument(getClass(), RestRepositoryApi)
        return entityClass
    }

    /**
     * Gets the repository for the entityClass
     * @return The repository
     */
    RepositoryApi<D> getRepo() {
        //GrailsClassUtils.getStaticPropertyValue(getEntityClass(),'repo')
        (RepositoryApi<D>) InvokerHelper.invokeStaticMethod(getEntityClass(), 'getRepo', null)
    }

    /**
     * POST /api/entity
     * Create with data
     */
    @Action
    def post() {
        try {
            D instance = (D) getRepo().create(getDataMap())
            respond jsonObject(instance), [status: CREATED] //201
        } catch (RuntimeException e) {
            handleException(e)
        }
    }

    /**
     * PUT /api/entity/${id}* Update with data
     */
    @Action
    def put() {
        Map data = [id: params.id]
        data.putAll(getDataMap()) // getDataMap doesnt contains id because it passed in params
        try {
            D instance = (D) getRepo().update(data)
            respond jsonObject(instance), [status: OK] //200
        } catch (RuntimeException e) {
            handleException(e)
        }

    }

    /**
     * DELETE /api/entity/${id}* update with params
     */
    @Action
    def delete() {
        try {
            getRepo().removeById((Serializable) params.id)
            callRender(status: NO_CONTENT) //204
        } catch (RuntimeException e) {
            handleException(e)
        }

    }

    /**
     * GET /api/entity/${id}* update with params
     */
    @Action
    def get() {
        try {
            D instance = (D) getRepo().get(params)
            def renderObj = jsonObject(instance)
            // println "renderObj $renderObj"
            respond(renderObj)
        } catch (RuntimeException e) {
            handleException(e)
        }
    }

    @Action
    def index() {
        list()
    }

    /**
     * request type is handled in urlMapping
     *
     * returns the list of domain objects
     */
    @Action
    def listPost() {
        respond query((request.JSON ?: [:]) as Map, params)
    }

    /**
     * request type is handled in urlMapping
     *
     * returns the list of domain objects
     */
    @Action
    def list() {
        Pager pager = pagedQuery(params, 'list')
        Map renderArgs = [:] //[includes: ['name']]
        respond([view: '/object/_pagedList'], [pager: pager, renderArgs: renderArgs])
        // respond query(params)
    }

    @Action
    def pickList() {
        Pager pager = pagedQuery(params, 'pickList')
        Map renderArgs = [:]
        respond([view: '/object/_pagedList'], [pager: pager, renderArgs: renderArgs])
    }

    //@CompileDynamic
    Pager pagedQuery(Map params, String includesKey) {
        Pager pager = new Pager(params)
        // println "params ${params.class} $params"
        List dlist = query(pager, params)
        List incs = getIncludes(includesKey)
        return pager.setupList(dlist, incs)
    }

    List query(Pager pager, Map p = [:]) {
        ['max', 'offset', 'page'].each{ String k ->
            p[k] = pager[k]
        }

        //def qSearch = p.remove('q')
        if(p.q && getSearchFields()) {
            Map qMap = ['text': p.q, 'fields': getSearchFields()]
            p['$q'] = qMap
        }

        getMangoApi().query(p)
    }

    /**
     * builds the response model with the EntityMap wrapper.
     *
     * @param instance the entity instance
     * @param includeKey the key to use in the includes map, use default by default
     * @return the object to pass on to json views
     */
    Object jsonObject(D instance, String includesKey = 'get'){
        List incs = getIncludes(includesKey)
        // def emap = BeanPathTools.buildMapFromPaths(instance, incs)
        def emap = EntityMapFactory.createEntityMap(instance, incs)
        return emap
    }

    @SuppressWarnings(['ReturnsNullInsteadOfEmptyCollection'])
    //@CompileDynamic
    List getIncludes(String includesKey){
        if(!includes || !includes['__configChecked__']){
            //see if there is a config for it
            Map cfgIncs = grailsApplication.config.getProperty("restApi.${getControllerName()}.includes", Map)
            if(cfgIncs) includes = cfgIncs
            if(!includes) {
                List includesDefault = GrailsClassUtils.getStaticPropertyValue(getEntityClass(), 'includes') as List
                includes = includesDefault ? ['get': includesDefault] : [:]
                // look for pickList too
                List pickListIncludes = GrailsClassUtils.getStaticPropertyValue(getEntityClass(), 'pickListIncludes') as List
                if(pickListIncludes) includes['pickList'] = pickListIncludes
            }
            includes['__configChecked__'] = true //mark it so we don't check config again each time
        }
        List incs = includes[includesKey] as List
        incs = incs ?: includes['get'] as List
        // println "incs $includesKey $incs"
        return incs
    }

    List getSearchFields(){
        if(!qSearchIncludes || !_qSearchConfigChecked){
            //see if there is a config for it
            List cfgQSearch = grailsApplication.config.getProperty("restApi.${getControllerName()}.qSearch", List)
            if(cfgQSearch) qSearchIncludes = cfgQSearch
            if(!qSearchIncludes) {
                List qSearchFieldsStatic = GrailsClassUtils.getStaticPropertyValue(getEntityClass(), 'qSearchIncludes') as List
                qSearchIncludes = qSearchFieldsStatic ?: []
            }
            _qSearchConfigChecked = true //mark it so we don't do config setup again on each request
        }
        return qSearchIncludes
    }

    /**
     * getControllerName() works inisde a request and should be used, but during init or outside a request use this
     * should give roughly what logicalName is which is used to setup the urlMappings by default
     */
    String getLogicalControllerName(){
        String logicalName = GrailsNameUtils.getLogicalName(this.class, 'Controller')
        return GrailsNameUtils.getPropertyName(logicalName)
    }

    /**
     * The Map object that can be bound to create or update domain entity.  Defaults whats in the request based on mime-type.
     * Subclasses may override this
     */
    @CompileDynamic
    //so it can access the SimpleMapDataBindingSource.map
    Map getDataMap() {
        SimpleMapDataBindingSource bsrc =
                (SimpleMapDataBindingSource) DataBindingUtils.createDataBindingSource(grailsApplication, getEntityClass(), getRequest())
        return bsrc.map
    }

    /**
     * Cast this to ResponseRenderer and call render
     * @param args
     */
    void callRender(Map args) {
        ((ResponseRenderer) this).render args
    }

    /**
     * CAst this to RestResponder and call respond
     * @param value
     * @param args
     */
    def callRespond(Object value, Map args = [:]) {
        ((RestResponder) this).respond value, args
    }

}
