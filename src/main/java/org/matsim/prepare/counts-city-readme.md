# Readme for output summary (.txt) of CreateCityCounts.java
(the summary is at the bottom)

### Station
One sensor device, for example for one direction or one specific lane. <br/>
So called Erfassungsgerät (EG), e.g. `IDe-33-01-B1-MBGH1a, IDe-33-01-B1-MBGH2a, VDe-61-01-B1-MAGV1a, ...`

### Section
Aggregated stations for one cross section, i.e. for both directions and multiple lanes.
They might be still distinguishible by their reference link (refLink), BUT please double check with _**city-counts-node-matching.csv**_ (= map-matching). <br/>
So called Messquerschnitt (MQS), e.g. `IDe-33-01-B1, VDe-61-01-B1, ...` (always `station.substring(0,12)`)
---
Following explanations are in reference to the **_counts-city-log-summary.txt_**, as it gives an effective overview on the general functions and outcomes of CreateCityCount (following order of appearance may differ)

### [Avg. Stations per months]
= (all stations of one section)/12 <br/>
Gives a quick overview, which sections might not have data for the whole year. <br/> Example:  `in section IDe-66-01-B1 : 6.0` <br/>
It means that 72 count-files were found. Usually it means that `6` stations in average delivered 12 times (= all the year) valid data for section `IDe-66-01-B1`. BUT it is better to double-check, see [contrib.info]

### [Missing mapping]
Integrated into counts-city.xml.gz: YES <br/>
Stations, which have no map-matching, but other stations inside the same section have. "Missing mapped"-station takes refLinks from mapped station from the same section -> manual mapping (risk: wrong direction, but does not matter for section count)
<br/> Example:  `Station { IDe-32-06-A1-MAGH2a } from [02] takes infos from station { IDe-32-06-A1-MAGV2a } with ref link: 241243447` <br/>
No mapping for csv `IDe-32-06-A1-MAGH2a` from zip folder `02 2019`, but takes refLink `241243447` from other station `IDe-32-06-A1-MAGV2a` from the same section `IDe-32-06-A1`

### [NO mapping BUT counts]
Integrated into counts-city.xml.gz: NO <br/>
Stations wihtout map-matching and without alternative stations from the same section. Reading csv file shows valid count data! StationIDs are being saved in `Set<String> noMappingStations`, but never used.
<br/> Example:  `Station { IDe-33-02-A1-MBGV } from (01) will not be considered for counts` <br/>
Either station `IDe-33-02-A1-MBGV` nor section `IDe-33-02-A1` from zip-folder `01 2019` have map-matching, but they have valid count data!

### [NO mapping NO counts]
Integrated into counts-city.xml.gz: NO <br/>
Stations wihtout map-matching and without alternative stations from the same section. Reading csv file shows sum of all valid count data = 0! StationIDs are being saved in `Set<String> noMappingStations`, but never used.
<br/> Example:  `Station { VDe-33-02-A1-MBGV } from (09) will not be considered for counts` <br/>
Either station `VDe-33-02-A1-MBGV` nor section `VDe-33-02-A1` from zip-folder `09 2019` have map-matching, plus its valid count data sum is 0!

### [RESULT] 
Final output of yearly average section counts, specified by hour. This will be written into counts-city.xml.gz
<br/> Example:  `Section { IDe-16-03-C1 } has averageCounts = [1617.9459942011174, [...], 2577.6623232162715]` <br/>
Yearly average counts of section `IDe-16-03-C1` with the average counts `[1617.9459942011174, [...], 2577.6623232162715]`, ordered by hour [0,1,2,...,22,23]

### [Remove]
Removed station from ...
<br/> ..1.. `Set<String> allStations`, because reading csv file shows sum of valid count data = 0 <br/>
Example:  `{ VDe-21-01-D1-MCGV1a } - all values in 'processed_all_vol' = 0.0` <br/>

..2.. `Counts<Link> counts`, because it survived ..1.., as other stations from the same section have valid count data. This additional remove avoids empty counts in the output file
<br/> Example:  `{ VDe-21-04-A1-MBGV } - averageCounts = [0.0, 0.0, [...], 0.0]`

### [contrib.info]
Shows how many stations contributed over the year with valid count data in a section for a specific hour, and hence the divisor to calculate the correct average count. If **at least** one station of each month had valid count data, then the average will be calculated by dividing the sum by 12. Hint: Over the year, the same amount of stations has count data for each hour hh:XX. However, this has not to be a general case. So the contribution rate and the average is being calculated for each hour individually.
<br/> Example:  `Sum of Section IDe-12-01-A1 at 00:XX has divisor: 7 | And counting stations per month: [3, 3, 3, 3, 3, 0, 3, 3, 0, 0, 0, 0]` <br/>
From `January` (01 2019) to `May` and from `June` to `July`, `3` stations delivered valid count data to section `IDe-12-01-A1` from `00:00 `to` 00:59`. The sum of these values will be divided by `7`

### [no file]
Reason why a stationID is not being used in the sum of count data of a section: No csv-file was found for the specified month.
<br/> Example:  `IDe-12-01-A1-MAGV1a refLink (41075426#0) not in folder [06 2019]` <br/>
A csv-file with stationID `IDe-12-01-A1-MAGV1a` and refLink `41075426#0` does not exist in folder `06 2019`

### [no.vol=0]
Reason why a stationID is not being used in the sum of count data of a section: The csv-file has no defined volumes for the specified month.
<br/> Example:  `IDe-12-01-A1-MAGV1a refLink (41075426#0) from 06 2019 has 0 valid volumes!` <br/>
A csv-file with stationID `IDe-12-01-A1-MAGV1a` and refLink `41075426#0` in folder `06 2019` has no defined volumes (`nof_volumes = 0`)

### Summary
Provides a summary of valid sections, and stations not being map-matched, being modified manually, removed or causing null pointer errors. Hint: Over 300 stationIDs not being considered include duplicates! It has not to be over 300 different stationIDs. The monthly overview maybe is better for evaluation.
<br/><br/>
Author: DJP | Last review: 2021-01-31