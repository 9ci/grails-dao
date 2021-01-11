package gorm.tools.audit

import gorm.tools.repository.model.RepoEntity
import grails.compiler.GrailsCompileStatic
import grails.persistence.Entity

@AuditStamp
@Entity
@GrailsCompileStatic
class StampedEntity implements RepoEntity<StampedEntity>{

    String name
    String beforeInsertTest

    static mapping = {
        cache true
        table 'stampedEntity'
    }

    static constraints = {
        name nullable: false
    }

    def beforeInsert(){
        println "in before insert"
        beforeInsertTest = "gotcha"
    }

}
