package org.matsim.analysis.simWrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class TravelTime {

	final Id<Link> linkId;
	final double expectedTravelTime;
	double travelTime = 0.0;
	int count = 0;

	TravelTime(Link link) {
		this.linkId = link.getId();
		this.expectedTravelTime = link.getLength()/link.getFreespeed();
	}

	public void addTime(double travelTime) {
		this.travelTime += travelTime;
		count++;
	}

	public double getAvgTravelTime() {
		if (count != 0) {
			return  travelTime/count;
		}
		return 0;
	}

}
