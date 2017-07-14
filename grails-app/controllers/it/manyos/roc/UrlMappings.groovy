package it.manyos.roc

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        /*"/$server/$form?/$query?"(controller:"api") {
            action = [GET: "get", PUT: "put", DELETE: "delete", POST: "post"]
        }*/

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
