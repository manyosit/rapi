package it.manyos.roc

import com.bmc.arsys.api.ARServerUser
import grails.converters.JSON
import grails.converters.XML
import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

class ApiController {
    def remedyService

    def getNewFieldID() {
        def returnValue = [:]
        def fieldID = 0
        def message = 'success'
        def environment = RemedyEnvironment.get(params.id)
        if (!environment) {
            message = 'Error. Environment not found'
        } else {
            fieldID = environment.lastFieldID+1
            environment.lastFieldID = fieldID;
            environment.save(flush: true)
            returnValue['fieldID'] = fieldID
        }
        returnValue['message'] = message
        render returnValue as JSON
    }

    def index() {
        TreeMap totals = [:]
        def myMetric = Metric.get(2)
        def results = myMetric.results
        def jsonSlurper = new JsonSlurper()
        results.each {

            def myObject = jsonSlurper.parseText(it.value)
            def date = it.executionDate.format("yyyy-MM-dd HH:mm")
            def myTotals = totals[date]
            //totals[date]
            def arServer = myObject."AR Server"
            if (arServer) {
                //println '' + date + ' : ' + arServer
                def fixed = arServer.fixed?:0
                def floating = arServer.floating?:0
                def count = fixed + floating
                println()
                totals[date] = totals[date]?:0 + count
            }
        }
        println('totals: ' + totals);
        def returns = [:]
        returns['keys'] = totals.keySet()
        returns['values'] = totals.values()
        render returns as JSON
    }

    def licenseCount() {
        TreeMap totals = [:]
        def max=params.max?:100
        def myMetric = Metric.get(2)
        def results = myMetric.results
        /*def results = MetricResult.findAllByMetric(myMetric,
                [max: max, sort: "executionDate", order: "desc", offset: 0])
        println(results.size())*/
        def jsonSlurper = new JsonSlurper()
        results.each {

            def myObject = jsonSlurper.parseText(it.value)
            def date = it.executionDate.format("yyyy-MM-dd HH:mm")

            def licenseData = [:]
            if (totals[date])
                licenseData = totals[date]

            //totals[date]
            def arServer = myObject."AR Server"
            if (arServer) {
                //Get AR Data
                def fixed = arServer.fixed?:0
                def floating = arServer.floating?:0
                def read = arServer.read?:0

                if (licenseData.'arFixed')
                    licenseData['arFixed'] = licenseData['arFixed'] + fixed
                else
                    licenseData['arFixed'] = fixed

                if (licenseData.'arFloating')
                    licenseData['arFloating'] = licenseData['arFloating'] + floating
                else
                    licenseData['arFloating'] = floating

                if (licenseData.'arRead')
                    licenseData['arRead'] = licenseData['arRead'] + read
                else
                    licenseData['arRead'] = read

                totals[date] = licenseData

            }
        }
        def allValues = []
        //Reverse Sort
        Comparator comparator = [compare: {a , b -> b.compareTo(a) }] as Comparator
        //Map sortedMap = new TreeMap(comparator)
        //sortedMap.putAll(totals)

        totals.keySet().each {
            def resultValue = ['executionDate':it, 'licenseData':totals[it]]
            allValues.add(resultValue)
        }
        def renderValue = ['results':allValues]
        render renderValue as JSON
    }

    def environments = {
        def environmentList = []
        def environments = RemedyEnvironment.findAll()
        environments.each {
            def myEnvironment = [:]
            myEnvironment['id'] = it.id
            myEnvironment['name'] = it.name
            myEnvironment['category'] = it.category
            environmentList.add(myEnvironment)
        }
        //println environments
        def returnValue = ['environments':environmentList]
        render returnValue as JSON
    }

    def activeQuery = {
        def returnValue = [:]
        returnValue['status'] = 'success'
        def startDate = new Date()
        log.debug "params" + params.toString()
        def returnFieldNames = true
        def translateSelectionFields = true
        def format = "JSON"
        def countOnly = false
        def showServerConfig = false
        def showServerStatistics = false
        def showDisplayOnlyFields = false
        ARServerUser context = new ARServerUser();
        try {
            //check for environment
            if (params.environment == null || params.environment == '')
                throw new Exception("Environment not provided")
            def environment = RemedyEnvironment.get(params.environment)
            if (!environment)
                throw new Exception("Environment not found")
            def server = environment.server[0]
            log.debug("Server: " + server)
            context = remedyService.getARContext(server)
            //context = remedyService.getARContext("server", 0.intValue(), "a", "v")
            //def records
            //Set parameter
            if (params.fieldNames && params.fieldNames.equals("false"))
                returnFieldNames = false
            if (params.countOnly && params.countOnly.equalsIgnoreCase("true"))
                countOnly = true
            if (params.translateSelectionFields && params.translateSelectionFields.equals("false"))
                translateSelectionFields = false
            if (params.format && params.format.equalsIgnoreCase("XML"))
                format = "XML"
            if (params.showServerConfig && params.showServerConfig.equalsIgnoreCase("true"))
                showServerConfig = true
            if (params.showServerStatistics && params.showServerStatistics.equalsIgnoreCase("true"))
                showServerStatistics = true
            if (params.showDisplayOnlyFields && params.showDisplayOnlyFields.equalsIgnoreCase("true"))
                showDisplayOnlyFields=true

            //Return Formlist if no query given
            if (params.form == null || params.form == '') {
                if (showServerConfig)
                    returnValue['config'] = remedyService.getServerConfig(context)
                else if (showServerStatistics)
                    returnValue['statistics'] = remedyService.getServerStatistics(context)
                else
                    returnValue['forms'] = remedyService.getForms(context)
                if (format == "XML")
                    render returnValue as XML
                else
                    render returnValue as JSON
            }
            //Return Fielddef if no query given
            else if (params.query == null || params.query == '') {
                returnValue['fields'] = remedyService.getFields(context, params.form)
                returnValue['form'] = params.form
                if (format == "XML")
                    render returnValue as XML
                else
                    render returnValue as JSON
            }
            //Return only size
            else if (countOnly == true) {
                def resultSize = remedyService.countRecords(context,
                        params.form,
                        params.query)
                if (format == "XML")
                    render resultSize as XML
                else
                    render resultSize as JSON
            } else {
                //Return records
                def records = remedyService.queryForm(context,
                        params.form,
                        params.query,
                        returnFieldNames,
                        translateSelectionFields,
                        params.firstEntry?.toInteger() ?:0,
                        params.maxEntries?.toInteger() ?:0,
                        showDisplayOnlyFields)

                returnValue['status'] = 'success'
                returnValue['query'] = params.query
                returnValue['server'] = server.name + ":" + server.port
                returnValue['form'] = params.form
                returnValue['runtime'] = new Date().getTime() - startDate.getTime()
                returnValue['dataSize'] = records.size()
                returnValue['data'] = records

                if (format == "XML")
                    render returnValue as XML
                else
                    render returnValue as JSON
            }
        } catch (Exception e) {
            returnValue['status'] = 'error'

            if (e.getCause() == null) {
                //render e.getClass().getSimpleName().toString() + ": " + e.toString()
                returnValue['message'] = e.getClass().getSimpleName().toString() + ": " + e.toString()
                log.error(e.getMessage())
            }
            else {
                returnValue['message'] = e.getClass().getSimpleName().toString() + ": " + e.toString()
                //render e.getClass().getSimpleName().toString() + ": " + (e.getCause().toString());
                log.error e.getCause()
            }

            if (format == "XML")
                render returnValue as XML
            else
                render returnValue as JSON

        } finally {
            context.logout()
        }
    }
}
