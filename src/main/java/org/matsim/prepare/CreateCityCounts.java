package org.matsim.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.lang3.StringUtils.countMatches;

@CommandLine.Command(
		name = "createCityCounts",
		description = "Aggregate and convert counts from inner city"
)
public class CreateCityCounts implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateCityCounts.class);

	private final Set<String> allStations = new HashSet<>();

	private List<String> infoSummary = new ArrayList<>();

	/**
	 * Map station name to link id.
	 */
	private final Map<String, Id<Link>> mapping = new HashMap<>();

	/**
	 * To analyze stations without mapping
	 */
	private final Set<String> noMappingStations = new HashSet<>();
	private String problemMonth;

	@CommandLine.Option(names = {"--mapping"}, description = "Path to map matching csv file",
			defaultValue = "../public-svn/matsim/scenarios/countries/de/duesseldorf/duesseldorf-v1.0/original-data/city-counts-node-matching.csv")
//			defaultValue = "../../svn-projects/duesseldorf-network/duesseldorf-v1.0/original-data/city-counts-node-matching.csv")
	private Path mappingInput;

	@CommandLine.Option(names = {"--input"}, description = "Input folder with zip files",
			defaultValue = "../../shared-svn/komodnext/data/counts")
//			defaultValue = "../../svn-projects/komodnext/data/counts")
	private Path input;

	@CommandLine.Option(names = {"--output"}, description = "Output counts.xml.gz",
			defaultValue = "../public-svn/matsim/scenarios/countries/de/duesseldorf/duesseldorf-v1.0/matsim-input-files/counts-city.xml.gz")
//			defaultValue = "../../svn-projects/duesseldorf-network/duesseldorf-v1.0/matsim-input-files/counts-city_2div.xml.gz")
	private String output;

	@CommandLine.Option(names = {"--summaryOutput"}, description = "Short summary file summary.txt",
			defaultValue = "../public-svn/matsim/scenarios/countries/de/duesseldorf/duesseldorf-v1.0/matsim-input-files/counts-city-log-summary.txt")
//			defaultValue = "../../svn-projects/duesseldorf-network/duesseldorf-v1.0/matsim-input-files/counts-city-log-summary_2div.txt")
	private String summaryOutput;

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateCityCounts()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input {} does not exist.", input);
			return 1;
		}

		if (!Files.exists(mappingInput)) {
			log.error("Mapping {} does not exist.", mappingInput);
			return 1;
		}

		infoSummary.add("###### Start log ######");

		readMapping(mappingInput);

		// map of month to counts
		Map<String, Counts<Link>> collect = Files.list(input)
				.filter(f -> f.getFileName().toString().endsWith(".zip"))
				.collect(Collectors.toList()) // need to collect first
				.parallelStream()
				.map(this::readCounts)
				.collect(Collectors.toMap(Counts::getName, Function.identity()));

		for (var counts : collect.entrySet()) {
			log.info("**********************************************************************************************************************************");
			log.info("* Month: " + counts.getKey());

			for (Count<Link> value : counts.getValue().getCounts().values()) {
				log.info("LinkId : {}", value.getId());
				log.info("* Counts per hour: {}", value.getVolumes());
			}

			log.info("**********************************************************************************************************************************");
		}

		Counts<Link> finalCounts = aggregateCounts(collect);
		finalCounts.setYear(2019);

		new CountsWriter(finalCounts).write(output);

		// Count issues/errors and write them into log file
		try{
			FileWriter fileWriter = new FileWriter(summaryOutput);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			// Use following line to delete duplicates (e.g. 12x delete-count-message, one for each month)
			Set<String> summarySet = new HashSet<>(infoSummary); infoSummary.clear(); infoSummary.addAll(summarySet);

			infoSummary = infoSummary.stream().sorted().collect(Collectors.toList());
			for (String msg : infoSummary) {
				writer.write(msg);
				writer.newLine();
			}
			writer.newLine(); writer.write("#### Summary ####"); writer.newLine();
			writer.write("#### "+finalCounts.getCounts().size()+" valid sections has been added to final counts list ####"); writer.newLine();
			List<String> months = Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");
			writer.write("#### "+ countMatches(infoSummary.toString(),"[NO mapping") +" stationIDs were not being considered because there was no mapping for this station ####"); writer.newLine();
			for(String mm : months) {
				writer.write("## " + countMatches(infoSummary.toString(),"("+mm+")") + " stationIDs had no mapping in zip folder "+mm+" ##");
				writer.newLine();
			}
			writer.write("#### "+ countMatches(infoSummary.toString(),"[Missing mapping]") +" stationIDs got mapped by another valid stationID from the same section ####"); writer.newLine();
			for(String mm : months) {
				writer.write("## " + countMatches(infoSummary.toString(),"["+mm+"]") + " stationIDs in "+mm+" got mapped with other mappingIDs ##");
				writer.newLine();
			}
			writer.write("#### " + countMatches(infoSummary.toString(),"all values in 'processed_all_vol'") + " stationIDs were removed from 'allstations' because all their values in 'processed_all_vol' were 0.0 ####"); writer.newLine();
			writer.write("#### " + countMatches(infoSummary.toString(),"Null pointer error doing averageCounts") + " null pointer errors occurred, because stationId or getVolume.getValue resulted 'null'  ####"); writer.newLine();
//			writer.write("###### End log ######");
			writer.close();
		}catch (IOException ee) {
			throw new RuntimeException(ee);
		}
		return 0;
	}

	/**
	 * Reads map matched csv file.
	 */
	private void readMapping(Path mappingInput) throws IOException {

		try (var in = new InputStreamReader(Files.newInputStream(mappingInput), StandardCharsets.UTF_8)) {

			CSVParser csv = new CSVParser(in, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
			for (CSVRecord record : csv) {
				mapping.put(record.get(0), Id.createLinkId(record.get("Link-Id")));		// is this Link-Id or stationId? -> link-Id
			}
		}
	}


	/**
	 * Read one month of count data from for all sensors for zip file.
	 */
	private Counts<Link> readCounts(Path zip) {

//		infoLog.add("#### Enter readCounts ####");
		Counts<Link> counts = new Counts<>();
		Counts<Link> problemCounts = new Counts<>();
		Count<Link> problemCount = problemCounts.createAndAddCount(Id.createLinkId("dummyLink"), "dummyStation");
		counts.setYear(2019);
		String monthNumber = zip.getFileName().toString().split("-")[0];
		counts.setName(monthNumber);

		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {

			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;

				try {
					String stationId = entry.getName().split("_")[2];
					stationId = stationId.substring(0, stationId.length() - 4);
					Id<Link> linkId = null;
//					allStations.add(stationId);  // later, to be sure that every mapped count has a valid refLink (djp)

					if (!mapping.containsKey(stationId)) {
						boolean alternativeMapping = false;
//						log.info("Probably missing mapping for station {} -> try to find section", stationId);
////						infoSummary.add("No mapping for station { "+ stationId + " } from (" + monthNumber + ") - will not be considered for counts");
//						infoSummary.add("Probably missing mapping for station { "+ stationId + " } from (" + monthNumber + ") -> try to find in section " + stationId.substring(0,12));
						for(String mappedStation : mapping.keySet()){
							// find and add alternative link to mapping list
							if(mappedStation.substring(0,12).equals(stationId.substring(0,12))){
								linkId = Id.createLinkId(mapping.get(mappedStation));
//								allStations.add(stationId);
//								Count<Link> count;
//								if (counts.getCounts().containsKey(linkId)) {
//									count = counts.getCount(linkId);
//								} else {
//									count = counts.createAndAddCount(linkId, stationId);
//								}
								mapping.put(stationId, linkId);
								log.warn("Missing mapping for station {} from [{}] takes infos from station {} with ref link {}", stationId, monthNumber, mappedStation, linkId);
								infoSummary.add("[Missing mapping] Station { "+ stationId + " } from ["+monthNumber+"] takes infos from station { " + mappedStation + " } with ref link: " + linkId);
								log.info("Finished alternative reading {}", entry.getName());
								alternativeMapping = true;
								break;
							}
						}
						if(!alternativeMapping) {
//							log.warn("Station {} from [{}] goes as problemCount ", stationId, monthNumber);
//							infoSummary.add("[NO mapping] Station { " + stationId + " } from (" + monthNumber + ") will not be considered for counts");
							// Saved into a Set but actually never used
							noMappingStations.add(stationId);
						}
					}else {
						linkId = Id.createLinkId(mapping.get(stationId));
					}

					if(linkId!= null){
						allStations.add(stationId);

						Count<Link> count;
						if (counts.getCounts().containsKey(linkId)) {
							count = counts.getCount(linkId);
						} else {
							count = counts.createAndAddCount(linkId, stationId);
							count.setCsId(stationId);	// Set stationID also as CsId
						}

						readCsvCounts(in, count);
						log.info("Finished reading {}", entry.getName());
					} else{
						log.info("linkID is null! (No mapping!) Analyze {} ...", stationId);
						problemMonth = monthNumber;
						problemCount.setCsId(stationId);
						readCsvCounts(in, problemCount);
					}
				} catch (ArrayIndexOutOfBoundsException e){
					log.warn("Array error! Could not parse stationId from file {}", entry, e);
					infoSummary.add("[![1]!] Array error or could not find stationId in this file { "+ entry + " }");
				}

			}
		} catch (IOException e) {
			log.error("Could not read zip file {}", zip, e);
			infoSummary.add("[![2]!] Could not read zip file "+ zip +" from /" + monthNumber);
		}

		return counts;
	}

	/**
	 * Read counts from CSV data for one station and aggregate whole month into one count object.
	 *
	 * @param count count object of the link that must be populated with count data
	 */
	private void readCsvCounts(InputStream in, Count<Link> count) throws IOException {

//		infoLog.add("#### Enter readCsvCounts ####");
		boolean isProblemCount = false;
		if(count.getId().toString().contains("dummy")){isProblemCount = true;}

		Map<Integer, List<Double>> tempCountSum = new HashMap<>();

		List<String> holidays2019 = Arrays.asList("01.01.2019", "19.04.2019", "22.04.2019", "01.05.2019",
				"30.05.2019", "10.06.2019", "20.06.2019", "03.10.2019", "01.11.2019", "25.12.2019",
				"26.12.2019");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		List<Integer> weekendDaysList = Arrays.asList(1, 5, 6, 7);		// todo: maybe reason for missing counts?
		Double countMean;
		double sum = 0D;

		InputStreamReader reader = new InputStreamReader(in);
		CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
				.withDelimiter(';')
				.withFirstRecordAsHeader()
		);

		for (CSVRecord record : parser) {
			if (!isWeekend(LocalDate.parse(record.get("Time").split(" ")[0], formatter), weekendDaysList) || !holidays2019.contains(record.get("Time").split(" ")[0])) {

				Integer hour = Integer.parseInt(record.get("Time").split(" ")[1].split(":")[0]);
				double value = Double.parseDouble(record.get("processed_all_vol").replaceAll(",", "."));
				tempCountSum.computeIfAbsent(hour, k -> new ArrayList<>()).add(value);
				sum += value;
			}
		}

		if (sum == 0 && !isProblemCount) {
//			allStations.remove(count.getId().toString());
//			mapping.remove(count.getId());
			allStations.remove(count.getCsLabel());
			infoSummary.add("[Remove] { " + count.getCsLabel() + " } - all values in 'processed_all_vol' = " + sum);

		} else if (sum == 0 && isProblemCount){
			log.warn("Station {} from {} has no mapping and no counts ", count.getCsLabel(), problemMonth);
			infoSummary.add("[NO mapping NO counts] Station { " + count.getCsLabel() + " } from (" + problemMonth + ") will not be considered for counts");
		} else if (isProblemCount){
			log.warn("Station {} from {} has no mapping but has counts! ", count.getCsLabel(), problemMonth);
			infoSummary.add("[NO mapping BUT counts] Station { " + count.getCsLabel() + " } from (" + problemMonth + ") will not be considered for counts");
		} else {
			for (Map.Entry<Integer, List<Double>> meanCounts : tempCountSum.entrySet()) {
				countMean = 0.0;
				for (Double value : meanCounts.getValue()) {
					countMean += value;
				}
				// Average value from "per minute" to "per hour"
				countMean = countMean / (meanCounts.getValue().size());

				int key = meanCounts.getKey() + 1;
				if (count.getVolumes().containsKey(key))
					count.createVolume(key, count.getVolume(key).getValue() + countMean);
				else
					count.createVolume(key, countMean);
			}
		}
	}

	private boolean isWeekend(LocalDate date, List<Integer> weekendDaysList) {
		return weekendDaysList.contains(date.getDayOfWeek().getValue());
	}

	private Counts<Link> aggregateCounts(Map<String, Counts<Link>> collect) {

//		infoSummary.add("#### Enter aggregateCounts ####");

		Counts<Link> counts = new Counts<>();
		double[] averageCounts;
		int[][] contribCounts;
		ArrayList<Count<Link>> sectionList;
		double realSize;
		int validMonths;
		Count<Link> selectedCount;

		// Iterate through allStations to find sections (= station root, char 1-12) and count them together
		for (String name : allStations) {
			Id<Link> refLink = Id.createLinkId(mapping.get(name));
			averageCounts = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
			contribCounts = new int[24][12];
			sectionList = new ArrayList<>();
			try {
				if (!counts.getCounts().containsKey(refLink)) {
					counts.createAndAddCount(refLink, name.substring(0, 12));
					for (String station : allStations) {
						if (station.contains(name.substring(0, 12))) {
							for (int mm = 0; mm < 12; mm++) {
								selectedCount = collect.get(String.format("%02d", mm + 1)).getCount(refLink);
								if(selectedCount.getVolumes().size() != 0) {
									sectionList.add(selectedCount);
									for (int hh = 0; hh < 24; hh++) {
										try {
											// Sum of average counts from all stations belonging to a section, seperated by hour
											averageCounts[hh] = averageCounts[hh] + selectedCount.getVolume(hh + 1).getValue();
											contribCounts[hh][mm]++;
										} catch (NullPointerException e) {
											log.warn("Null Pointer error! Could not do averageCounts at {} in entry {}", hh, selectedCount);
//								Detailed messages (for which hour error occurred)
//								if(!infoLog.contains("Null pointer error doing averageCounts for station { " + name + " } at iter " + i + "/23 [0-23]")) {
//									infoLog.add("Null pointer error doing averageCounts for station { " + name + " } at iter " + i + "/23 [0-23]");
//								}
											if (!infoSummary.contains("[![3]!] Null pointer error doing averageCounts for station { " + name + " } at least once")) {
												infoSummary.add("[![3]!] Null pointer error doing averageCounts for station { " + name + " } at least once");
											}
										}
									}
//									 for troubleshoot
//									 infoSummary.add("ADDED Count " + collect.get(String.format("%02d", mm + 1)).getCount(refLink) + " from " + mm);
								} else if (collect.get(String.format("%02d", mm+1)).getCounts().containsValue(selectedCount)){
									log.info("File {} not found in [{} 2019]", selectedCount, mm+1);
									infoSummary.add("[no file] " + selectedCount.getCsLabel() + " refLink (" + refLink + " not in folder [" + String.format("%02d", mm + 1) + " 2019]");
								} else {
									log.info("Count {} from {} has 0 valid volumes!!", selectedCount, mm+1);
									infoSummary.add("[no.vol=0] " + selectedCount.getCsLabel() + " refLink (" + refLink + ") from " + String.format("%02d", mm + 1));
								}
							}
						}
					}
					realSize = (double) sectionList.size() / 12.;
					log.info("Section " + name.substring(0, 12) + " has " + realSize + " station elements!");
					infoSummary.add("[Avg. Stations per months] in section " + name.substring(0, 12) + " : " + realSize);

					double checksum = 0;
//					log.info("check divisorCounts {}", divisorCounts);
//					infoSummary.add("check divisorCounts [[ "+ divisorCounts +" ]]");
					for (int hh = 0; hh < 24; hh++) {
						validMonths = 0;
						// which months had valid data = months of active measuring
						for(int mm = 0; mm<12; mm++){
							if(contribCounts[hh][mm] != 0) validMonths++;
						}
						if(validMonths != 0) {
							// Final: Yearly average of counts, seperated by hour
							averageCounts[hh] = averageCounts[hh] / validMonths;
							counts.getCount(refLink).createVolume(hh + 1, averageCounts[hh]);
							checksum = checksum + counts.getCount(refLink).getVolume(hh + 1).getValue();
							infoSummary.add("[contrib.info] Sum of Section " + name.substring(0,12) + " at " + String.format("%02d", hh) + ":XX has divisor: " + validMonths + " | And counting stations per month: " + Arrays.toString(contribCounts[hh]));
							log.info("[ {} ] Contributing stations at {}:XX in section {} (divisor: {} ) with averageCounts = {}", contribCounts[hh], hh, name.substring(0, 12), validMonths, Arrays.toString(averageCounts));
						}
					}
					if (checksum == 0) {
						infoSummary.add("[Remove] { " + name + " } - averageCounts = " + Arrays.toString(averageCounts));
						log.info("ID deleted: " + name + " (Link-Id: " + refLink + ")\t" + Arrays.toString(averageCounts));
						counts.getCounts().remove(refLink);

					} else {
						log.info("ID: {} (Link-Id: {})\t{}", name, refLink, Arrays.toString(averageCounts));
						infoSummary.add("[RESULT] Section { " + name.substring(0, 12) + " } has averageCounts = " + Arrays.toString(averageCounts));
					}
					log.info("Total: "+counts.getCounts().size()+" sections counted!");
				}

			}catch (NullPointerException e){
				log.warn("Null Pointer error in entry {} with refLink {} (maybe due to remapping or all_vol = 0)", name, refLink);
			}

		}

		counts.setName("2019");
		return counts;

	}

}
