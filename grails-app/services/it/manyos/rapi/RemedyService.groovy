package it.manyos.rapi

import com.bmc.arsys.api.*
import java.text.SimpleDateFormat

class RemedyService {
    def cache = new HashMap()
    def resultCache = new HashMap()
    def cacheTime = 10 * 60 * 1000 //10 minutes

    def sessionFactory
    def UtilService
    def DataParser

    def getCurrentLicenses(ARServerUser context) {
        def licenseStats = [:]

        def userInfo = context.getListUser(2,0)

        userInfo.each {
            it.licenseInfo.each { licInfo ->
                def currentLicenseType //= "LicType:" + licInfo.currentLicenseType
                switch ( licInfo.currentLicenseType) {
                    case 0:
                        currentLicenseType = "read"
                        break
                    case 1:
                        currentLicenseType = "fixed"
                        break
                    case 2:
                        currentLicenseType = "floating"
                        break
                    default:
                        currentLicenseType = "unknown"
                        break
                }

                //handle AR Server license
                def licName = licInfo.appLicenseDescriptor
                if (licName == "") {
                    licName = "AR Server"
                }
                //Handle app license
                def licDetails = licenseStats[licName]

                if (!licDetails) {
                    licenseStats[licName] = [:]
                    licenseStats[licName]["${currentLicenseType}"] = 1
                } else if (!licDetails[currentLicenseType]) {
                    licenseStats[licName]["${currentLicenseType}"] = 1
                } else {
                    //filter wrong read
                    if (licName.equalsIgnoreCase("AR Server") && licInfo.licenseTag == 3) {
                        //ignore wrong read licenses
                    } else
                        licDetails["${currentLicenseType}"]++
                }
            }
        }
        return licenseStats
    }

    def getARContext(String server, int port, String user, String password) {
        ARServerUser ctx = new ARServerUser();
        ctx.setServer(server);
        ctx.setPort(port)
        log.debug("Connect to Server: " + ctx.getServer() + " on Port " + port + " with user " + user)
        ctx.setUser(user);
        ctx.setPassword(password);
        ctx.verifyUser()
        log.debug "Connected."
        return ctx
    }

    def getARContext(String server, int port, String user, String password, String impersonateUser) {
        ARServerUser ctx = getARContext(server, port, user, password)
        ctx.impersonateUser(impersonateUser)
        log.debug "impersonated User " + impersonateUser
        return ctx
    }

    def getARContext(String server, int port) {
        ARServerUser ctx = getARContext(server, port, UtilService.getUsername(), UtilService.getPassword())
        return ctx
    }

    def getARContext(String server, int port, String impersonateUser) {
        ARServerUser ctx = getARContext(server, port, UtilService.getUsername(), UtilService.getPassword(), impersonateUser)
        return ctx
    }

    /**
     * @param context The AR System Context to work with
     * @param schema The schema to query
     * @param query The query itself. If empty it will be set to '1'!=$NULL$
     * @return returns the record size
     */
    def countRecords(ARServerUser context, String schema, String query) {
        def startDateEach = new Date()
        def returnValue = new TreeMap();
        def formFields = getFields(context, schema)
        log.debug "Query " + schema + " with " + query
        if (query == null || query == '')
            query = "'1' != \$NULL\$"
        QualifierInfo qual = context.parseQualification(schema, query);
        OutputInteger arSize = new OutputInteger();
        List<EntryListInfo> eListInfos = context.getListEntry(schema, qual, 0, 0, null, null, false, arSize);
        def duration = new Date().getTime() - startDateEach.getTime()
        log.debug "Query returned " + eListInfos.size() + " records of total " + arSize
        returnValue["form"] = schema
        returnValue["query"] = query
        returnValue["dataSize"] = arSize.intValue()
        returnValue["runtime"] = duration
        return returnValue
    }


    /**
     * @param context The ARServerUser context to connect to the AR System Server
     * @param schema The schema name to get the field information
     * @return fieldDefinition of the schema
     */
    def getFields(ARServerUser context, String schema) {
        //log.debug context.getServer()
        //log.debug context.getUser()
        log.debug "load schema definition"
        //def cachedFields = cache.get(context.getServer())
        def serverCache = cache.get(context.getServer())
        def userCache = null
        if (serverCache != null) {
            userCache = serverCache.get(context.getUser())
            if (userCache != null) {
                def cachedFields = userCache.get(schema)
                if (cachedFields != null && ((new Date().getTime() - cachedFields.cacheDate.getTime()) < cacheTime)) {
                    log.debug "load schema definition - fetched from cache"
                    log.debug "cache valid since " + (new Date().getTime() - cachedFields.cacheDate.getTime()) + " msecs. Max cache time " + cacheTime + " msecs"
                    return cachedFields.fields
                }
            }
        }
        log.debug "load schema definition - fetched from server"
        //throw new Exception("Form not found")
        def fields = context.getListFieldObjects(schema)
        def allFields = new ArrayList()
        fields.each {
            //Set entryMode
            def entryMode = ""
            if (it.getFieldOption() == 1)
                entryMode = "Required"
            else if (it.getFieldOption() == 2)
                entryMode = "Optional"
            else if (it.getFieldOption() == 4)
                entryMode = "Display-only"
            else
                entryMode = "System"
            //Set FieldType
            def fieldType = it.getClass().getSimpleName().toString()
            log.debug "read field definition" + it
            //Create field
            def myField = new FieldDetails(name:it.getName(), fieldId:it.getFieldID(), type:fieldType, entryMode: entryMode, fieldLimit:it.getFieldLimit())
            //Remove qualifier from fieldlimits due to
            if (myField.fieldLimit?.getClass()?.getName().equals("com.bmc.arsys.api.TableFieldLimit"))
                myField.fieldLimit.qualifier = null
            //Assign field to List
            allFields.add(myField)
            //Get Valuemappings if Selectionfield
            if (myField.getType().equals("SelectionField")) {
                def myValues = new TreeMap()
                if (myValues != null) {
                    def arValues = it.getFieldLimit().getValues()
                    arValues.each {
                        myValues[it.getEnumItemNumber()] = it.getEnumItemName()
                    }
                }
                myField.setValueMapping(myValues)
            }
        }
        log.debug "load schema definition - done"
        def myCache = new FieldCache(schema:schema, cacheDate:new Date(), fields:allFields)

        //Initialize Objects if not found
        if (serverCache == null)
            serverCache = new HashMap()
        if (userCache == null)
            userCache = new HashMap()
        userCache[schema] = myCache
        serverCache[context.getUser()] = userCache
        cache[context.getServer()] = serverCache
        return allFields
    }

    /**
     * @param context The AR System Context to work with
     * @param schema The schema to query
     * @param query The query itself. If empty it will be set to '1'!=$NULL$
     * @param fisrtRow The first record to return. Counting starts with 0
     * @param maxRows The number of rows to return. 0 = all entries
     * @return returns all records as HashMap
     */
    def queryForm(ARServerUser context, String schema, String query, Boolean returnFieldNames, Boolean translateSelectionFields, int firstEntry, int maxEntries, Boolean showDisplayOnlyFields, Boolean cacheResults, int cacheTime, String dateFormat, String sortString) {
        return queryForm(context, schema, query, returnFieldNames, translateSelectionFields, firstEntry, maxEntries, showDisplayOnlyFields, cacheResults, cacheTime, dateFormat, sortString);
    }


    /**
     * @param context The AR System Context to work with
     * @param schema The schema to query
     * @param query The query itself. If empty it will be set to '1'!=$NULL$
     * @param fisrtRow The first record to return. Counting starts with 0
     * @param maxRows The number of rows to return. 0 = all entries
     * @return returns all records as HashMap
     */
    def queryForm(ARServerUser context, String schema, String query, Boolean returnFieldNames, Boolean translateSelectionFields, int firstEntry, int maxEntries, Boolean showDisplayOnlyFields, Boolean cacheResults, int cacheTime, ArrayList fields, String dateFormat, String sortString) {
        log.debug "Cache results " + cacheResults

        def userCache = null
        def schemaCache = null;

        def formFields = getFields(context, schema)
        def dataFields = ["CharacterField", "CurrencyField", "DateOnlyField", "DateTimeField", "DecimalField", "DiaryField", "IntegerField", "RealField", "SelectionField", "TimeOnlyField"]
        log.debug "Query form " + schema + " with " + query
        if (query == null || query == '')
            query = "'1' != \$NULL\$"
        def allRecords = []
        QualifierInfo qual = context.parseQualification(schema, query);

        def sortInfoList = new ArrayList();
        //Prepare SortInfo
        if (sortString != null) {
            sortInfoList = UtilService.getSortInfo(sortString, formFields);
        }

        //log.debug "Qualifier: " + qual.toString();

        def fieldIds = new ArrayList();

        if (fields != null) {
            //fieldIds = fields
            formFields.each { field ->
                fields.each { givenField ->
                    givenField = givenField.trim()
                    //loop given fields and compare Name and ID
                    if (field.fieldId.toString() == givenField || field.name.equalsIgnoreCase(givenField))
                        fieldIds.add(field.fieldId);
                }
            }
        } else {
            formFields.each { field ->
                if (dataFields.contains(field.type)) {
                    if (field.entryMode != "Display-only" && field.fieldId != 15)
                        fieldIds.add(field.fieldId)
                    else if (field.entryMode == "Display-only" && showDisplayOnlyFields == true)
                        fieldIds.add(field.fieldId)
                }
            }
        }


        //List<EntryListInfo> eListInfos = context.getListEntry(schema, qual, firstEntry, maxEntries, null, null, false, null);
        def records = context.getListEntryObjects(schema, qual, firstEntry, maxEntries, sortInfoList, fieldIds as int[] , false, null)

        records.each { record ->
            def recordData = convertRecord(record, formFields, dateFormat, translateSelectionFields, returnFieldNames);
            allRecords.add(recordData);
        }
        log.debug "Query returned " + allRecords.size() + " records "
        return allRecords
    }

    def convertRecord(record, formFields, dateFormat, translateSelectionFields, returnFieldNames) {
        def sdf = new SimpleDateFormat(dateFormat)
        def recordData = [:]
        //for (EntryListInfo eListInfo : eListInfos) {
        //Entry record = ${it} //context.getEntry(schema,eListInfo.getEntryID(), null);
        def myRecordValues = [:]
        for (Integer i : record.keySet()) {
            def myField
            formFields.each {
                if (it.fieldId == i)
                    myField = it
            }

            def recordValue = null
            //Check for Valuemappings if Selection field

            //log.debug(record.get(i).getValue().toString().toString())
            //log.debug(record.get(i).getValue().getClass())
            //log.debug(myField.getFieldId())
            if (record.get(i).getValue() == null)
                recordValue = null
            else if (myField.getType().equals("SelectionField")) {
                //log.error "Error " + record.get(i).getValue().toString();
                //log.error "Error " + record.get(i).getValue();
                if (translateSelectionFields)
                    recordValue = myField.valueMapping[Integer.parseInt(record.get(i).getValue().toString().toString())]
                else
                    recordValue = record.get(i).getValue()
            } else if (myField.getType().equals("DateTimeField") && record.get(i).getValue().getClass().getSimpleName().toString().equals("Timestamp")) {
                //Handle TimeStamp
                //recordValue = record.get(i).getValue().toDate().toString()
                recordValue = sdf.format(record.get(i).getValue().toDate());
            } else if (record.get(i).getValue() != null && record.get(i).getValue().getClass().getSimpleName().toString().equals("DiaryListValue")) {
                def diaryValues = new ArrayList()
                record.get(i).getValue().each { diaryItem ->
                    def myDiaryItem = new SimpleDiaryItem(
                            createDate:sdf.format(diaryItem.getTimestamp().toDate()),
                            text:diaryItem.getText(),
                            user:diaryItem.getUser()
                    )
                    diaryValues.add(myDiaryItem)
                }
                recordValue = diaryValues
            } else if (record.get(i).getValue() != null && myField.getFieldId() == 15) {
                if (translateSelectionFields)
                    recordValue = UtilService.convertStatusHistoryValue(record.get(i).getValue().toString(), new ArrayList(formFields.get(7).valueMapping.values()))
                else
                    recordValue = UtilService.convertStatusHistoryValue(record.get(i).getValue().toString(), new ArrayList(formFields.get(7).valueMapping.keySet()))
            }
            else if (record.get(i).getValue() != null && record.get(i).getValue().getClass().getSimpleName().toString().equals("CurrencyValue")) {
                recordValue = new SimpleCurrencyValue(
                        conversionDate:sdf.format(record.get(i).getValue().getConversionDate().toDate()),
                        value:record.get(i).getValue().getValue(),
                        currencyCode:record.get(i).getValue().getCurrencyCode()
                )
            } else if (myField.getType().equals("DecimalField") || myField.getType().equals("IntegerField") || myField.getType().equals("RealField")) {
                recordValue = record.get(i).getValue()
            } else if (myField.getType().equals("AttachmentField")) {
                def attachment = record.get(i).getValue()
                def attachmentValue = new HashMap()
                attachmentValue["URL"] = grailsLinkGenerator.link([controller: 'home', absolute:true]) + "/" + context.getServer() +  "/" + schema +"/getAttachment/" + record.getEntryId() + "/${i}"
                attachmentValue["Name"] = attachment.getName()
                recordValue = attachmentValue
            }
            else
                recordValue = record.get(i).getValue().toString().toString()

            if (returnFieldNames)
                myRecordValues[myField.name] = recordValue
            else
                myRecordValues[i] = recordValue
        }
        recordData['id'] = record.get(1).toString()
        recordData['values'] = myRecordValues
        return recordData;
    }

    /**
     * @param context The AR System Context to work with
     * @param schema The schema to query
     * @param query The query itself. If empty it will be set to '1'!=$NULL$
     * @param fisrtRow The first record to return. Counting starts with 0
     * @param maxRows The number of rows to return. 0 = all entries
     * @return returns all records as HashMap
     */
    def deleteEntries(ARServerUser context, String schema, String query, int firstEntry, int maxEntries) {
        def formFields = getFields(context, schema)
        if (query == null || query == '')
            query = "'1' != \$NULL\$"
        log.debug "Query " + schema + " with " + query
        def allEntries = new TreeMap();
        QualifierInfo qual = context.parseQualification(schema, query);
        List<EntryListInfo> eListInfos = context.getListEntry(schema, qual, firstEntry, maxEntries, null, null, false, null);
        for (EntryListInfo eListInfo : eListInfos) {
            try {
                context.deleteEntry(schema, eListInfo.getEntryID(), 0)
                allEntries.put(eListInfo.getEntryID(), "success")
            } catch (Exception e) {
                log.error("handle exception" + e.getCause().toString())
                e.printStackTrace()
                log.error e.getMessage()
                allEntries.put(eListInfo.getEntryID(), e.getMessage().toString())
            }
        }

        return allEntries
    }



    /**
     * @param context The AR Server Context to use
     * @return a list of all forms on the server
     */
    def getForms(ARServerUser context, boolean details) {
        def forms
        if (!details) {
            forms = context.getListForm()
            Collections.sort(forms);
        } else {
            def formCriteria = new FormCriteria();
            //formCriteria.setPropertiesToRetrieve(FormCriteria.DEFAULT_VUI | FormCriteria.SCHEMA_TYPE)
            formCriteria.setRetrieveAll(true)
            def formDetails = context.getListFormObjects(0,
                    Constants.AR_LIST_SCHEMA_ALL | Constants.AR_HIDDEN_INCREMENT,
                    null,
                    null,
                    formCriteria);
            forms = new ArrayList()
            //log.error("hui" + formDetails)
            formDetails.each {
                def form = new HashMap();
                form['name'] = it.getName()
                def formType = it.getFormType()
                if (formType == 1) {
                    form['formType'] = "Base Form"
                } else if (formType == 2) {
                    form['formType'] = "Join Form"
                } else if (formType == 3) {
                    form['formType'] = "View Form"
                } else if (formType == 4) {
                    form['formType'] = "Display-only Form"
                } else if (formType == 5) {
                    form['formType'] = "Vendor Form"
                } else {
                    form['formType'] = "Unknown Form Type: " + formType
                }
                form['defaultVUI'] = it.getDefaultVUI()
                form['class'] = it.getClass()
                form['lastChangedBy'] = it.getLastChangedBy()
                form['lastUpdateTime'] = it.getLastUpdateTime()?.value
                form['sortInfo'] = it.sortInfo
                if (it.getClass() == com.bmc.arsys.api.JoinForm) {
                    def joinInfo = new HashMap()
                    def joinForm = (com.bmc.arsys.api.JoinForm) it;
                    joinInfo['memberA'] = joinForm.getMemberA();
                    joinInfo['memberB'] = joinForm.getMemberB();
                    joinInfo['joinQualification'] = joinForm.getJoinQualification().toString();
                    joinInfo['joinOption'] = joinForm.getJoinOption();
                    form['joinInfo'] = joinInfo;
                }
                forms.add(form)
            }
        }
        return forms
    }

    def	getServerConfig(ARServerUser context) {
        //int[] serverValue = new int[2]
        //serverValue[0] = Constants.AR_SERVER_INFO_DBCONF
        //serverValue[1] = Constants.AR_SERVER_INFO_DB_TYPE
        def serverConfigValues = new HashMap()
        serverConfigValues[Constants.AR_SERVER_INFO_DB_TYPE] = 'AR_SERVER_INFO_DB_TYPE'
        serverConfigValues[Constants.AR_SERVER_INFO_DBCONF] = 'AR_SERVER_INFO_DBCONF'
        serverConfigValues[Constants.AR_SERVER_INFO_ACTLINK_DIR] = 'AR_SERVER_INFO_ACTLINK_DIR'

        def returnValue = new HashMap()
        //context.getServerInfo(int[][90,97])

        TreeMap serverInfo = context.getServerInfo(serverConfigValues.keySet().toArray())
        serverInfo.keySet().each {
            returnValue[it] = serverInfo.get(it).toString()
        }
        log.debug serverInfo.getClass()
        return returnValue
    }

    def	getServerStatistics(ARServerUser context) {
        int[] serverConfigValues = new int[1]
        serverConfigValues[0] = Constants.AR_SERVER_STAT_CURRENT_USERS

        def returnValue = new HashMap()

        TreeMap serverInfo = context.getServerStatistics(serverConfigValues)
        serverInfo.keySet().each {
            returnValue[it] = serverInfo.get(it).toString()
        }


        log.debug serverInfo.getClass()
        return returnValue
    }

    /**
     * @param context The AR Server Context to use
     * @param schema The AR Form Name
     * @param entryObjects The JSON Objects to update
     * @return returns a TreeMap with all entry ids and any errors
     */
    def updateEntries(ARServerUser context, String schema, entryObject, boolean useMerge, String dateFormat, boolean translateSelectionFields) {
        int multiMatchOption = 2;
        def recordId = entryObject['id']
        def values = entryObject['values']
        def query = entryObject['query']
        int mergeOptions = entryObject['mergeOptions']?.toInteger() ?: 1028;
        log.debug("mergeOp: "+ mergeOptions)
        def formFields = getFields(context, schema)

        def returnValue = [:]
        if (entryObject['multiMatchOption'] != null) {
            try {
                multiMatchOption = entryObject['multiMatchOption'];
                if (multiMatchOption < 0 || multiMatchOption > 2) {
                    log.error('MultimatchOption: ' + multiMatchOption + ' not valid. Fallback to 2');
                    multiMatchOption = 2;
                }
            } catch (e) {
                log.error("Can't covert MultiMatchOption to int", e);
            }
        }
        //loadfieldlist
        def myEntries = []
        int[] fields = [1,2]
        if (query != null) {
            QualifierInfo qual = context.parseQualification(schema, query);
            def sortInfoList = new ArrayList();
            myEntries = context.getListEntryObjects(schema, qual, 0, 0, sortInfoList, fields , false, null)
        } else {
            def myEntry = context.getEntry(schema, recordId, fields)
            myEntries.push(myEntry)
        }

        log.debug("Entries found for update: " + myEntries.size())

        log.debug("MultimatchOption: " + multiMatchOption)
        def fieldCache = getFields(context, schema)

        def updateResults = [];

        //handle MultiMatch
        if (multiMatchOption == 0 && myEntries.size() > 1) {
            def myResult = [:];
            myResult['message'] = 'error';
            myResult['details'] = '' + myEntries.size() + ' Entries found. Multiple Match error.'
            updateResults.push(myResult)
            return updateResults;
        } else if (multiMatchOption == 1 && myEntries.size() > 1) {
            //Take only first record
            myEntries = [myEntries[0]];
        }

        myEntries.each {
            def myEntry = it;
            def myRecordId = it.get(1).getValue();

            myEntry = UtilService.setEntry(myEntry, values, fieldCache)
            try {
                def entryResult = updateEntryInternal(context, schema, myEntry, myRecordId, mergeOptions, useMerge)
                //log.error('done ' + entryResult);
                def myResult = [:];
                myResult['message'] = 'success';
                //log.error('done ' + myResult);
                //myResult['entry'] = convertRecord(myEntry, formFields, dateFormat, true, true);
                //Return Entry the same way it was send.
                myResult['entry'] = myEntry;
                updateResults.push(myResult)
            } catch (error) {
                def myResult = [:];
                myResult['message'] = 'error';
                try {
                    myResult['entry'] = convertRecord(myEntry, formFields, dateFormat, translateSelectionFields, true);
                } catch (errorConvert) {
                    log.error("Cannot convert record " + errorConvert)
                }
                myResult['details'] = error
                updateResults.push(myResult)
            }
        }
        //Save entry

        log.debug("Results: " + updateResults)

        return updateResults
    }

    /**
     * @param context The AR Server Context to use
     * @param schema The AR Form Name
     * @param myEntry The Entry to update
     * @param recordId The id of the record
     * @param mergeOptions
     * @param useMerge
     * @return returns a TreeMap with all entry ids and any errors
     */
    def updateEntryInternal(ARServerUser context, String schema, Entry myEntry, String recordId, mergeOptions, Boolean useMerge) {
        if (useMerge == true) {
            log.debug('Use merge for update.');
            context.mergeEntry(schema, myEntry, mergeOptions);
        } else {
            context.setEntry(schema, recordId, myEntry, new Timestamp(), 0);
        }
    }

    /**
     * @param context The AR Server Context to use
     * @param schema The AR Form Name
     * @param entryObjects The JSON Objects to create
     * @return returns a TreeMap with all entry ids and any errors
     */
    //def createEntries(ARServerUser context, String schema, org.codehaus.groovy.grails.web.json.JSONObject entryObjects) {
    def createEntries(ARServerUser context, String schema, entryObjects) {
        //loadfieldlist
        def fieldCache = getFields(context, schema)

        def allEntries = new TreeMap();
        entryObjects.keySet().each {
            try {
                def myEntry = new Entry()
                myEntry = UtilService.setEntry(myEntry, entryObjects.get(it), fieldCache)
                def requestId = context.createEntry(schema, myEntry)
                allEntries.put(it, requestId)
            } catch (Exception e) {
                e.printStackTrace()
                log.error e.getMessage()
                allEntries.put(it, e.getCause().toString() + " - " + e.getMessage().toString())
            }
        }
        return allEntries
    }

    def setAttachment(context, schema, entryId, fieldId, fileName, uploadedFile) {
        int[] fields = [1,2]
        def myEntry = context.getEntry(schema, entryId, fields)
        def attachmentValue = new AttachmentValue()
        attachmentValue.setName(fileName)
        attachmentValue.setValue(uploadedFile.getBytes())
        myEntry.putAt(fieldId, new Value(attachmentValue))
        //try save
        context.setEntry(schema, entryId, myEntry, new Timestamp(), 0)
        def allEntries = new HashMap()
        allEntries.put(entryId, "success")
        return allEntries
    }

    def setRpcQueue(context, params) {
        if (params && params.rpcQueue) {
            int rpcQueue = Integer.parseInt(params.rpcQueue)
            log.debug('Use RPC Queue', rpcQueue)
            context.usePrivateRpcQueue(rpcQueue)
        }
    }
}
