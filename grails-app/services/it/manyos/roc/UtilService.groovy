package it.manyos.roc

import com.bmc.arsys.api.CurrencyValue
import com.bmc.arsys.api.StatusHistoryValue
import com.bmc.arsys.api.Value
import com.sun.xml.internal.ws.client.sei.ValueSetter.ReturnValue
import org.grails.web.util.WebUtils;

import java.text.SimpleDateFormat

//import org.codehaus.groovy.grails.web.util.WebUtils

class UtilService {
	
    def getUsername() {
		def authHeader = WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getHeader('Authorization')
		if (authHeader) {
			def usernamePassword = new String(authHeader.split(' ')[1].decodeBase64())
			def username = usernamePassword.split(':')[0]
			return username
		}
    }
	
	def String getPassword() {
		def authHeader = WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getHeader('Authorization')
		if (authHeader) {
			def usernamePassword = new String(authHeader.split(' ')[1].decodeBase64())
			def password = usernamePassword.split(':')[1]
			return password
		}
	}

    //TODO Fieldcache Auf ArrayList umbauen
	def int getFieldId(fieldCache, fieldName) {
		int myId = -1
		fieldCache.keySet().each {
			def myField = fieldCache.get(it)
			if (myField.getName().equalsIgnoreCase(fieldName)) {
				myId =  it.intValue()
			}
		}
		return myId
	}
	
	def Value getFieldValue(fieldType, JSONValue) {
		SimpleDateFormat dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
		def value = new Value()
		/*if (JSONValue.equals("\$NULL\$"))
			return new Value(null)
		log.debug "Field: " + fieldType + " " + JSONValue*/
		
		if (fieldType.equals("DateTimeField")) {
			value = new Value(dateParser.parse(JSONValue).getTime() / 1000)
		} else if (fieldType.equals("CurrencyField")) {
			def myValue = new CurrencyValue()
			myValue.setValue(JSONValue.value)
			myValue.setCurrencyCode(JSONValue.currencyCode)
			myValue.setConversionDate((long)dateParser.parse(JSONValue.conversionDate).getTime() / 1000)
			value = new Value(myValue)
		} else
			value = new Value(JSONValue)
			
		return value
	}
	
	/**
	 * @param fieldCache The cache object with the field definition
	 * @param field The name or id of the field
	 * @return the type of the field
	 */
	def String getFieldType(fieldCache, field) {
		//log.debug field
		
		String fieldType = null
		try {
			def fieldId = Integer.parseInt(field)
			def myField = fieldCache.get(fieldId)
			//log.debug myField
			fieldType = myField.type
		} catch (Exception e) {
			fieldCache.keySet().each {
				def myField = fieldCache.get(it)
				if (myField.getName().equalsIgnoreCase(field)) {
					fieldType =  myField.type
				}
			}
		}
		return fieldType
	}
	
	def setEntry(myEntry, JSONEntry, fieldCache) {
		JSONEntry.keySet().each { sourceField ->
			def fieldId = -1
			try {
				fieldId = Integer.parseInt(sourceField)
			} catch (Exception e1) {
				fieldId = getFieldId(fieldCache, sourceField)
			}
			log.error(fieldId)
			if (fieldId == -1)
				throw new Exception("Field: "+ sourceField + " not found on form ")
			
			def fieldType = getFieldType(fieldCache, sourceField)
			
			def value = getFieldValue(fieldType, JSONEntry.get(sourceField))
				
			myEntry.put(fieldId, value)
		}
		return myEntry
	}
	
	def convertStatusHistoryValue(String statusHistory, ArrayList statusValues) {
		def returnValue = new HashMap() 
		StatusHistoryValue shVal = StatusHistoryValue.decode(statusHistory);
		//log.debug shVal.size()
		//log.debug statusValues
		int counter = -1
		shVal?.each { statusHistoryItem ->
			counter++
			if (statusHistoryItem != null) {
				def myValue = new HashMap()
				myValue["User"] = statusHistoryItem.getUser()
				myValue["Date"] = statusHistoryItem.getTimestamp().toDate().toString()
				/*log.debug statusHistoryItem
				log.debug counter
				log.debug statusValues.get(counter)*/
				returnValue[statusValues.get(counter)] = myValue
			} else {
				returnValue[statusValues.get(counter)] = null
			}
		}
		//log.debug returnValue
		return returnValue
	}
}
