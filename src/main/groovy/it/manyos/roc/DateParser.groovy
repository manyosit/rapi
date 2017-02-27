package it.manyos.roc

class DateParser {
	def static parseDate = { 
		str -> Date.parseToStringDate( "EEE MMM dd HH:mm:ss zzz yyyy", str )
	}
}
