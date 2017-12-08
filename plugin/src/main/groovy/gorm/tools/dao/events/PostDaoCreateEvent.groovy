package gorm.tools.dao.events

import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.EventType

/**
 * Created by sudhir on 06/12/17.
 */
class PostDaoCreateEvent extends AbstractPersistenceEvent {

    Map params

    PostDaoCreateEvent(Datastore source, Object entity, Map params) {
        super(source, entity)
        this.params = params
    }

    @Override
    EventType getEventType() {
        return EventType.Validation
    }
}
