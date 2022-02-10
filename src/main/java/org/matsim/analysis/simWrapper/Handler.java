package org.matsim.analysis.simWrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

public class Handler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	Network network;
	private HashMap<Id<Link>, Integer> volume = new HashMap<>();
	private HashMap<Id<Link>, Integer> volume8 = new HashMap<>();
	private HashMap<Id<Link>, Integer> volume15 = new HashMap<>();
	private HashMap<Id<Link>, TravelTime> travelTimeMap = new HashMap<>();
	private HashMap<Id<Vehicle>, Double> tmpTime = new HashMap<>();

	Handler(Network network){
		this.network = network;
	}

	@Override
	public void handleEvent(LinkEnterEvent linkEnterEvent) {
		var linkId = linkEnterEvent.getLinkId();
		var link = network.getLinks().get(linkId);
		var modes = link.getAllowedModes();
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

			if (tmpTime.containsKey(linkEnterEvent.getVehicleId())) {
				tmpTime.put(linkEnterEvent.getVehicleId(), linkEnterEvent.getTime());
			} else {
				System.out.println("Vehicle enters a link without leaving before");
			}

		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		var linkId = linkLeaveEvent.getLinkId();
		var link = network.getLinks().get(linkId);
		if (link.getAllowedModes().contains("car")) {
			if (linkLeaveEvent.getTime() < 8 * 3600) {
				int count = volume8.get(linkId);
				volume8.put(linkId, count - 1);
			}
			if (linkLeaveEvent.getTime() < 15 * 3600) {
				int count = volume15.get(linkId);
				volume15.put(linkId, count - 1);
			}

			if (tmpTime.containsKey(linkLeaveEvent.getVehicleId())) {
				if (travelTimeMap.containsKey(linkId)) {
					var travelTime = travelTimeMap.get(linkId);
					var enterTime = tmpTime.get(linkLeaveEvent.getVehicleId());
					tmpTime.remove(linkLeaveEvent.getVehicleId());
					travelTime.addTime(linkLeaveEvent.getTime() - enterTime);
				} else {
					var travelTime = new TravelTime(link);
					travelTimeMap.put(linkId, travelTime);
					var enterTime = tmpTime.get(linkLeaveEvent.getVehicleId());
					tmpTime.remove(linkLeaveEvent.getVehicleId());
					travelTime.addTime(linkLeaveEvent.getTime() - enterTime);
				}
			} else {
				System.out.println("Vehicle leaves link without entering before");
			}

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

	public HashMap<Id<Link>, TravelTime> getTravelTimeMap() {
		return travelTimeMap;
	}
}
