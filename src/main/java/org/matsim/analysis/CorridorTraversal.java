package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.prepare.CreateNetwork;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@CommandLine.Command(name = "linkLeaveCounts", description = "Get link leave event counts to double-check the linkStats file")

public class CorridorTraversal implements Callable<Integer>, LinkLeaveEventHandler {
	Map<Id<Link>, AtomicInteger> vehiclesOnLink = new TreeMap<>();
	@CommandLine.Option(names = "--corridor", description = "File with corridor links.")
	private Path corridor;
	@CommandLine.Option(names = "--network", description = "Network file.")
	private Path networkPath;

	private Network network;
	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT_EVENTS", description = "Input events file")
	private Path events;
	@CommandLine.Parameters(arity = "1", paramLabel = "OUTPUT_CSV", description = "Output CSV path")
	private Path output;

	public static void main(String[] args) throws IOException {

		System.exit(new CommandLine(new CorridorTraversal()).execute(args));

	}



	@Override
	public Integer call() throws Exception {
		if (corridor != null) {
			CreateNetwork.readLinkCorridors(corridor).keySet().forEach(link -> vehiclesOnLink.put(link, new AtomicInteger(0)));
		} else {
			network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(networkPath.toString());
			network.getLinks().keySet().forEach(link -> vehiclesOnLink.put(link, new AtomicInteger(0)));
		}
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(this);
		new EventsReaderXMLv1(eventsManager).readFile(events.toString());
		write(output.toString());
		return 0;
	}

	@Override
	public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		AtomicInteger leaveCount = vehiclesOnLink.get(linkLeaveEvent.getLinkId());
		if (leaveCount != null)
			leaveCount.incrementAndGet();
		else {
			if (corridor == null) {
				leaveCount = new AtomicInteger(1);
				vehiclesOnLink.put(linkLeaveEvent.getLinkId(), leaveCount);
			}
		}
	}

	public void write(String fileName) throws IOException {
		FileWriter out = new FileWriter(fileName);
		try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(new String[]{"id", "volume"}))) {
			for (Map.Entry<Id<Link>, AtomicInteger> entry : vehiclesOnLink.entrySet()) {
				printer.printRecord(entry.getKey().toString(), entry.getValue().get());
			}
			printer.flush();
		}
	}

}
