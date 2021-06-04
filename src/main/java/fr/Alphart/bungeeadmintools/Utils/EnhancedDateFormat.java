package fr.alphart.bungeeadmintools.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import fr.alphart.bungeeadmintools.i18n.I18n;

public class EnhancedDateFormat{
	private final Calendar currDate = Calendar.getInstance();
	private final boolean literalDate;
	private final DateFormat defaultDF;
	private DateFormat tdaDF;
	private DateFormat tmwDF;
	private DateFormat ydaDF;
	
	/**
	 * @param literalDate if it's true, use tda, tmw or yda instead of the default date format
	 */
	public EnhancedDateFormat(final boolean literalDate){
		this.literalDate = literalDate;
		final String at = I18n.formatWithColor("at");
		defaultDF = new SimpleDateFormat("dd-MM-yyyy '" + at + "' HH:mm");
		if(literalDate){
			tdaDF = new SimpleDateFormat("'" + I18n.formatWithColor("today").replace("'", "''") + " " + at + "' HH:mm");
			tmwDF = new SimpleDateFormat("'" + I18n.formatWithColor("tomorrow").replace("'", "''") + " " + at + "' HH:mm");
			ydaDF = new SimpleDateFormat("'" + I18n.formatWithColor("yesterday").replace("'", "''") + " " + at + "' HH:mm");
		}
	}
	
	public String format(final Date date){
		if(literalDate){
			final Calendar calDate = Calendar.getInstance();
			calDate.setTime(date);
			final int dateDoY = calDate.get(Calendar.DAY_OF_YEAR);
			final int currDoY = currDate.get(Calendar.DAY_OF_YEAR);
			
			if(calDate.get(Calendar.YEAR) == currDate.get(Calendar.YEAR)){
				if(dateDoY == currDoY){
					return tdaDF.format(date);
				}else if(dateDoY == currDoY - 1){
					return ydaDF.format(date);
				}else if(dateDoY == currDoY + 1){
					return tmwDF.format(date);
				}
			}
		}

		return defaultDF.format(date);
	}
}