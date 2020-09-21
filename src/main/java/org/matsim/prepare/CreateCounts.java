package org.matsim.prepare;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateCounts {

	private static final Logger logger = Logger.getLogger(CreateCounts.class);

	@CommandLine.Option(names = {"-p", "--project"}, description = "Input csv federal road count file", defaultValue = "/Users/friedrich/SVN/public-svn/matsim/scenarios/countries/de/duesseldorf/")
	private static String project;

	@CommandLine.Option(names = {"-f", "--federalRoad"}, description = "Input csv federal road count file", defaultValue = "duesseldorf-v1.0/original-data/long-term-counts-federal-road.txt")
	private static String federalRoad;

	@CommandLine.Option(names = {"-h", "--highway"}, description = "Input csv highway count file", defaultValue = "duesseldorf-v1.0/original-data/long-term-counts-highway.txt")
	private static String highway;

	@CommandLine.Option(names = {"-m", "--mapping"}, description = "Input csv mapping file", defaultValue = "duesseldorf-v1.0/original-data/countstation-osm-node-matching.csv")
	private static String mapping;

	@CommandLine.Option(names = {"-n", "--network"}, description = "Input csv mapping file", defaultValue = "duesseldorf-v1.0/input/duesseldorf-v1.0-network-with-pt.xml.gz")
	private static String network;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Output xml file", defaultValue = "duesseldorf-v1.0/matsim-input-files/counts-duesseldorf.xml.gz")
	private static String output;

	public static void main(String[] args) throws IOException {

		logger.info("Program starts!");

		var test = new CommandLine(new CreateCounts()).execute(args);

		if (!testInputFiles()) {
			throw new RuntimeException("NO!");
		}

		var matching = new NodeMatcher();
		var matchingResult = matching.parseNodeMatching(project + mapping);

		logger.info("Finished with matching nodes.");

		var longTerm = new GetCountData();
		var longTermResult = longTerm.countData(project + federalRoad, project + highway, network, matchingResult);

		var counts = new Counts<Link>();
		counts.setYear(2018);

		for (var data : longTermResult.entrySet()) {

			GetCountData.CountingData value = data.getValue();

			var count = counts.createAndAddCount(Id.createLinkId(value.getLinkId()), data.getValue().getStationId());

			for (var hour : data.getValue().getResult().keySet()) {

				count.createVolume(Integer.parseInt(StringUtils.stripStart(hour, "0")), data.getValue().getResult().get(hour));

			}
			logger.info("Create new count object! Station ID: " + value.getStationId() + "  Link ID: " + value.getLinkId() + "  Counts: " + value.getResult());
		}

		new CountsWriter(counts).write(project + output);

		logger.info("Finish!!!");

	}

	private static boolean testInputFiles() {
		return Files.exists(Paths.get(project + mapping)) && Files.exists(Paths.get(project + highway)) && Files.exists(Paths.get(project + federalRoad)) && Files.exists(Paths.get(network)) && Files.exists(Paths.get(project + output));
	}

}