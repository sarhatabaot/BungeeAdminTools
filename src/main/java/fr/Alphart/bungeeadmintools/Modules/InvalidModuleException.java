package fr.alphart.bungeeadmintools.modules;

import lombok.Getter;

public class InvalidModuleException extends Exception {
	private static final long serialVersionUID = 1L;
	@Getter
	private final String message;
	
	public InvalidModuleException(final String message){
		this.message = message;
	}
}
