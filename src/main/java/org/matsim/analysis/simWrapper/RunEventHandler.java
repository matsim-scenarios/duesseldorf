package org.matsim.analysis.simWrapper;

import org.apache.commons.csv.CSVFormat;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunEventHandler {

	public static void main(String[] args) {

		var config = ConfigUtils.createConfig();
		config.network().setInputFile("D:/SimWrapper/base/dd.output_network.xml.gz");
		var scenario = ScenarioUtils.loadScenario(config);
		var network = scenario.getNetwork();

		var handler = new Handler(network);
		var manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);
		EventsUtils.readEvents(manager, "D:/SimWrapper/base/dd.output_events.xml.gz");

		var volume = handler.getVolume();
		var volume8 = handler.getVolume8();
		var volume15 = handler.getVolume15();

		try (var writer = Files.newBufferedWriter(Path.of("volumes.csv")); var printer = CSVFormat.DEFAULT.withHeader("linkId", "count", "count8", "count15").print(writer)) {
			for (var entry : volume.entrySet()) {
				var count8 =volume8.get(entry.getKey());
				var count15 =volume15.get(entry.getKey());
				if (count8 == null) {
					count8 = 0;
				}if (count15 == null) {
					count15 = 0;
				}
				printer.printRecord(entry.getKey(), entry.getValue(), count8, count15);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
