/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.audit

import groovy.transform.CompileStatic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener

import gorm.tools.repository.events.BeforeValidateEvent

/**
 * listens for the BeforeValidateEvent so it can get set and nullable errors are not fired
 * we do this with the gorm-tools BeforeValidateEvent because the ValidationEvent doesn't get fired
 * during a cascade to associations in gorm-hibernate
 */
@CompileStatic
class AuditStampBeforeValidateListener {// implements ApplicationListener<BeforeValidateEvent> {
    @Autowired AuditStampSupport auditStampSupport

    @EventListener
    void beforeValidate(BeforeValidateEvent event) {
        // println "AuditStampEventListener beforeValidate"
        if(isAuditStamped(event.entity))
            auditStampSupport.stampIfNew(event.entity)
    }

    // @Override
    // void onApplicationEvent(BeforeValidateEvent event) {
    //     // println "AuditStampEventListener beforeValidate"
    //     if(isAuditStamped(event.entity))
    //         auditStampSupport.stampIfNew(event.entity)
    // }

    //check if the given domain class should be audit stamped.
    boolean isAuditStamped(Object entity) {
        return auditStampSupport.isAuditStamped(entity.class.name)
    }

}
