package fr.alphart.bungeeadmintools.modules.mute;

import java.sql.Timestamp;
import java.util.Objects;

public final class MuteEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp beginDate;
	private final Timestamp endDate;
	private final Timestamp unmuteDate;
	private final String unmuteReason;
	private final String unmuteStaff;
	private final boolean active;

	public MuteEntry(String entity, String server, String reason,
					 String staff, Timestamp beginDate, Timestamp endDate,
					 Timestamp unmuteDate, String unmuteReason, String unmuteStaff,
					 boolean active) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.unmuteDate = unmuteDate;
		this.unmuteReason = unmuteReason;
		this.unmuteStaff = unmuteStaff;
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

	public Timestamp unmuteDate() {
		return unmuteDate;
	}

	public String unmuteReason() {
		return unmuteReason;
	}

	public String unmuteStaff() {
		return unmuteStaff;
	}

	public boolean active() {
		return active;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (MuteEntry) obj;
		return Objects.equals(this.entity, that.entity) &&
				Objects.equals(this.server, that.server) &&
				Objects.equals(this.reason, that.reason) &&
				Objects.equals(this.staff, that.staff) &&
				Objects.equals(this.beginDate, that.beginDate) &&
				Objects.equals(this.endDate, that.endDate) &&
				Objects.equals(this.unmuteDate, that.unmuteDate) &&
				Objects.equals(this.unmuteReason, that.unmuteReason) &&
				Objects.equals(this.unmuteStaff, that.unmuteStaff) &&
				this.active == that.active;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entity, server, reason, staff, beginDate, endDate, unmuteDate, unmuteReason, unmuteStaff, active);
	}

	@Override
	public String toString() {
		return "MuteEntry[" +
				"entity=" + entity + ", " +
				"server=" + server + ", " +
				"reason=" + reason + ", " +
				"staff=" + staff + ", " +
				"beginDate=" + beginDate + ", " +
				"endDate=" + endDate + ", " +
				"unmuteDate=" + unmuteDate + ", " +
				"unmuteReason=" + unmuteReason + ", " +
				"unmuteStaff=" + unmuteStaff + ", " +
				"active=" + active + ']';
	}

}
