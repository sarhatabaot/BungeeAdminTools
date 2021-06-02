package fr.alphart.bungeeadmintools.modules.kick;

import java.sql.Timestamp;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class KickEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp date;

	public KickEntry(String entity, String server, String reason,
					 String staff, Timestamp date) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.date = date;
	}

	public String entity() {
		return entity;
	}

	public String server() {
		return server;
	}

	public String reason() {
		return reason;
	}

	public String staff() {
		return staff;
	}

	public Timestamp date() {
		return date;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (KickEntry) obj;
		return Objects.equals(this.entity, that.entity) &&
				Objects.equals(this.server, that.server) &&
				Objects.equals(this.reason, that.reason) &&
				Objects.equals(this.staff, that.staff) &&
				Objects.equals(this.date, that.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(entity, server, reason, staff, date);
	}

	@Override
	public String toString() {
		return "KickEntry[" +
				"entity=" + entity + ", " +
				"server=" + server + ", " +
				"reason=" + reason + ", " +
				"staff=" + staff + ", " +
				"date=" + date + ']';
	}

}