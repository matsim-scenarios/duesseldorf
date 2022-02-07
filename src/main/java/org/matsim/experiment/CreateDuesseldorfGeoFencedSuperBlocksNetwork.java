package org.matsim.experiment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.prepare.CreateNetwork;
import org.matsim.prepare.ExtractMinimalConnectedNetwork;
import org.matsim.run.RunDuesseldorfScenario;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains a simple main method script to create an experiemental network where the interior of Duesseldorf has
 * high-capacity (2x), high speed (60kph+) trunks, med-high (1.5x) capacity, slower moving (45kph on primary and
 * secondary, 30kph on tertiary) arterials, and very slow (15kph), low capacity (0.6x) residential roads.
 */
public class CreateDuesseldorfGeoFencedSuperBlocksNetwork {
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		// want to mark only links appearing inside Duesseldorf boundary
		performNetworkSuperBlockConversion(new File(args[1]).toPath(), network);

		new NetworkWriter(network).write(args[2]);
	}

	public static void performNetworkSuperBlockConversion(Path shapeFilePath, Network network) {
		ShpOptions shpOptions = new ShpOptions(shapeFilePath, RunDuesseldorfScenario.COORDINATE_SYSTEM, Charset.defaultCharset());
		network.getLinks().values().forEach(link -> link.getAttributes().putAttribute("keepLink", false));
		ExtractMinimalConnectedNetwork.networkSpatialJoinToBoundaryPolygon(network, shpOptions);

		Map<Id<Link>, Integer> trunksAndMotorways = new HashMap<>();
		Map<Id<Link>, Integer> arterials = new HashMap<>();
		Map<Id<Link>, Integer> residentials = new HashMap<>();

		network.getLinks().values().forEach(link -> {
			if ((boolean) link.getAttributes().getAttribute("keepLink")) {
				Object type = link.getAttributes().getAttribute("type");
				if (type != null) {
					String t = type.toString();
					if (t.contains("motorway") || t.contains("trunk")) {
						if (link.getFreespeed() < 60 / 3.6)
							link.setFreespeed(60 / 3.6);
						trunksAndMotorways.put(link.getId(), t.contains("trunk") ? 1 : 2);
						return;
					}
					if (t.contains("primary") || t.contains("secondary")) {
						link.setFreespeed(45 / 3.6);
						arterials.put(link.getId(), t.contains("primary") ? 1 : 2);
						return;
					}
					if (t.contains("tertiary")) {
						link.setFreespeed(30 / 3.6);
						arterials.put(link.getId(), 3);
						return;
					}
					link.setFreespeed(15 / 3.6);
					residentials.put(link.getId(), 1);
				}
			}
		});

		CreateNetwork.reduceLinkLanesAndMultiplyPerLaneCapacity(network, trunksAndMotorways, 2d, 0);
		CreateNetwork.reduceLinkLanesAndMultiplyPerLaneCapacity(network, arterials, 1.5d, 0);
		CreateNetwork.reduceLinkLanesAndMultiplyPerLaneCapacity(network, residentials, 0.6d, 0);
	}
}
