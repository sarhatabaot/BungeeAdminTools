package fr.alphart.bungeeadmintools.modules.comment;

import fr.alphart.bungeeadmintools.modules.core.Core;

import java.util.Calendar;
import java.util.TimeZone;


public class CommentEntry {
	private Calendar localTime = Calendar.getInstance(TimeZone.getDefault());
	private final int id;
	private final String entity;
	private final String content;
	private final String author;
	private final Type type;

	public CommentEntry(final int id, final String entity, final String note, final String author, final Type type, final long date) {
		this.id = id;
		this.entity = entity;
		this.content = note;
		this.author = author;
		this.type = type;
		localTime.setTimeInMillis(date);
	}

	public int getID(){
		return id;
	}
	
	public String getEntity() {
		return entity;
	}

	public String getContent() {
		return content;
	}

	public String getAuthor() {
		return author;
	}

	public Type getType() {
		return type;
	}

	public Calendar getDate() {
		return localTime;
	}

	public String getFormattedDate(){
		return Core.defaultDF.format(localTime.getTime());
	}
	
	public enum Type{
		NOTE,
		WARNING;
	}
}