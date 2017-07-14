package it.manyos.roc

import com.bmc.arsys.api.*
import com.bmc.rsso.client.impl.RSSOClient

import javax.servlet.http.HttpServletRequest


class ServerController {
    def RemedyService

    static scaffold = Server

    def run = {


        def activate = grailsApplication.config.getProperty('authentication.rsso-enabled')

        log.debug("RSSO activate status = " +activate)

        if (activate) {
            //RemedyService.processMetrics()
            def rsso = new RSSOClient()
            def token = rsso.getToken(request)
            log.error "Token: " + token.toString()
            def principal = token.getPrincipal()
            log.error "Principal: " + principal
            def name = principal.getName()
            log.error "Name: " + name

            render "Hello " + name
        } else render "RSSO is not enabled"
    }

    def test = {
        ARServerUser context = new ARServerUser();
        context = RemedyService.getARContext("54.201.167.131",46200,"admin","Yaq12wsx#")

        /*def status = [:]

        def server = [:]
        server["Name"] = context.getServer()
        server["Port"] = context.getPort()
        server["Version"] = context.getServerVersion()
        server["CharSet"] = context.getServerCharSet()
        status["Date"] = new Date()
        status["ServerInfo"] = server
        status["CurrentLicenses"] = RemedyService.getCurrentLicenses(context)
        */

        def status = RemedyService.queryForm(context, 'User', '1=1', true, true, 0.intValue(), 0.intValue(), false)

        render status as grails.converters.JSON
    }
}
