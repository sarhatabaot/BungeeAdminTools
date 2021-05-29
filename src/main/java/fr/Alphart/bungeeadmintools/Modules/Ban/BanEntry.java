package fr.alphart.bungeeadmintools.modules.ban;

import java.sql.Timestamp;

public record BanEntry(String entity, String server, String reason,
					   String staff, Timestamp beginDate, Timestamp endDate,
					   Timestamp unbanDate, String unbanReason, String unbanStaff,
					   boolean active) {
}