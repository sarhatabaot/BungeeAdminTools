package fr.alphart.bungeeadmintools.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class FormatUtils {

	/**
	 * Get the duration between the given timestamp and the current one
	 * 
	 * @param futureTimestamp
	 *            in milliseconds which must be superior to the current
	 *            timestamp
	 * @return readable duration
	 */
	public static String getDuration(final long futureTimestamp) {
		int seconds = (int) ((futureTimestamp - System.currentTimeMillis()) / 1000) + 1;
		Preconditions.checkArgument(seconds > 0,
				"The timestamp passed in parameter must be superior to the current timestamp !");

		final List<String> item = new ArrayList<>();

		int months = 0;
		while (seconds >= 2678400) {
			months++;
			seconds -= 2678400;
		}
		if (months > 0) {
			item.add(months + " months");
		}

		int days = 0;
		while (seconds >= 86400) {
			days++;
			seconds -= 86400;
		}
		if (days > 0) {
			item.add(days + " days");
		}

		int hours = 0;
		while (seconds >= 3600) {
			hours++;
			seconds -= 3600;
		}
		if (hours > 0) {
			item.add(hours + " hours");
		}

		int mins = 0;
		while (seconds >= 60) {
			mins++;
			seconds -= 60;
		}
		if (mins > 0) {
			item.add(mins + " mins");
		}

		if (seconds > 0) {
			item.add(seconds + " secs");
		}

		return Joiner.on(", ").join(item);
	}

	public static List<BaseComponent[]> formatNewLine(final String message) {
		final String[] strMessageArray = message.split("\n");
		final List<BaseComponent[]> bsList = new ArrayList<>();
		for (final String s : strMessageArray) {
			bsList.add(TextComponent.fromLegacyText(s));
		}
		return bsList;
	}
}