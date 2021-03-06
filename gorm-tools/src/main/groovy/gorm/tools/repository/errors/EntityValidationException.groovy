/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.repository.errors

import groovy.transform.CompileStatic

import org.grails.datastore.mapping.validation.ValidationException
import org.springframework.validation.Errors

import gorm.tools.support.MsgKey
import gorm.tools.support.MsgSourceResolvable

/**
 * an extension of the default ValidationException so you can pass the entity and the message source
 *
 * @author Joshua Burnett (@basejump)
 * @since 6.1
 */
@CompileStatic
class EntityValidationException extends ValidationException implements MsgSourceResolvable {
    private static String defMsg = "Save or Validation Error(s) occurred"
    Object entity //the entity that the error occured on
    Map meta //any meta that can be set and passed up the chain for an error
    // Map messageMap //map with message info code,args and defaultMessage
    // MsgSourceResolvable messageKey

    EntityValidationException(String msg, Throwable cause = null) {
        this(msg, new EmptyErrors("empty"), cause)
    }

    EntityValidationException(String msg, Errors e) {
        this(msg, e, null)
    }

    EntityValidationException(String msg, Errors e, Throwable cause) {
        this(new MsgKey([], null, msg), null, e, cause)
    }

    EntityValidationException(String code, List arguments, Throwable cause = null) {
        this(new MsgKey(code, arguments, defMsg), null, null, null)
    }

    EntityValidationException(String code, Object entity) {
        this(new MsgKey(code, [entity.class.simpleName], defMsg), entity, null, null)
    }

    EntityValidationException(MsgSourceResolvable msgKey, Throwable cause = null) {
        this(msgKey, null, null, cause)
    }

    EntityValidationException(MsgSourceResolvable msgKey, Object entity, Errors errors = null, Throwable cause = null) {
        super(msgKey.defaultMessage ?: msgKey.code, errors ?: new EmptyErrors("empty"))
        if(cause) initCause(cause)
        setMessage(msgKey.msgCodes, msgKey.args, msgKey.defaultMessage)
        this.entity = entity
    }
}
