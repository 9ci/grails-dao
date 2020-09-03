/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.security.domain

import groovy.transform.CompileDynamic

import org.codehaus.groovy.util.HashCodeHelper

import grails.compiler.GrailsCompileStatic
import grails.persistence.Entity

import static grails.gorm.hibernate.mapping.MappingBuilder.orm

@Entity
@GrailsCompileStatic
class SecRoleUser implements Serializable {

    SecUser user
    SecRole role

    static transients = ['roleName', 'userName', 'id']

    static mapping = orm {
        id composite: ['user', 'role']
        version false
        property 'user', [column:'userId']
        property 'role', [column:'secRoleId']

    }

    String getRoleName() {
        this.role.name
    }

    String getUserName() {
        this.user.name
    }

    @CompileDynamic
    static List<SecRoleUser> getByUser(long userId) {
        SecRoleUser.createCriteria().list {
            user{
                eq("id", userId)
            }
        } as List<SecRoleUser>
    }

    static SecRoleUser get(long userId, long roleId) {
        find 'from SecRoleUser where user.id=:userId and role.id=:roleId', [userId: userId, roleId: roleId]
    }

    static SecRoleUser create(SecUser user, SecRole role, boolean flush = false) {
        new SecRoleUser(user: user, role: role).save(flush: flush, insert: true)
    }

    static boolean remove(SecUser user, SecRole role, boolean flush = false) {
        SecRoleUser instance = SecRoleUser.findByUserAndRole(user, role)
        if (!instance) {
            return false
        }

        instance.delete(flush: flush)
        true
    }

    static void removeAll(SecUser user) {
        executeUpdate 'DELETE FROM SecRoleUser WHERE user=:user', [user: user]
    }

    static void removeAll(SecRole role) {
        executeUpdate 'DELETE FROM SecRoleUser WHERE role=:role', [role: role]
    }

    @CompileDynamic
    static getRoleMap(SecUser userInstance) {
        List roles = SecRole.list()
        Set userRoleNames = []
        if (userInstance.id) {
            for (r in userInstance.roles) {
                userRoleNames << r.springSecRole
            }
        }
        Map<SecRole, Boolean> roleMap = [:]
        for (r in roles) {
            roleMap[(r)] = userRoleNames.contains(r.springSecRole)
        }

        return roleMap
    }

    @Override
    boolean equals(Object other) {
        if (other instanceof SecRoleUser) {
            other.userId == user?.id && other.roleId == role?.id
        }
    }

    @Override
    int hashCode() {
        int hashCode = HashCodeHelper.initHash()
        if (user) {
            hashCode = HashCodeHelper.updateHash(hashCode, user.id)
        }
        if (role) {
            hashCode = HashCodeHelper.updateHash(hashCode, role.id)
        }
        hashCode
    }

}
