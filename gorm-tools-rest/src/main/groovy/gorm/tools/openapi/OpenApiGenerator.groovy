/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package gorm.tools.openapi


import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import groovy.transform.CompileStatic

import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.beans.factory.annotation.Autowired

import gorm.tools.rest.ast.RestApiAstUtils
import gorm.tools.support.ConfigAware
import gorm.tools.utils.GormMetaUtils
import yakworks.commons.io.FileSystemUtils
import yakworks.commons.io.FileUtil
import yakworks.commons.lang.NameUtils

import static gorm.tools.openapi.ApiSchemaEntity.CruType

/**
 * Generates the domain part
 * should be merged with either Swaggydocs or Springfox as outlined
 * https://github.com/OAI/OpenAPI-Specification is the new openAPI that
 * Swagger moved to.
 * We are chasing this part https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#schemaObject
 * Created by JBurnett on 6/19/17.
 */
//@CompileStatic
@SuppressWarnings(['UnnecessaryGetter', 'AbcMetric', 'Println'])
@CompileStatic
class OpenApiGenerator implements ConfigAware {
    static final String API_SRC = 'src/api-docs'
    static final String API_BUILD = 'build/api-docs'

    @Autowired GormToSchema gormToSchema

    void generate() {
        def buildDest = makeBuildDirs()
        def srcPath = getApiSrcPath()

        FileSystemUtils.copyRecursively(srcPath, buildDest)

        generateModels()
        genOpenapiYaml()
    }

    /**
     * gets a path using the gradle.projectDir as the root
     */
    String getProjectDir(){
        return System.getProperty("gradle.projectDir", '')
    }

    /**
     * gets a path using the gradle.projectDir as the root
     */
    Path getApiSrcPath(String sub = null){
        def path =  Paths.get(getProjectDir(), API_SRC)
        return sub ? path.resolve(sub) : path
    }

    /**
     * gets a path using the gradle.projectDir as the root
     */
    Path getApiBuildPath(String sub = null){
        def path =  Paths.get(getProjectDir(), API_BUILD)
        return sub ? path.resolve(sub) : path
    }

    //creates the build/openapi dir
    Path makeBuildDirs(){
        def path = getApiBuildPath()
        Files.createDirectories(path)
        return path
    }

    //generates the openapi.yaml with paths. starts by reading the src/openapi/openapi.yaml
    void genOpenapiYaml(){
        def openapiYaml = getApiSrcPath('openapi/api.yaml')
        Map api = (Map)YamlUtils.loadYaml(openapiYaml)
        assert api['openapi'] == '3.0.3'
        spinThroughRestApi(api)
    }

    //iterate over the restapi keys and add setup the yaml
    void spinThroughRestApi(Map api){
        Map restCfg = config.getProperty('restApi', Map)
        Map restApi = restCfg.paths as Map<String, Map>

        //Map paths = (Map)api.paths
        List tags = (List)api.tags
        Map<String, List> xTagGroups = [:]

        for(entry in restApi){
            String pathName = entry.key
            Map pathMap = (Map)entry.value
            Map pathParts = RestApiAstUtils.splitPath(pathName, pathMap)
            String endpoint = pathParts.name
            String namespace = pathParts.namespace

            Map tagEntry = [name: endpoint]
            if(pathMap.description) tagEntry.description = pathMap.description
            tags << tagEntry

            if(!xTagGroups[namespace]) xTagGroups[namespace] = []
            xTagGroups[namespace].add(endpoint)

            try{
                createPaths(api, endpoint, pathMap)
            } catch(e){
                String msg = "Error on $endpoint"
                throw new IllegalArgumentException(msg, e)
            }

        }
        api.tags = tags
        //api.paths = paths
        def xTagGroupsList = []
        xTagGroups.each{k, v ->
            xTagGroupsList << [name: k, tags: v as List]
        }
        api['x-tagGroups'] = xTagGroupsList
        def buildOpenapiYaml = getApiBuildPath().resolve('openapi/api.yaml')
        YamlUtils.saveYaml(buildOpenapiYaml, api)
    }

    //create the files for the path
    Map createPaths(Map api, String endpoint, Map pathMap){
        Map paths = (Map)api.paths
        String namespace = pathMap.namespace ?: ''
        String pathKey = "/${namespace}/${endpoint}"//.toString()
        String pathKeyId = "${pathKey}/{id}"//.toString()
        String pathKeyPrefix = pathMap.namespace ? "${pathMap.namespace}/" : ''
        pathKeyPrefix = "./paths/${pathKeyPrefix}"
        String pathKeyRef = "${pathKeyPrefix}${endpoint}.yaml"//.toString()
        String pathKeyIdRef = "${pathKeyPrefix}${endpoint}@{id}.yaml"//.toString()
        //make sure dirs exist
        Files.createDirectories(getApiBuildPath('openapi').resolve(pathKeyPrefix))

        paths[pathKey] = ['$ref': pathKeyRef]
        paths[pathKeyId] = ['$ref': pathKeyIdRef]

        String capitalName = NameUtils.getClassNameFromKebabCase(endpoint)
        String modelName = NameUtils.getShortName((String)pathMap.entityClass)
        Map model = [endpoint: endpoint, name: endpoint, capitalName: capitalName, modelName: modelName, namespace: namespace]

        //createPathFiles(pathKeyRef, model)
        //createPathIdFiles(pathKeyIdRef, model)
        //path file
        String tplRef = "${pathKeyPrefix}${endpoint}.yaml"//.toString()
        processTplFile('paths/tpl.yaml', tplRef, model)

        //path Id file
        tplRef = "${pathKeyPrefix}${endpoint}@{id}.yaml"//.toString()
        processTplFile('paths/tpl@{id}.yaml', tplRef, model)

        // tplRef = "${pathKeyPrefix}${endpoint}_pager.yaml"//.toString()
        // processTplFile('paths/tpl_pager.yaml', tplRef, model)
        // tplRef = "${pathKeyPrefix}${endpoint}_request_create.yaml".toString()
        // processTplFile('paths/tpl_request_create.yaml', tplRef, model)
        // tplRef = "${pathKeyPrefix}${endpoint}_request_update.yaml".toString()
        // processTplFile('paths/tpl_request_update.yaml', tplRef, model)
        // tplRef = "${pathKeyPrefix}${endpoint}_response.yaml".toString()
        // processTplFile('paths/tpl_response.yaml', tplRef, model)
    }

    //create the files for the path
    void createPathFiles(String pathRef, Map model){
        Map tplYaml = (Map)YamlUtils.loadYaml(getApiSrcPath('openapi/paths/tpl.yaml'))
        Map tplGet = (Map)tplYaml['get']
        tplGet.tags = [model.name]
        tplGet.summary = "${model.capitalName} List".toString()
        tplGet.description = "Query and retrieve a ${model.capitalName} list".toString()
        tplGet.operationId = "get${model.capitalName}List".toString()
        tplGet['responses']['200']['$ref'] = "${model.name}_pager.yaml".toString()

        Map tplPost = (Map)tplYaml['post']
        tplPost.tags = [model.name]
        tplPost.summary = "Create a ${model.capitalName}".toString()
        tplPost.description = "Create a new ${model.capitalName}".toString()
        tplPost.operationId = "create${model.capitalName}".toString()
        tplPost.requestBody['$ref'] = "${model.name}_request_create.yaml".toString()
        tplPost['responses']['201']['$ref'] = "${model.name}_response.yaml".toString()

        def pathYaml = getApiBuildPath('openapi').resolve(pathRef)
        YamlUtils.saveYaml(pathYaml, tplYaml)
    }

    //create the files for the path
    void createPathIdFiles(String pathRef, Map model){
        Path tplFile = getApiSrcPath().resolve('openapi/paths/tpl@{id}.yaml')
        Map tplYaml = (Map)YamlUtils.loadYaml(tplFile)

        Map tplGet = (Map)tplYaml['get']
        tplGet.tags = [model.name]
        tplGet.summary = "Get a ${model.capitalName}".toString()
        tplGet.description = "Retrieve a ${model.capitalName}".toString()
        tplGet.operationId = "get${model.capitalName}ById".toString()
        tplGet['responses']['200']['$ref'] = "${model.name}_response.yaml".toString()

        Map tplPut = (Map)tplYaml['put']
        tplPut.tags = [model.name]
        tplPut.summary = "Update a ${model.capitalName}".toString()
        tplPut.operationId =  "update${model.capitalName}".toString()
        tplPut.requestBody['$ref'] = "${model.name}_request_update.yaml".toString()
        tplPut['responses']['200']['$ref'] = "${model.name}_response.yaml".toString()

        Map tplDelete = (Map)tplYaml['delete']
        tplDelete.tags = [model.name]
        tplDelete.summary = "Delete a ${model.capitalName}".toString()
        tplDelete.description = "Delete a ${model.capitalName} and any child associations".toString()
        tplDelete.operationId = "delete${model.capitalName}".toString()

        def pathYaml = getApiBuildPath('openapi').resolve(pathRef)
        YamlUtils.saveYaml(pathYaml, tplYaml)
    }

    void processTplFile(String srcPath, String outputPath, Map model){
        Path tplFile = getApiSrcPath('openapi').resolve(srcPath)
        String ymlTpl = FileUtil.readFileToString(tplFile.toFile())
        ymlTpl = FileUtil.parseStringAsGString(ymlTpl, model)
        Path outPath = getApiBuildPath('openapi').resolve(outputPath)
        Files.write(outPath, ymlTpl.getBytes())
    }

    void generateModels() {
        def mapctx = GormMetaUtils.getMappingContext()
        for( PersistentEntity entity : mapctx.persistentEntities){
            def map = gormToSchema.generate(entity, CruType.Read)
            def mapCreate = gormToSchema.generate(entity, CruType.Create)
            def mapUpdate = gormToSchema.generate(entity, CruType.Update)
            writeYmlModel(entity.javaClass, map, CruType.Read)
            writeYmlModel(entity.javaClass, mapCreate, CruType.Create)
            writeYmlModel(entity.javaClass, mapUpdate, CruType.Update)
        }
    }

    Path writeYmlModel(Class clazz, Map schemaMap, CruType type) {
        //if type is read then dont do suffix
        String suffix = type == CruType.Read ? '' : "_$type"
        Files.createDirectories(getApiBuildPath('openapi/models'))
        def path = getApiBuildPath("openapi/models/${clazz.simpleName}${suffix}.yaml")
        YamlUtils.saveYaml(path, schemaMap)
        return path
    }

}
