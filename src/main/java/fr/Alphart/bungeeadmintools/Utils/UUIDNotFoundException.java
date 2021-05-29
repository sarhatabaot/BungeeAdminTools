package fr.alphart.bungeeadmintools.utils;


import java.io.Serial;

public class UUIDNotFoundException extends RuntimeException{
	@Serial
	private static final long serialVersionUID = 1L;
	private final String player;
	
	public UUIDNotFoundException(String player){
		this.player = player;
	}
	
	public String getInvolvedPlayer(){
		return player;
	}
}
