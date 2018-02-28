/* Copyright 2018. 9ci Inc. Licensed under the Apache License, Version 2.0 */
package gorm.tools.testing.integration

import gorm.tools.DbDialectService
import grails.build.support.MetaClassRegistryCleaner
import grails.buildtestdata.TestDataBuilder
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.orm.hibernate.HibernateDatastore
import org.junit.After
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Contains helpers for integration tests. Can be chained with some custom helper traits with the application-specific
 * initialization logic.
 */
trait DataIntegrationTest implements TestDataBuilder {

    JdbcTemplate jdbcTemplate
    DbDialectService dbDialectService
    HibernateDatastore hibernateDatastore

    /**
     * A metaclass registry cleaner to track and clean all changes, that were made to the metaclass during the test.
     * It is automatically cleaned up after each test case.
     */
    private MetaClassRegistryCleaner registryCleaner

    @After
    void cleanupRegistry() {
        //clear meta class changes after each test, if they were tracked and are not already cleared.
        if(registryCleaner) clearMetaClassChanges()
    }

    /** consistency with other areas of grails and other unit tests */
    AbstractDatastore getDatastore() {
        hibernateDatastore
    }

    Session getCurrentSession(){
        getDatastore().currentSession
    }

    void flushAndClear(){
        getDatastore().currentSession.flush()
        getDatastore().currentSession.clear()
    }

    /**
     * Flushes the current datastore session.
     */
    void flush() {
        getDatastore().currentSession.flush()
    }

    /**
     * Start tracking all metaclass changes made after this call, so it can all be undone later.
     */
    void trackMetaClassChanges() {
        registryCleaner = MetaClassRegistryCleaner.createAndRegister()
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener(registryCleaner)
    }

    /**
     * Reverts all metaclass changes done since last call to trackMetaClassChanges()
     */
    void clearMetaClassChanges() {
        if(registryCleaner) {
            registryCleaner.clean()
            GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(registryCleaner)
            registryCleaner = null
        }
    }
}
