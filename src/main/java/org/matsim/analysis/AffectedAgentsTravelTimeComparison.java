package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AffectedAgentsTravelTimeComparison {

	public static void main(String[] args) throws IOException {
		// == 1. Read the list of affected agents ===
		Set<Id<Person>> affectedPersons = loadAffectedPersons("output/directly_affected_agents.txt");

		// === 2. Read the events before policy ===
		System.out.println("Read the events before policy...");
		Map<Id<Person>, Double> beforeTravelTimes = analyzeTravelTimes(
			"output/before-policy-import/before-policy-import-run-id.output_events.xml.gz", affectedPersons);

		// === 3. Read the events after policy ===
		System.out.println("Read the events after policy...");
		Map<Id<Person>, Double> afterTravelTimes = analyzeTravelTimes(
			"output/after-policy-import/after-policy-import-run-id.output_events.xml.gz", affectedPersons);

		// === 4. Comparison Result Output ===
		System.out.println("=== Change in travel time (in seconds) ===");
		for (Id<Person> personId : affectedPersons) {
			double before = beforeTravelTimes.getOrDefault(personId, 0.0);
			double after = afterTravelTimes.getOrDefault(personId, 0.0);
			double diff = after - before;
			String change = diff > 0 ? "↑" : diff < 0 ? "↓" : "=";
			System.out.printf("%s: before=%.1f, after=%.1f, diff=%.1f %s%n",
				personId, before, after, diff, change);
		}
	}

	private static Map<Id<Person>, Double> analyzeTravelTimes(String eventsFile, Set<Id<Person>> personIds) {
		EventsManager manager = EventsUtils.createEventsManager();
		TravelTimeHandler handler = new TravelTimeHandler(personIds);
		manager.addHandler(handler);
		EventsUtils.readEvents(manager, eventsFile);
		return handler.getTotalTravelTimes();
	}

	private static Set<Id<Person>> loadAffectedPersons(String filePath) throws IOException {
		Set<Id<Person>> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					ids.add(Id.create(line.trim(), Person.class));
				}
			}
		}
		return ids;
	}

	static class TravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
		private final Set<Id<Person>> targetPersons;
		private final Map<Id<Person>, Double> departureTimes = new HashMap<>();
		private final Map<Id<Person>, Double> totalTravelTimes = new HashMap<>();

		TravelTimeHandler(Set<Id<Person>> targetPersons) {
			this.targetPersons = targetPersons;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			Id<Person> personId = event.getPersonId();
			if (targetPersons.contains(personId)) {
				departureTimes.put(personId, event.getTime());
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			Id<Person> personId = event.getPersonId();
			if (targetPersons.contains(personId) && departureTimes.containsKey(personId)) {
				double depTime = departureTimes.remove(personId);
				double travelTime = event.getTime() - depTime;
				totalTravelTimes.merge(personId, travelTime, Double::sum);
			}
		}

		public Map<Id<Person>, Double> getTotalTravelTimes() {
			return totalTravelTimes;
		}
	}
}
