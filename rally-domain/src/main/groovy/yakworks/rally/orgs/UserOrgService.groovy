/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.rally.orgs

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import gorm.tools.security.domain.AppUser
import gorm.tools.security.services.SecService
import yakworks.rally.orgs.model.Contact
import yakworks.rally.orgs.model.Org

@Service @Lazy
@Slf4j
@CompileStatic
class UserOrgService {

    @Autowired SecService<AppUser> secService

    /**
     * gets the org from contact for the currently logged in user
     * @return
     */
    Org getUserOrg(){
        getUserOrg(secService.getUser())
    }

    /**
     * gets the org from contact for the passed in AppUser
     * @return
     */
    Org getUserOrg(AppUser appUser){
        Contact contact = Contact.findByUser(appUser)
        contact.org
    }
}
