package it.manyos.rapi

class UrlMappings {

    static mappings = {
        /*"/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }*/

        "/$server/$form?/$query?"(controller:"api") {
            action = [GET: "get", PUT: "put", DELETE: "delete", POST: "post"]
        }

        "/$server/$form/getAttachment/$entryId/$fieldId"(controller:"api") {
            action = [GET: "getAttachment"]
        }

        "/$server/$form/setAttachment/$entryId/$fieldId"(controller:"api") {
            action = [POST: "setAttachment"]
        }

        /*"/$server/status"(controller:"api") {
            action = [GET: "status"]
        }*/

        /*"/$server/$form?/$query?"(controller:"api") {
            action = [GET: "get", PUT: "put", DELETE: "delete", POST: "post"]
        }*/

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
