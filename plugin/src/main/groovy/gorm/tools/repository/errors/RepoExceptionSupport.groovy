package gorm.tools.repository.errors

import gorm.tools.repository.RepoMessage
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.validation.Errors

/**
 * Handler for exceptions thrown by the Repository
 */
@CompileStatic
class RepoExceptionSupport {

    @CompileDynamic
    RuntimeException translateException(RuntimeException ex, GormEntity entity) {
        if (ex instanceof grails.validation.ValidationException) {
            grails.validation.ValidationException ve = (grails.validation.ValidationException) ex
            return new EntityValidationException(RepoMessage.notSaved(entity, true), entity, ve.errors, ve)
        } else if (ex instanceof DataIntegrityViolationException) {
            //see http://www.baeldung.com/spring-dataIntegrityviolationexception
            String ident = RepoMessage.badge(entity.ident(), entity)
            //log.error("repository delete error on ${entity.id} of ${entity.class.name}",dae)
            Errors errors = ex.hasProperty('errors') ? ex.errors : new EmptyErrors("empty")
            return new EntityValidationException(RepoMessage.notSaved(entity), entity, errors, ex)
        } else if (ex instanceof OptimisticLockingFailureException) {
            return new OptimisticLockingFailureException(RepoMessage.optimisticLockingFailure(entity, true).defaultMessage as String)
        } else if (ex instanceof DataAccessException) {
            return new EntityValidationException(RepoMessage.notSavedDataAccess(entity, true), entity, ex)
        }
        throw ex
    }
}
