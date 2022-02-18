package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.CreateNetwork;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@CommandLine.Command(name = "findPersonsOnLinks", description = "Find persons, trips in the selected Plans of a Population that traverse one or more of a set of Link Ids, " + "typically provided in a CSV file.")
public class FindPersonTripLegTraversingCorridor implements Callable<Integer>, PersonAlgorithm {

	private static final String[] HEADERS = new String[]{"personId", "trip_id", "time"};
	List<PersonTripLegEntry> result = new ArrayList<>();
	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT_PLANS", description = "Input plans file")
	private Path plans;
	@CommandLine.Parameters(arity = "1", paramLabel = "LINK_IDS", description = "Input CSV with link Ids")
	private Path corridor;
	@CommandLine.Parameters(arity = "1", paramLabel = "OUTPUT_CSV", description = "Output CSV path")
	private Path output;

	private Map<Id<Link>, Integer> linkCorridors;

	public static void main(String[] args) {
		System.exit(new CommandLine(new FindPersonTripLegTraversingCorridor()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		linkCorridors = CreateNetwork.readLinkCorridors(corridor);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
		reader.addAlgorithm(this);
		reader.readFile(plans.toString());
		write();
		return 0;
	}

	@Override
	public void run(Person person) {
		AtomicInteger tripId = new AtomicInteger();
		AtomicReference<Double> lastTime = new AtomicReference<>(0d);
		person.getSelectedPlan().getPlanElements().forEach(planElement -> {
			if (planElement instanceof Activity) {
				lastTime.set(((Activity) planElement).getEndTime().orElse(lastTime.get()));
				if (!((Activity) planElement).getType().contains("interaction"))
					tripId.getAndIncrement();
			}
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					if(route.getLinkIds().size()==0)
						return;
					Stream<Boolean> booleanStream = route.getLinkIds().stream().map(linkId -> linkCorridors.containsKey(linkId));
					boolean aBoolean = booleanStream.reduce((b1, b2) -> b1 || b2).get();
					if (aBoolean) {
						result.add(new PersonTripLegEntry(person.getId(), tripId.get(), leg.getDepartureTime().orElse(lastTime.get())));
					} else if (linkCorridors.containsKey(route.getEndLinkId())) {
						result.add(new PersonTripLegEntry(person.getId(), tripId.get(), leg.getDepartureTime().orElse(lastTime.get())));
					} else if (linkCorridors.containsKey(route.getEndLinkId())) {
						result.add(new PersonTripLegEntry(person.getId(), tripId.get(), leg.getDepartureTime().orElse(lastTime.get())));
					}


				}
			}
		});
	}

	public void write() throws IOException {
		FileWriter out = new FileWriter(output.toString());
		try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(HEADERS))) {
			for (PersonTripLegEntry p : result) {
				printer.printRecord(p.personId, p.trip_id, p.time);
			}
			printer.flush();
		}
	}

	private class PersonTripLegEntry {
		final String personId;
		final String trip_id;
		final double time;


		private PersonTripLegEntry(Id<Person> personId, int trip_id, double time) {
			this.personId = personId.toString();
			this.trip_id = this.personId + "_" + trip_id;
			this.time = time;
		}
	}
}
