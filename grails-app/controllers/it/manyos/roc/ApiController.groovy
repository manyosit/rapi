package it.manyos.roc

import com.bmc.arsys.api.ARServerUser
import grails.converters.JSON
import grails.converters.XML
import groovy.json.JsonSlurper
import org.springframework.web.multipart.MultipartHttpServletRequest

import java.text.SimpleDateFormat

class ApiController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE", getNewFieldID: "GET", licenseCount:"GET", environments:"GET", activeQuery:"GET"]

    def remedyService

    /*def getNewFieldID() {
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
    }*/

    def index() {
    }

    /*def licenseCount() {
        TreeMap totals = [:]
        def max=params.max?:100
        def myMetric = Metric.get(2)
        def results = myMetric.results
        /*def results = MetricResult.findAllByMetric(myMetric,
                [max: max, sort: "executionDate", order: "desc", offset: 0])
        println(results.size())*/
    /*
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
    }*/

    /*def environments = {
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
    }*/

    def get() {
        log.debug "Execute get with params: " + params
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
        def cacheResults = false
        def cacheTime = 600000
        def impersonateUser = null
        ArrayList fields = null;
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
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
            if (params.cacheResults && params.cacheResults.equalsIgnoreCase("true"))
                cacheResults=true
            if (params.cacheTime)
                cacheTime=params.cacheTime
            if (params.fields)
                fields=params.fields.tokenize( ',' );

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
                        showDisplayOnlyFields, cacheResults, cacheTime, fields)

                returnValue['status'] = 'success'
                returnValue['query'] = params.query
                returnValue['server'] = params.server + ":" + params.port?.toInteger() ?: 0
                returnValue['form'] = params.form
                returnValue['runtime'] = new Date().getTime() - startDate.getTime()
                returnValue['dataSize'] = records.size()
                returnValue['data'] = records
                //Pobiert, war aber nix
                /*if (records.size==0) {
                    response.status = 404
                }*/
                if (format == "XML")
                    render returnValue as XML
                else
                    render returnValue as JSON
            }
        } catch (Exception e) {
            returnValue['status'] = 'error'
            response.status = 500
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


    //Put = Update
    def put() {
        log.debug("Params: " + params)
        def format = "JSON"
        def impersonateUser = null
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
            def returnValue
            //checke form
            if (params.form == null || params.form.equals(""))
                render "Please provide a form"
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                format = "XML"
                //create Entries
                /*request.XML.entry.entry.Strasse__c.each {
                    log.debug it
                }*/
                returnValue = remedyService.updateEntries(context, params.form, request.XML)
                render(status: 200, text: returnValue) as XML
            } else {
                returnValue = remedyService.updateEntries(context, params.form, request.JSON)
                render(status: 200, text: returnValue) as JSON
                //render returnValue as JSON
            }
        } catch (Exception e) {
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                render(status: 500, text: e.getMessage()) as XML
            } else {
                render(status: 500, text: e.getMessage()) as JSON
            }
            log.error(e.getMessage())
        } finally {
            context.logout()
        }
    }

    def delete() {
        //TODO Kann kein Chunk Delete (falls return list eingeschr�nkt ist)
        log.debug("Params: " + params)
        def format = "JSON"
        def returnFieldNames = true
        def translateSelectionFields = true
        def impersonateUser = null
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
            def records
            //Return Error if no query given
            if (params.form == null || params.form == '') {
                render "Error: Please provide a form!"
            }
            //Check format
            if (params.format && params.format.equalsIgnoreCase("XML"))
                format = "XML"
            //Return Error if no query given
            if (params.query == null || params.query == '') {
                def fields = remedyService.getFields(context, params.form)
                render "Error: Please provide a query!"
            } else {
                //Return records
                records = remedyService.deleteEntries(context,
                        params.form,
                        params.query,
                        params.firstEntry?.toInteger() ?:0,
                        params.maxEntries?.toInteger() ?:0)
                if (format == "XML")
                    render records as XML
                else
                    render records as JSON
            }
        } catch (Exception e) {
            log.error(e.getMessage())
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                render(status: 500, text: e.getMessage()) as XML
            } else {
                render(status: 500, text: e.getMessage()) as JSON
            }
        } finally {
            context.logout()
        }

    }

    //Post = create
    def post() {
        log.debug ('params', params);
        def format = "JSON"
        def impersonateUser = null
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
            def returnValue
            //checke form
            if (params.form == null || params.form.equals(""))
                render "Please provide a form"
            //check format
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                format = "XML"
                returnValue = remedyService.createEntries(context, params.form, request.XML)
                render returnValue as XML
            } else {
                //create Entries
                returnValue = remedyService.createEntries(context, params.form, request.JSON)
                render returnValue as JSON
            }
        } catch (Exception e) {
            log.error(e.getMessage())
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                render(status: 500, text: e.getMessage()) as XML
            } else {
                render(status: 500, text: e.getMessage()) as JSON
            }
        } finally {
            context.logout()
        }
    }

    def getAttachment() {
        //log.debug params
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
            int[] fieldIds = [Integer.parseInt(params.fieldId)]
            def myEntry = context.getEntry(params.form, params.entryId, fieldIds)
            //log.debug myEntry
            def attachmentValue = myEntry.get(Integer.parseInt(params.fieldId)).getValue()
            //log.debug attachmentValue
            def fileName = attachmentValue.getName()
            //log.debug fileName
            response.setContentType("application/octet-stream") // or or image/JPEG or text/xml or whatever type the file is
            response.setHeader("Content-disposition", "attachment;filename=${fileName}")
            response.outputStream << context.getEntryBlob(params.form, params.entryId, Integer.parseInt(params.fieldId))
        } catch (Exception e) {
            log.error(e.getMessage())
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                render(status: 500, text: e.getMessage()) as XML
            } else {
                render(status: 500, text: e.getMessage()) as JSON
            }
        } finally {
            context.logout()
        }
    }


    def setAttachment() {
        //log.debug params
        //log.debug request.toString()
        ARServerUser context = new ARServerUser();
        try {
            if (params.impersonateUser ) {
                impersonateUser = params.impersonateUser
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0, impersonateUser)
            } else {
                context = remedyService.getARContext(params.server, params.port?.toInteger() ?: 0)
            }
            if (request instanceof MultipartHttpServletRequest){
                //log.debug request.getFileNames().toString()
                //Get the file's name from request
                def fileName = request.getFileNames()[0]
                //Get a reference to the uploaded file.
                def uploadedFile = request.getFile(fileName)
                //log.debug(fileName)
                //log.debug uploadedFile.getBytes()
                render remedyService.setAttachment(context, params.form, params.entryId, Integer.parseInt(params.fieldId), fileName, uploadedFile)
            } else {
                def returnValue = new HashMap()
                returnValue[params.entryId] = "error: No file provied"
                render returnValue
            }
        } catch (Exception e) {
            log.error(e.getMessage())
            if (params.format && params.format.equalsIgnoreCase("XML")) {
                render(status: 500, text: e.getMessage()) as XML
            } else {
                render(status: 500, text: e.getMessage()) as JSON
            }
        } finally {
            context.logout()
        }
    }
}
