/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.rally.attachment.repo

import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

import gorm.tools.repository.GormRepo
import gorm.tools.repository.GormRepository
import gorm.tools.repository.events.AfterBindEvent
import gorm.tools.repository.events.BeforeBindEvent
import gorm.tools.repository.events.BeforeRemoveEvent
import gorm.tools.repository.events.RepoListener
import gorm.tools.repository.model.IdGeneratorRepo
import yakworks.commons.io.FileUtil
import yakworks.rally.attachment.AttachmentSupport
import yakworks.rally.attachment.model.Attachment

/**
 * Attachments are not as simple as they might be in this application.  Please read this documentation before messing
 * with it.
 */
@Slf4j
@GormRepository
@CompileStatic
class AttachmentRepo implements GormRepo<Attachment>, IdGeneratorRepo {
    public static final String ATTACHMENT_LOCATION_KEY = "attachments.location"

    AttachmentSupport attachmentSupport
    AttachmentLinkRepo attachmentLinkRepo

    /**
     * The event listener, which is called before binding data to an Attachment entity
     * and before persisting this entity to a database.
     *
     * @param attachment an entity on which the bind operation was performed
     * @param p params data that will be bound to the entity
     * @param ev type of the action before which bind is performed, e.g. Create or Update
     */
    @RepoListener
    void beforeBind(Attachment attachment, Map p, BeforeBindEvent ev) {
        if (ev.isBindCreate()) {
            //**setup defaults
            //id early so we have it for parent child relationships
            generateId(attachment)
            if (!p.name) p.name = p.originalFileName
            if (!p.name) {
                rejectValue(attachment, 'name', null, 'default.null.message')
                return
            }
            if (!p.originalFileName) p.originalFileName = p.name
            if (!p.mimeType) p.mimeType = FileUtil.extractMimeType(p.originalFileName as String)
            if (!p.extension) p.extension = FileUtil.getExtension(p.originalFileName as String)
            //XXX hard coded design needs to be refactored out and simplified
            if (p.isCreditFile) p.locationKey = "attachments.creditFiles.location"
        }

        //DEPRECATED logic for storing data in db
        if (ev.isBindUpdate()) {
            if (p['fileData.data'] && p['fileData.data'] instanceof String) {
                String fdata = (p['fileData.data'] as String)
                p.contentLength = fdata.getBytes().size()
                p['fileData.data'] = fdata.getBytes()
            }
        }
    }

    @RepoListener
    void afterBind(Attachment attachment, Map p, AfterBindEvent ev) {
        if (ev.isBindCreate()) {
            Path attachedFile = createFile(attachment, p)
            if(attachedFile){
                p.attachedFile = attachedFile //used later in exeption handling to delete the file
                attachment.location = attachmentSupport.getRelativePath(attachedFile, attachment.locationKey)
                attachment.contentLength = Files.size(attachedFile)
            } else if(attachment.fileData?.data) {
                attachment.contentLength = attachment.fileData.data.size()
            }
            // assert Files.exists(attachedFile)
            // assert attachment.locationKey
        }
    }

    /**
     * 4 ways a file can be set via params
     *   1. with tempFileName key, where its a name of a file that has been uploaded
     *      to the tempDir location key for appResourceLoader
     *   2. with sourcePath, this should be a absolute path object or string
     *   3. with MultipartFile
     *   4. with bytes, similiar to MultiPartFile. if this is the case then name should have the info for the file
     * @return the path object for the file to link in location
     */
    Path createFile(Attachment attachment, Map p){
        String originalFileName = p.originalFileName as String

        if (p.tempFileName) { //this would be primary way to upload files via UI and api
            return attachmentSupport.createFileFromTempFile(attachment.id, originalFileName, p.tempFileName as String, attachment.locationKey)
        }
        else if (p.sourcePath) { //used for copying attachments and testing
            return attachmentSupport.createFileFromSource(attachment.id, originalFileName, p.sourcePath as Path, attachment.locationKey)
        }
        else if (p.multipartFile) { //multipartFile from a ui
            MultipartFile multipartFile = p.multipartFile as MultipartFile
            Path tempFile = attachmentSupport.createTempFile(originalFileName, null)
            multipartFile.transferTo(tempFile) //do this instead of bytes as it is more memory efficient for big files
            return attachmentSupport.createFileFromTempFile(attachment.id, originalFileName, tempFile.fileName.toString(), attachment.locationKey)
        }
        else if (p.bytes && p.bytes instanceof byte[]) { //used mostly for testing but also for string templates
            return attachmentSupport.createFileFromBytes(attachment.id, originalFileName, p.bytes as byte[], attachment.locationKey)
        }
    }

    /**
     * wraps super.bindAndCreate in try catch so that on any exception it will delete the file reference in the data params
     */
    @Override
    Attachment doCreate(Map data, Map args) {
        Attachment attachment
        try {
            attachment = new Attachment()
            bindAndCreate(attachment, data, args)
        } catch (e) {
            // the file may have been created in afterBind so delete it if exception fires
            Path attachedFile = data['attachedFile'] as Path
            if(attachedFile) Files.deleteIfExists(attachedFile)
            throw e
        }

        attachment
    }

    @RepoListener
    void beforeRemove(Attachment attachment, BeforeRemoveEvent e) {
        if(attachment.location) {
            attachmentSupport.deleteFile(attachment.location, attachment.locationKey)
        }
    }

    /**
     * Inserts the list of files into Attachments, and returns the attachments as a list
     * @param fileDetailsList a list of maps, Each list entry (which is a map) represents a file.
     * The map has keys as follows: <br>
     *  - originalFileName: The name of the file the user sent. <br>
     *  - tempFileName: The name of the temp file the app server created to store it when uploaded. <br>
     * @return the list of attachments
     */
    // @Transactional
    // List<Attachment> insertList(List<Map> fileDetailsList) {
    //     log.debug("*******-->File details list: ${fileDetailsList}")
    //     List<Attachment> resultList = []
    //     fileDetailsList.each { Map fileDetails ->
    //         Attachment attachment = create(fileDetails)
    //         resultList.add(attachment)
    //     }
    //     resultList
    // }

    /**
     * creates from a multipart file as an attachment. Used in LogoService for example.
     *
     * @param multipartFile the MultipartFile that has the bytes and info
     * @param params any extra params for the Activity
     */
    Attachment create(MultipartFile multipartFile, Map params) {
        params['originalFileName'] = multipartFile.originalFilename
        //params['mimeType'] = multipartFile.contentType
        params['multipartFile'] = multipartFile

        create(params)
    }

    Resource getResource(Attachment attachment){
        attachmentSupport.getResource(attachment)
    }

    Path getFile(Attachment attachment){
        attachmentSupport.getFile(attachment.location, attachment.locationKey)
    }

    String getDownloadUrl(Attachment attachment) {
        attachmentSupport.getDownloadUrl(attachment)
    }

    /**
     * Create a copy of the given attachment
     *
     * @param source  attachment which should be copied
     * @return a new attachment, which is copied from the source, in case 'source' is null - returns null
     */
    // @Transactional create is already in a trx and is enough
    Attachment copy(Attachment source) {
        if(source == null) return null
        Map params = [:]
        ['name', 'description', 'mimeType', 'kind', 'subject', 'locationKey'].each {String prop ->
            params[prop] = source[prop]
        }
        if(source.location) {
            params.sourcePath = getFile(source)
        }
        else if(source.fileData?.data) {
            params.fileData = [data: source.fileData.data] as Map
        }
        Attachment copy = create(params)
        return copy
    }

}
