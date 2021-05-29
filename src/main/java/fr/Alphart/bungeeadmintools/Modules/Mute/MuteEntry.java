package fr.alphart.bungeeadmintools.modules.mute;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record MuteEntry(String entity, String server, String reason,
						String staff, Timestamp beginDate, Timestamp endDate,
						Timestamp unmuteDate, String unmuteReason, String unmuteStaff,
						boolean active) {
}
