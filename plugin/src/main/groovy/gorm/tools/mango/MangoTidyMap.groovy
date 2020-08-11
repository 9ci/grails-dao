/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.mango

import groovy.transform.CompileStatic

/**
 * Utils to normalizes params map to transform it to mango language
 */
@CompileStatic
class MangoTidyMap {

    /**
     * Transforms passed params map to normalized mango criteria map
     *
     * @param map params that should be transformed to mango language
     * @return normalized mango map
     */
    static Map tidy(Map<String, Object> map) {
        Map nested = [:]
        map.each { String k, Object v ->
            pathToMap(k, v, nested)
        }
        toMangoOperator(nested)
    }

    /**
     * Extends the map with nested value by specific path
     * so pathToMap("a.b.c", 1, [:]) -> [a:[b:[c:1]]]
     * or pathToMap("a.b.c", 1, [d:2]) -> [a:[b:[c:1]], d:2]
     *
     * @param path path in the nested map where value should be placed
     * @param val value that should be added to the nested map
     * @param map map that should be extended with the nested value
     * @return extended map
     */
    static Map pathToMap(String path, Object val, Map map) {
        if (path == '$sort') {
            return tidySort(path, val, map)
        } else if (path.contains(".")) {
            String[] splitPath = path.split("[.]")
            //get first thing in dot ex: foo.bar this will be foo
            String newKey = splitPath[0]
            if (!map[newKey]) map[newKey] = [:]
            String newPath = splitPath.tail().join(".")
            pathToMap(newPath, val, map[newKey] as Map)
        }
        else {
            //we should check if nested values have composed keys("customer.address.id")
            if (val instanceof Map) {
                if (!map[path]) map[path] = [:]
                (val as Map).each {
                    pathToMap(it.key as String, it.value, map[path] as Map)
                }
            } else {
                map[path] = val
            }
        }
        return map
    }

    /**
     * Adds mango operators based on values types
     *
     * @param map params that should be extended with mango operators
     * @param result map that should contain mango results
     * @return map with mango criteria params
     */
    static Map toMangoOperator(Map map, Map result = [:]) {
        map.each { key, val ->
            result[key] = [:]
            if (MangoBuilder.JunctionOps.keySet().contains(key)) {
                if (val instanceof Map) {
                    result[key] = (val as Map).collect { k, v -> tidy([(k.toString()): v]) }
                    return
                }

                if (val instanceof List) {
                    result[key] = (val as List).collect { v -> tidy(['$and': v]) }
                    return
                }
            }
            if (val instanceof Map && key != '$sort') {
                toMangoOperator(val as Map, result[key] as Map)
            } else {
                if (key.toString().startsWith('$')) {
                    result[key] = val
                    return
                } //if we already have Mango method
                if (val instanceof List) {
                    List valAsList = val as List
                    // for handling case {customer: [{id:1}, {id:2}]}, transforms to {customer:{id:{'$in': [1,2]}}}
                    if (valAsList[0] instanceof Map) {
                        Map mapVal = valAsList[0]
                        Map inMap = ['$in': valAsList.collect { (it as Map).values()[0] }]
                        result[key]["${mapVal.keySet()[0]}"] = inMap
                        return
                    }
                    result[key]['$in'] = val
                    return
                }
                if (val instanceof String && val.contains("%")) {
                    result[key]['$ilike'] = val
                    return
                }
                if (MangoBuilder.ExistOps.keySet().contains(val)) {
                    (result[key] as Map)[val] = true
                } else {
                    result[key]['$eq'] = val
                }
            }
        }
        result

    }

    static Map tidySort(String path, Object val, Map map) {
        if (val instanceof String) {
            String sval = (val as String).trim()
            if (sval.contains(',') || sval.contains(' ')) {
                Map<String, String> sortMap = [:]
                sval.split(",").each { String item ->
                    String[] sorting = item.trim().split(" ")
                    sortMap[(sorting[0])] = sorting[1] ?: 'asc'
                }
                map[path] = sortMap
            } else {
                map[path] = sval
            }
        } else {
            map[path] = val
        }
        return map
    }

}
