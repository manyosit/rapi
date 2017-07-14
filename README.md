# roc-server
Grails Applikation die sich in Remedy SSO integriert und API call unter dem Account des Users ermöglicht.

Die SSO Integration wird in der resources.groovy aktiviert:

    beans = {
        def activate = grailsApplication.config.getProperty('authentication.rsso-enabled')

        log.debug("RSSO activate status = " +activate)

        if (activate) {
            myFilter(FilterRegistrationBean) {
                filter = new com.bmc.rsso.agent.RSSOFilter()
                order = Ordered.HIGHEST_PRECEDENCE
                urlPatterns = ["/*"]
            }
        }
    }