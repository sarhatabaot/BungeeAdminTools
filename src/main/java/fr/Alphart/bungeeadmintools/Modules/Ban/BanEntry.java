package fr.alphart.bungeeadmintools.modules.ban;

import java.sql.Timestamp;
import java.util.Objects;

public final class BanEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp beginDate;
	private final Timestamp endDate;
	private final Timestamp unbanDate;
	private final String unbanReason;
	private final String unbanStaff;
	private final boolean active;

	public BanEntry(String entity, String server, String reason,
					String staff, Timestamp beginDate, Timestamp endDate,
					Timestamp unbanDate, String unbanReason, String unbanStaff,
					boolean active) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.unbanDate = unbanDate;
		this.unbanReason = unbanReason;
		this.unbanStaff = unbanStaff;
		this.active = active;
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

	public Timestamp beginDate() {
		return beginDate;
	}

	public Timestamp endDate() {
		return endDate;
	}

	public Timestamp unbanDate() {
		return unbanDate;
	}

	public String unbanReason() {
		return unbanReason;
	}

	public String unbanStaff() {
		return unbanStaff;
	}

	public boolean active() {
		return active;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (BanEntry) obj;
		return Objects.equals(this.entity, that.entity) &&
				Objects.equals(this.server, that.server) &&
				Objects.equals(this.reason, that.reason) &&
				Objects.equals(this.staff, that.staff) &&
				Objects.equals(this.beginDate, that.beginDate) &&
				Objects.equals(this.endDate, that.endDate) &&
				Objects.equals(this.unbanDate, that.unbanDate) &&
				Objects.equals(this.unbanReason, that.unbanReason) &&
				Objects.equals(this.unbanStaff, that.unbanStaff) &&
				this.active == that.active;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active);
	}

	@Override
	public String toString() {
		return "BanEntry[" +
				"entity=" + entity + ", " +
				"server=" + server + ", " +
				"reason=" + reason + ", " +
				"staff=" + staff + ", " +
				"beginDate=" + beginDate + ", " +
				"endDate=" + endDate + ", " +
				"unbanDate=" + unbanDate + ", " +
				"unbanReason=" + unbanReason + ", " +
				"unbanStaff=" + unbanStaff + ", " +
				"active=" + active + ']';
	}

}