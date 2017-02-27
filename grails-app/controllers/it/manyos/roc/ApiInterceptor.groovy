package it.manyos.roc


class ApiInterceptor {

    boolean before() {
        //fix for all locations
        def origin = request.getHeader("Origin");
        header( "Access-Control-Allow-Origin", origin )
        header( "Access-Control-Allow-Credentials", "true" )
        header( "Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE" )
        header( "Access-Control-Max-Age", "3600" )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
