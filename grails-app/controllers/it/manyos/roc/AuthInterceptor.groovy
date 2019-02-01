package it.manyos.roc


class AuthInterceptor {

    AuthInterceptor() {
        match(controller: "api", action: "*")
    }

    boolean before() {
        //log.error "auth filter"
        response.setHeader('WWW-Authenticate', 'basic realm="myRealm"')
        def authHeader = request.getHeader('Authorization')
        //log.error "auth filter: " +request.getHeader('Authorization')
        if (authHeader) {
            //log.error "auth filter true: " + authHeader.split(' ')[1].decodeBase64()
            def usernamePassword = new String(authHeader.split(' ')[1].decodeBase64())
            if (usernamePassword) {
                return true
            }
        }
        //log.error "auth filter: false"
        response.setHeader('WWW-Authenticate', 'basic realm="myRealm"')
        response.sendError(response.SC_UNAUTHORIZED)
        return false
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
