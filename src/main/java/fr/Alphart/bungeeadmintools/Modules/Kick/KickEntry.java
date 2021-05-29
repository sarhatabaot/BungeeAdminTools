package fr.alphart.bungeeadmintools.modules.kick;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record KickEntry(String entity, String server, String reason,
						String staff, Timestamp date) {
}