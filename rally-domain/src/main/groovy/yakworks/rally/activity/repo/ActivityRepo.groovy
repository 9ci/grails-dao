/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.rally.activity.repo

import java.time.LocalDateTime
import javax.annotation.Nullable
import javax.inject.Inject
import javax.persistence.criteria.JoinType

import groovy.transform.CompileStatic

import org.apache.commons.lang3.StringUtils

import gorm.tools.beans.Pager
import gorm.tools.model.Persistable
import gorm.tools.repository.GormRepo
import gorm.tools.repository.GormRepository
import gorm.tools.repository.errors.EntityValidationException
import gorm.tools.repository.events.AfterBindEvent
import gorm.tools.repository.events.AfterPersistEvent
import gorm.tools.repository.events.BeforeBindEvent
import gorm.tools.repository.events.BeforePersistEvent
import gorm.tools.repository.events.BeforeRemoveEvent
import gorm.tools.repository.events.RepoListener
import gorm.tools.repository.model.IdGeneratorRepo
import gorm.tools.security.services.SecService
import gorm.tools.support.Results
import gorm.tools.utils.GormUtils
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import yakworks.commons.lang.Validate
import yakworks.rally.activity.model.Activity
import yakworks.rally.activity.model.ActivityLink
import yakworks.rally.activity.model.ActivityNote
import yakworks.rally.activity.model.ActivityTag
import yakworks.rally.activity.model.Task
import yakworks.rally.activity.model.TaskStatus
import yakworks.rally.activity.model.TaskType
import yakworks.rally.attachment.model.Attachment
import yakworks.rally.attachment.model.AttachmentLink
import yakworks.rally.attachment.repo.AttachmentRepo
import yakworks.rally.orgs.model.Contact
import yakworks.rally.orgs.model.Org

import static yakworks.rally.activity.model.Activity.Kind as ActKind
import static yakworks.rally.activity.model.Activity.VisibleTo

@GormRepository
@CompileStatic
class ActivityRepo implements GormRepo<Activity>, IdGeneratorRepo {

    @Inject @Nullable
    ActivityLinkRepo activityLinkRepo

    @Inject @Nullable
    ActivityTagRepo activityTagRepo

    @Inject @Nullable
    AttachmentRepo attachmentRepo

    @Inject @Nullable
    SecService secService

    @RepoListener
    void beforeValidate(Activity activity) {
        if(activity.isNew()) {
            generateId(activity)
        }
        wireAssociations(activity)
        updateSummary(activity)
    }

    @RepoListener
    void beforeBind(Activity activity, Map data, BeforeBindEvent e) {
        fixUpTaskParams(data)
        if (e.isBindUpdate()) {
            if (activity.note && data.summary) {
                activity.note.body = data.summary
                activity.note.persist()
            }
        }
    }

    @RepoListener
    void afterBind(Activity activity, Map data, AfterBindEvent e) {
        if (e.isBindUpdate() && data.deleteAttachments) {
            attachmentRepo.handleAttachmentRemoval(activity, data.deleteAttachments as List)
        }
    }

    @RepoListener
    void beforeRemove(Activity activity, BeforeRemoveEvent e) {
        if (activity.note) {
            ActivityNote note = activity.note
            activity.note = null
            activity.persist(flush: true)
            note.delete()
        }

        AttachmentLink.repo.removeAll(activity)
        //XXX missing removal for attachments if its not linked to anything else
        ActivityTag.repo.removeAll(activity)
        ActivityLink.repo.removeAllByActivity(activity)

    }

    @RepoListener
    void beforePersist(Activity activity, BeforePersistEvent e) {
        generateId(activity)
        if(e.data) {
            Map data = e.data
            addRelatedDomainsToActivity(activity, data)
        }
        if (activity.task) {
            //setup defaults for status and kind
            if (!activity.task.status) activity.task.status = TaskStatus.OPEN
            if (!activity.task.taskType) activity.task.taskType = TaskType.TODO
        }

    }

    @RepoListener
    void afterPersist(Activity activity, AfterPersistEvent e) {
        //FIXME this is a hack so the events for links get fired after data is inserted
        // not very efficient as removes batch inserting for lots of acts so need to rethink this strategy
        flush()

        Map data = e.data
        if(data?.tags) {
            //do after persisted so tables FK has activity
            activityTagRepo.bind(activity, data.tags)
        }
        // now do the links
        if (data?.arTranId) {
            activityLinkRepo.create(data.arTranId as Long, 'ArTran', activity)
        }
    }

    void wireAssociations(Activity activity) {
        if (activity.note && !activity.note.id) activity.note.id = activity.id
        if (activity.task && !activity.task.id) activity.task.id = activity.id
    }

    void updateSummary(Activity activity) {
        //title to 255
        String title = activity.title
        if (title?.length() > 255) {
            activity.title = StringUtils.abbreviate(title, 255)
        }

        //update Summary
        if (activity.kind in [ActKind.Note, ActKind.Comment] && activity.note) {
            int endChar = activity.note.body.trim().length()
            activity.summary = (endChar > 255) ? activity.note.body.trim().substring(0, 251) + " ..." : activity.note.body.trim()
        } else if (activity.kind.isTaskKind) {
            activity.summary = activity.title
        }

    }

    AttachmentLink linkAttachment(Activity activity, Attachment attachment){
        AttachmentLink.repo.create(activity, attachment)
    }

    // This adds the realted and children entities from the params to the Activity
    // called in afterBind
    void addRelatedDomainsToActivity(Activity activity, Map data) {
        if (data.attachments) {
            List attachments = attachmentRepo.bulkCreate(data.attachments as List)
            attachments.each { Attachment attachment ->
                linkAttachment(activity, attachment)
            }
        }
        else if (data.attachment) {
            Attachment attachment = attachmentRepo.create((Map) data.attachment)
            if (attachment) {
                linkAttachment(activity, attachment)
            }
        }

        Map task = data.task as Map

        if (!data.kind && task?.dueDate) {
            activity.kind = ActKind.Todo
        }

        if (data.contact) {
            activity.addToContacts((Map) data.contact)
        }

        String summary = (data.summary as String)?.trim()
        if (!activity.note && summary?.length() > 255 ) {
            addNote(activity, summary)
        }

        if (activity.note && !activity.note.body && data.summary) {
            activity.note.body = data.summary
            //activity.note.persist() Dont save it here, it will be cascaded
        }

    }

    ActivityNote addNote(Activity act, String body, String contentType = "plain") {
        if (!act.note) {
            act.note = new ActivityNote()
        }
        act.note.body = body
        act.note.contentType = contentType
        return act.note
    }

    void completeTask(Task task, Long completedById) {
        Validate.notNull(completedById, "[completedById]")
        task.bind(status: TaskStatus.COMPLETE,
                state: TaskStatus.COMPLETE.id as Integer,
                completedDate: new Date(),
                completedBy: completedById)

    }

    /**
     * insert a single activity and note for a list of domains.
     * @param targets A list of domains which need to have the activity assigned.
     * @param entityName is the class name. Should be the same as target.getClass().getSimpleName()
     * @param org is an Org to which this target is related.  All targets must be related to the same Org.
     * @param title The title of the activity.
     */
    @Transactional
    Activity insertMassNote(List targets, String entityName, Org org, String title, String body, String source = null) {
        Activity activity = new Activity()
        activity.org = org
        addNote(activity, body)
        updateSummary(activity)

        activity.title = title
        activity.source = source
        activity.sourceEntity = entityName
        activity.persist()

        targets.each { target ->
            activityLinkRepo.create(target['id'] as Long, entityName, activity)
        }

        activity.persist()
        return activity
    }

    /**
     * Insert activities for the given list of target domains
     *
     * @param targets One of the [ArTran, Customer, CustAccount, Payment]
     * @param activityData The data for new activity. Example below.
     *        <pre>
     *        [
     *        summary: "The text for note/title/summary"
     *        task: [
     *          dueDate : "2017-04-28",
     *          priority: "10",
     *          state   : "1",
     *          taskType: [id: "1"],
     *          user    : [id: 1, contact: [name: "9ci"]]
     *        ]
     *        attachments:[
     *          name: "test.txt",
     *          tempFileName: tempFileName
     *        ]
     *        ]
     *        </pre>
     * @param source activity source - if the source is from outside
     * @param newAttachments if new attachments should be created for each target
     * @return list of activities
     */
    @Transactional
    List<Activity> insertMassActivity(List targets, Map activityData, String source = null, boolean newAttachments = false) {

        Map<Long, Activity> createdActivities = [:]
        List attachments = []
        List attachmentData = activityData?.attachments as List
        if (attachmentData) {
            attachments = attachmentRepo.bulkCreate(attachmentData)
            if (targets[0].class.simpleName == "Payment") {
                attachments.each { Attachment att ->
                    String summary = activityData?.summary
                    att.description = summary?.size() > 255 ? summary[0..254] : summary
                    att.persist()
                }
            }
        }
        List<Activity> activities = []
        targets.eachWithIndex { target, i ->
            String entityName = target.getClass().getSimpleName()
            Org org = (entityName == "ArTran" ? target['customer']['org'] : target['org']) as Org //possible candidates, ArTran,Customer,CustAccount,Payment
            Activity activity
            if (createdActivities[org.id] && entityName != "Payment") {
                activity = createdActivities[org.id]
            } else {
                List copiedAttachments = attachments
                //Here !=0 = for first payment use the original attachments and for all rest of the payments copy it.
                //so same attachments are not shared between payments.
                if (i != 0 && newAttachments) {
                    copiedAttachments = attachments.collect { attachmentRepo.copy(it as Attachment)}
                }
                activity = createActivity(activityData.summary.toString(), org, (Map) activityData.task, copiedAttachments, entityName, source)
                createdActivities[org.id as Long] = activity
            }

            Long linkedId = target['id'] as Long
            activityLinkRepo.create(linkedId, entityName, activity)

            activities.add(activity)
        }

        return activities
    }

    /**
     * Creates new activity
     *
     * @param text Text for note body/title/summary (Title and summary will be trimmed to 255 characters)
     * @param org the org for the activity
     * @param task Data for the new task
     * @param attachments list of attachments to attach to this activity
     * @param entityName linked entity name for which the activity is created (Eg. ArTran, Customer etc)
     * @param source activity source -  if this is from outside.
     * @return Activity
     */
    //FIXME this is old and should be deprected
    @Transactional
    Activity createActivity(String text, Org org, Map task, List<Attachment> attachments, String entityName, String source = null) {

        Activity activity = new Activity(
            org         : org,
            title       : text,
            source      : source,
            sourceEntity: entityName
        )
        generateId(activity)
        if (task) {
            activity.task = createActivityTask(task)
            activity.kind = activity.task.taskType.kind
        } else {
            addNote(activity, text)
            updateSummary(activity)
        }
        attachments?.each { attachment ->
            linkAttachment(activity, attachment)
        }
        activity.persist()
    }

    @Transactional
    Task createActivityTask(Map taskData) {
        TaskType taskType = TaskType.get(taskData.taskType['id'] as Long)
        Task task = new Task()
        task.bind([
            taskType: taskType,
            userId  : (taskData.user ? taskData.user['id'] : null) as Long,
            dueDate : taskData.dueDate,
            priority: taskData.priority,
            state   : taskData.state ? taskData.state : Task.State.Open,
            status  : TaskStatus.getOPEN()]
        )
        return task
    }

    /**
     * quick easy way to create a Todo activity
     */
    @Transactional
    Activity createTodo(Org org, Long userId, String title, String linkedEntity = null,
                        List<Long> linkedIds = null, LocalDateTime dueDate = LocalDateTime.now()) {

        Activity activity = create(org: org, title: title, kind : Activity.Kind.Todo)

        if(linkedIds){
            for(Long linkedId: linkedIds){
                ActivityLink activityLink = new ActivityLink(activity: activity, linkedId: linkedId, linkedEntity: linkedEntity)
                activityLink.persist()
            }
        }

        activity.task = new Task(
            taskType: TaskType.TODO,
            userId  : userId,
            dueDate : dueDate,
            status  : TaskStatus.OPEN
        )
        activity.persist()
        return activity
    }

    void fixUpTaskParams(Map params) {
        //if there is no due date then assume its not a task and remove all the task stuff if it exists
        Map taskParams = params.task as Map
        if (!taskParams || taskParams.dueDate == null) {
            //params.remove "title"
            params.remove "task"
        }
    }

    DetachedCriteria<Activity> linkedActivityCriteria(Persistable linkedEntity, Activity.Kind kind = null) {
        def actLinkExists = ActivityLink.query {
            setAlias 'actLink'
            eqProperty("activity.id", "act.id")
            eq("linkedId", linkedEntity.id)
            eq("linkedEntity", linkedEntity.class.simpleName)
        }

        return Activity.query {
            setAlias 'act'
            if(kind) {
                eq("kind", kind)
            }
            exists actLinkExists.id()
        }
    }

    @ReadOnly
    boolean hasActivityWithAttachments(Persistable entity) {
        def laQuery = linkedActivityCriteria(entity)

        def attachExists = AttachmentLink.query {
            setAlias 'attachLink'
            eqProperty("linkedId", "act.id")
            eq("linkedEntity", 'Activity')
        }

        return laQuery.exists(attachExists.id()).count()
    }

    @ReadOnly
    List<Activity> listByLinked(Long linkedId, String linkedEntity, Map params) {
        Pager pager = new Pager(params)
        def crit = getActivityByLinkedCriteria(linkedId, linkedEntity, params.custArea as boolean, )
        crit.order('createdDate', 'desc')
        List<Activity> activityList = crit.list(max: pager.max, offset: pager.offset)
        return activityList
    }

    DetachedCriteria<Activity> getActivityByLinkedCriteria(Long linkedId,  String linkedEntity, boolean custArea = false) {
        def actLinkExists = ActivityLink.query {
            setAlias 'actLink'
            eqProperty("activity.id", "act.id")
            eq("linkedId", linkedId)
            eq("linkedEntity", linkedEntity)
        }
        return Activity.query {
            setAlias 'act'
            createAlias('task', 'task')
            join('task', JoinType.LEFT)
            exists actLinkExists.id()
            or {
                isNull("task")
                le("task.state", 1)
            }
            or {
                eq("visibleTo", VisibleTo.Everyone)
                if (!custArea) {
                    ne("visibleTo", VisibleTo.Owner)
                    and {
                        eq("visibleTo", VisibleTo.Owner)
                        eq("createdBy", secService.userId)
                    }
                }
            }

        }
    }

    @Transactional
    Activity copy(Activity fromAct, Activity toAct) {
        if (fromAct == null) return null

        GormUtils.copyDomain(toAct, fromAct, [createdBy: fromAct['createdBy'], editedBy: fromAct['editedBy']])
        toAct.note = GormUtils.copyDomain(ActivityNote, fromAct.note, [activity: toAct], false)
        toAct.task = GormUtils.copyDomain(Task, fromAct.task, [activity: toAct], false)
        if(fromAct.template) toAct.template = attachmentRepo.copy(fromAct.template)
        if(!toAct.id) toAct.id = generateId()

        //actCopy.persist()

        fromAct.attachments?.each { Attachment attachment ->
            Attachment attachCopy = attachmentRepo.copy(attachment)
            if(attachCopy) {
                linkAttachment(toAct, attachCopy)
            }
        }

        fromAct.contacts?.each { Contact c ->
            toAct.addToContacts(c)
        }

        List activityLinks = ActivityLink.list(fromAct)
        activityLinks?.each { ActivityLink link ->
            activityLinkRepo.create(link.linkedId, link.linkedEntity, toAct)
        }

        toAct.persist()
        ActivityTag.repo.copyToActivity(fromAct, toAct)
        return toAct

    }

    /**
     * Copies all activities from given org to target org
     */
    @Transactional
    Results copyToOrg(Org fromOrg, Org toOrg) {
        List<Activity> activities = Activity.findAllWhere(org: fromOrg)
        List<Results> copiedActivities = [] as List<Results>
        activities.each { Activity activity ->
            try {
                Activity copy = copy(activity, new Activity(org: toOrg))
                if (copy) {
                    Map queryParams = [edDate: activity['editedDate'], crDate: activity['createdDate'], newid: copy.id]
                    Activity.executeUpdate("update Activity act set act.editedDate=:edDate, act.createdDate=:crDate where act.id=:newid ", queryParams)
                }
                copiedActivities.add(new Results(ok:true, code:"finished.ok"))

            } catch (EntityValidationException e) {
                copiedActivities.add( Results.error("failed", ["Copy attachment"], e).id(activity.id) )
            }
        }
        return new Results("finished.ok", copiedActivities)

    }

}
