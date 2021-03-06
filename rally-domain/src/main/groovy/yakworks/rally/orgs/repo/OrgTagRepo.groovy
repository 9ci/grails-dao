/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.rally.orgs.repo

import groovy.transform.CompileStatic

import gorm.tools.mango.MangoDetachedCriteria
import gorm.tools.model.Persistable
import gorm.tools.repository.GormRepository
import yakworks.rally.common.LinkedEntityRepoTrait
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.OrgTag
import yakworks.rally.tag.model.Tag

@GormRepository
@CompileStatic
class OrgTagRepo implements LinkedEntityRepoTrait<OrgTag, Tag> {

    @Override
    String getItemPropName() {'tag'}

    @Override
    Tag loadItem(Long id) { Tag.load(id)}

    // @RepoListener
    // void beforeValidate(OrgTag orgTag) {
    //
    // }

    List<Tag> listTags(Persistable entity) {
        list(entity)*.tag
    }

    @Override
    void validateCreate(Persistable entity, Tag tag) {
        Org org = (Org) entity
        if (tag.entityName != org.type.name())
            throw new IllegalArgumentException("Tags entityName: ${tag.entityName} not valid for org type ${org.type.name()}")
    }

    /**
     * Copies all tags from given org to target org
     */
    void copyToOrg(Org fromOrg, Org toOrg) {
        List<Long> tags = listItemIds(fromOrg)
        if (tags) add(toOrg, tags)
    }

    @Override
    MangoDetachedCriteria<OrgTag> queryFor(Persistable entity){
        query(linkedId: entity.id)
    }
}
