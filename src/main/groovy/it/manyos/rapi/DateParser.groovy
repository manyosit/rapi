package it.manyos.rapi

class DateParser {
	def static parseDate = { 
		str -> Date.parseToStringDate( "EEE MMM dd HH:mm:ss zzz yyyy", str )
	}
}
