package fr.alphart.bungeeadmintools.modules;

import lombok.Getter;

import java.io.Serial;

public class InvalidModuleException extends Exception {
	@Serial
	private static final long serialVersionUID = 1L;
	@Getter
	private final String message;
	
	public InvalidModuleException(final String message){
		this.message = message;
	}
}
