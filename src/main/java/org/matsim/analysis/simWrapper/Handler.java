package org.matsim.analysis.simWrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.HashMap;

public class Handler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	Network network;
	HashMap<Id<Link>, Integer> volume = new HashMap<>();
	HashMap<Id<Link>, Integer> volume8 = new HashMap<>();
	HashMap<Id<Link>, Integer> volume15 = new HashMap<>();

	Handler(Network network){
		this.network = network;
	}

	@Override
	public void handleEvent(LinkEnterEvent linkEnterEvent) {
		var linkId = linkEnterEvent.getLinkId();
		var link = network.getLinks().get(linkId);
		var modes = link.getAllowedModes();
		try {
			if (modes.contains("car")) {
				if (volume.containsKey(linkId)) {
					int count = volume.get(linkId);
					volume.put(linkId, count + 1);
				} else {
					volume.put(linkId, 1);
				}
				if (linkEnterEvent.getTime() < 8 * 3600) {
					if (volume8.containsKey(linkId)) {
						int count = volume8.get(linkId);
						volume8.put(linkId, count + 1);
					} else {
						volume8.put(linkId, 1);
					}
				}
				if (linkEnterEvent.getTime() < 8 * 3600) {
					if (volume15.containsKey(linkId)) {
						int count = volume15.get(linkId);
						volume15.put(linkId, count + 1);
					} else {
						volume15.put(linkId, 1);
					}
				}
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		var linkId = linkLeaveEvent.getLinkId();
		var link = network.getLinks().get(linkId);
		try {
			if (link.getAllowedModes().contains("car")) {
				if (linkLeaveEvent.getTime() < 8 * 3600) {
					int count = volume8.get(linkId);
					volume8.put(linkId, count - 1);
				}
				if (linkLeaveEvent.getTime() < 15 * 3600) {
					int count = volume15.get(linkId);
					volume15.put(linkId, count - 1);
				}
			}
		}catch (Exception e) {

		}
	}

	public HashMap<Id<Link>, Integer> getVolume() {
		return volume;
	}

	public HashMap<Id<Link>, Integer> getVolume8() {
		return volume8;
	}

	public HashMap<Id<Link>, Integer> getVolume15() {
		return volume15;
	}
}
