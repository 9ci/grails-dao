package yakworks.taskify.domain

import gorm.tools.repository.RepoEntity
import gorm.tools.transform.IdEqualsHashCode
import grails.compiler.GrailsCompileStatic
import grails.persistence.Entity

@IdEqualsHashCode
@GrailsCompileStatic
@Entity @RepoEntity
class TaskFlex {
    static belongsTo = [task: Task]

    String text1
    Date date1
    BigDecimal num1

    static mapping = {
        id generator: 'foreign', params: [property: 'task']
    }

}
