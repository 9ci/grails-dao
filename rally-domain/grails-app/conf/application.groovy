
grails {
    // gorm.failOnError = true
    // gorm.default.mapping = {
    //     id generator: 'gorm.tools.hibernate.SpringBeanIdGenerator'
    //     '*'(cascadeValidate: 'dirty')
    //     //cache usage: System.getProperty("cacheStrategy", "read-write").toString()
    // }
    gorm.default.constraints = {
        '*'(nullable:true)
    }
}

// grails.config.locations =  ["classpath:yakworks/test-config.groovy"]
//
// String projectRoot = System.getProperty('gradle.rootProjectDir')
// app {
//     resources {
//         currentTenant = {
//             return [num: 'virgin', id: 2]
//         }
//         rootLocation = { args ->
//             File root = new File("${projectRoot}/resources")
//             return root.canonicalPath
//         }
//         tempDir = {
//             File file = new File("./build/rootLocation/tempDir")
//             if (!file.exists()) file.mkdirs()
//             return file.canonicalPath
//         }
//         attachments.location = 'attachments'
//     }
// }
