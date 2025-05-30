package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AffectedAgentsTravelDistanceComparison {

	public static void main(String[] args) throws IOException {
		// == 1. read network ===
		Network network = loadNetwork("input/v3.1/kelheim-v3.0-network-with-pt.xml.gz");

		// === 2. Read the list of affected agents ===
		Set<Id<Person>> affectedPersons = loadAffectedPersons("output/directly_affected_agents.txt");

		// === 3. Before policy ===
		System.out.println("Analyze distance traveled before policy import...");
		Map<Id<Person>, Double> beforeDistances = analyzeTravelDistances(
			"output/before-policy-import/before-policy-import-run-id.output_events.xml.gz", affectedPersons, network);

		// === 4. after policy ===
		System.out.println("Analyze the distance traveled after the policy...");
		Map<Id<Person>, Double> afterDistances = analyzeTravelDistances(
			"output/after-policy-import/after-policy-import-run-id.output_events.xml.gz", affectedPersons, network);

		// === 5. sout ===
		System.out.println("=== Change in distance traveled (in meters) ===");
		for (Id<Person> personId : affectedPersons) {
			double before = beforeDistances.getOrDefault(personId, 0.0);
			double after = afterDistances.getOrDefault(personId, 0.0);
			double diff = after - before;
			String change = diff > 0 ? "↑" : diff < 0 ? "↓" : "=";
			System.out.printf("%s: before=%.1f, after=%.1f, diff=%.1f %s%n",
				personId, before, after, diff, change);
		}
	}

	private static Network loadNetwork(String networkFile) {
		Network network = org.matsim.core.network.NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		return network;
	}

	private static Set<Id<Person>> loadAffectedPersons(String filePath) throws IOException {
		Set<Id<Person>> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					ids.add(Id.createPersonId(line.trim()));
				}
			}
		}
		return ids;
	}

	private static Map<Id<Person>, Double> analyzeTravelDistances(String eventsFile, Set<Id<Person>> affectedPersons, Network network) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TravelDistanceHandler handler = new TravelDistanceHandler(affectedPersons, network);
		eventsManager.addHandler(handler);
		EventsUtils.readEvents(eventsManager, eventsFile);
		return handler.getTotalDistances();
	}

	private static Network throwMissingNetwork() {
		throw new RuntimeException("network.xml.gz file is not found, make sure the path is correct.");
	}

	static class TravelDistanceHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {
		private final Set<Id<Person>> affectedPersons;
		private final Network network;
		private final Map<Id, Id<Person>> vehicleToPerson = new HashMap<>();
		private final Map<Id<Person>, Double> totalDistances = new HashMap<>();

		TravelDistanceHandler(Set<Id<Person>> affectedPersons, Network network) {
			this.affectedPersons = affectedPersons;
			this.network = network;
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			vehicleToPerson.put(event.getVehicleId(), event.getPersonId());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id personId = vehicleToPerson.get(event.getVehicleId());
			if (personId != null && affectedPersons.contains(personId)) {
				Link link = network.getLinks().get(event.getLinkId());
				if (link != null) {
					double length = link.getLength();
					totalDistances.merge(personId, length, Double::sum);
				}
			}
		}

		public Map<Id<Person>, Double> getTotalDistances() {
			return totalDistances;
		}
	}
}

