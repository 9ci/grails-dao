/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.apache.commons.lang3.EnumUtils

/**
 * Statics for the operations
 *
 * @author Joshua Burnett (@basejump)
 * @since 6.1
 */
@SuppressWarnings(['FieldName', 'ConfusingMethodName']) //codenarc doesn't like the names we use to make this builder clean
@CompileStatic
@Slf4j
class MangoOps {

    public static final String CRITERIA = 'criteria'
    public static final String SORT = '$sort'
    public static final String Q = '$q'
    public static final String QSEARCH = '$qSearch'
    public static final String PROJECTIONS = '$projections'

    @CompileStatic
    static enum CompareOp {
        $gt, $eq, $gte, $lt, $lte, $ne, $not, $ilike, $like, $in, $inList

        private final String op //private for security
        String getOp(){ return op }

        CompareOp() {
            this.op = name().substring(1) //remove $
        }
    }

    @CompileStatic
    static enum PropertyOp {
        $gtf('gtProperty'),
        $gtef('geProperty'),
        $ltf('ltProperty'),
        $ltef('leProperty'),
        $eqf('eqProperty'),
        $nef('neProperty')

        private final String op //private for security
        String getOp(){ return op }

        PropertyOp(String op) {
            this.op = op
        }
    }

    @CompileStatic
    static enum OverrideOp {
        $between('between'),
        $nin('notIn')

        private final String op
        String getOp(){ return op }

        OverrideOp(String op) {
            this.op = op
        }
    }

    @CompileStatic
    static enum JunctionOp {
        $and, $or, $not

        private final String op //private for security
        String getOp(){ return op }

        JunctionOp() {
            this.op = name().substring(1) //remove $
        }
    }

    @CompileStatic
    static enum ExistOp {
        $isNull, $isNotNull

        private final String op
        String getOp(){ return op }

        ExistOp() {
            this.op = name().substring(1) //remove $
        }
    }

    @CompileStatic
    static enum ProjectionsOp {
        $sum, $avg

        private final String op
        String getOp(){ return op }

        ProjectionsOp() {
            this.op = name().substring(1) //remove $
        }
    }

    static boolean isValidOp(String key) {
        EnumUtils.isValidEnum(ExistOp, key) ||
            EnumUtils.isValidEnum(PropertyOp, key) ||
            EnumUtils.isValidEnum(CompareOp, key)
    }
}
