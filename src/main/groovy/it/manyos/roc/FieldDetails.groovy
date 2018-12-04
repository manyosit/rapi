package it.manyos.roc

import com.bmc.arsys.api.FieldLimit

class FieldDetails {

	String name
	
	Integer fieldId
	
	String type
	
	TreeMap valueMapping
	
	String entryMode
	
	FieldLimit fieldLimit
	
	def getValueMapping() {
		return valueMapping
	}

	@Override
	String toString() {
		return '' + name + ' (' + fieldId+')'
	}
}