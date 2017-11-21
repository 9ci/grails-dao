package gorm.tools

import gorm.tools.beans.DateUtil
import grails.core.GrailsDomainClassProperty
import grails.compiler.GrailsCompileStatic
import grails.databinding.converters.ValueConverter
import grails.util.Holders
import grails.web.databinding.GrailsWebDataBinder
import groovy.transform.CompileStatic
import org.apache.commons.lang.Validate
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association

/**
 * GormUtils provides a set of static helpers for working with domain classes.
 * It allows to copy domain instances, to copy separate properties of an object, etc.
 */
@GrailsCompileStatic
class GormUtils {

    /**
     * The list of domain properties which are ignored during copying.
     */
    final static List<String> IGNORED_PROPERTIES = ["id", "version", "createdBy", "createdDate", "editedBy", "editedDate", "num"]

    /**
     * Creates an instance of a given domain class and copies properties from source object.
     *
     * @param domainClass Domain class
     * @param source - domain class to copy properties from
     * @param override - properties to override after copying
     * @param ignoreAssociations - should associations be copied ? - ignored by default
     */
    static <T> T copyDomain(Class<T> domainClass, Object source, Map override = [:], boolean ignoreAssociations = true) {
        T copy = domainClass.newInstance()
        return copyDomain(copy, source, override, ignoreAssociations) as T
    }

    /**
     * Copies properties from source to target object.
     *
     * @param target domain instance to copy properties to
     * @param source - domain class to copy properties from
     * @param override - properties to override after copying
     * @param ignoreAssociations - should associations be copied ? - ignored by default
     */
    static Object copyDomain(Object target, Object source, Map override = [:], boolean ignoreAssociations = true) {
        if (target == null) throw new IllegalArgumentException("Target is null")
        if (source == null) return null

        GormMetaUtils.getDomainClass(target.class).persistentProperties.each { GrailsDomainClassProperty dp ->
            if (IGNORED_PROPERTIES.contains(dp.name) || dp.identity) return
            if (ignoreAssociations && dp.isAssociation()) return

            String name = dp.name
            target[name] = source[name]
        }

        if (override) {
            target.properties = override
        }

        return target
    }

    /**
     * Faster, simplier binder. Copies properties from source to target object.
     *
     * @param target domain instance to copy properties to
     * @param source - domain class to copy properties from
     * @param override - properties to override after copying
     * @param ignoreAssociations - should associations be copied ? - ignored by default
     */
    @CompileStatic
    static Object bindFast(Object target, Map<String, Object>  source, Map<String,Object> override = [:], boolean ignoreAssociations = false) {
        if (target == null) throw new IllegalArgumentException("Target is null")
        if (source == null) return null

        GormStaticApi sapi = GormEnhancer.findStaticApi(target.getClass())
        List<PersistentProperty>  properties = sapi.gormPersistentEntity.getPersistentProperties()
        for (PersistentProperty prop : properties){
            if(!source.containsKey(prop.name)) {
                continue
            }
            Object sval = source[prop.name]
            Object valToAssisgn = sval
            Class typeToConvertTo = prop.getType()
            if (prop instanceof Association && sval['id']) {
                if(ignoreAssociations) continue
                Association asocProp = (Association)prop
                GormStaticApi asc = GormEnhancer.findStaticApi(asocProp.associatedEntity.javaClass).load(sval['id'] as Long)
                target[prop.name] = asc
            }
            else if (sval instanceof String) {
                if(Number.isAssignableFrom(typeToConvertTo)){
                    valToAssisgn = (sval as String).asType(typeToConvertTo)
                }
                else if(Date.isAssignableFrom(typeToConvertTo)){
                    //valToAssisgn = dateFormat.parse(sval as String)
                    valToAssisgn = DateUtil.parseJsonDate(sval as String)
                    //println "converted $sval to ${valToAssisgn} for $prop.name with DateUtil.parseJsonDate"
                }
                else if(hasConverter(typeToConvertTo)){
                    ValueConverter converter = getConverter(typeToConvertTo, sval)
                    if (converter) {
                        valToAssisgn= converter.convert(sval)
                        //println new Date()
                        //println "converted $sval to ${valToAssisgn} for $prop.name with ${converter.class.name}"
                    }
                }
            }

            target[prop.name] = valToAssisgn

            //println prop
            //println "${prop.name}: ${obj[prop.name]} -> region:${obj.region}"
        }

        if (override) {
            override.each{String key, val ->
                if(target.hasProperty(key)){
                    target[key] = val
                }
            }
        }

        return target
    }
    //FIXME THIS IS BAD AND SLOW, NEEDS TO BE MOVED TO BEAN
    static boolean hasConverter(Class typeToConvertTo) {
        GrailsWebDataBinder grailsWebDataBinder = (GrailsWebDataBinder)Holders.applicationContext.getBean('grailsWebDataBinder')
        return grailsWebDataBinder.conversionHelpers.containsKey(typeToConvertTo)
    }

    static ValueConverter getConverter(Class typeToConvertTo, value) {
        GrailsWebDataBinder grailsWebDataBinder = (GrailsWebDataBinder)Holders.applicationContext.getBean('grailsWebDataBinder')
        if(grailsWebDataBinder.conversionHelpers.containsKey(typeToConvertTo)) {
            List converters = grailsWebDataBinder.conversionHelpers.get(typeToConvertTo)
            converters?.find { ValueConverter c -> c.canConvert(value) }
        }
    }

    /**
     * Copy all given property values from source to target
     * if and only if target's properties are null.
     *
     * @param source    a source object
     * @param target    a target object
     * @param propNames array of property names which should be copied
     */
    static void copyProperties(Object source, Object target, String... propNames) {
        copyProperties(source, target, true, propNames)
    }

    /**
     * Copy all given property values from source to target.
     * It can be specified whether to copy values or not in case target's properties are not null.
     *
     * @param source         a source object
     * @param target         a target object
     * @param copyOnlyIfNull if 'true' then it will copy a value only if target's property is null
     * @param propNames      an array of property names which should be copied
     */
    static void copyProperties(Object source, Object target, boolean copyOnlyIfNull, String... propNames) {
        for (String prop : propNames) {
            if (copyOnlyIfNull && (target[prop] != null)) {
                continue
            } else {
                target[prop] = source[prop]
            }
        }

    }

    /**
     * Return the value of the (possibly nested) property of the specified name, for the specified source object
     *
     * Example getPropertyValue(source, "x.y.z")
     *
     * @param source - The source object
     * @param property - the property
     * @return value of the specified property or null if any of the intermediate objects are null
     */
    static Object getPropertyValue(Object source, String property) {
        Validate.notNull(source)
        Validate.notEmpty(property)

        Object result = property.tokenize('.').inject(source){ Object obj, String prop ->
            Object value = null
            if (obj != null) value = obj[prop]
            return value
        }

        return result
    }

}
