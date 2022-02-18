package org.matsim.analysis;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

public class NetworkLinksToCsv {
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(args[1]);
		try (bufferedWriter) {
			bufferedWriter.write("link,cap,lanes\n");
			network.getLinks().values().forEach(link -> {
				if (!link.getAllowedModes().contains(TransportMode.pt))
					try {
//						bufferedWriter.write(link.getId() + "," + Math.min(link.getCapacity(), 6000d)+","+(link.getAttributes().getAttribute("type")!=null?link.getAttributes().getAttribute("type").toString():"ZZZ") + "\n");
						bufferedWriter.write(link.getId() + "," + Math.min(link.getCapacity(), 6000d)+","+link.getNumberOfLanes() +
								"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
			});
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
