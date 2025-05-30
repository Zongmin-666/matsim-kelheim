package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class AffectedAgentsRouteChangeComparison {

	public static void main(String[] args) throws Exception {
		// === Step 1: Load affected agents ===
		Set<String> affectedAgentIds = loadAffectedAgents("output/directly_affected_agents.txt");

		// == Step 2: Analyze before policy ==
		System.out.println("Analyzing BEFORE policy...");
		Map<String, List<String>> beforeRoutes = extractRoutes(
			"output/before-policy-import/before-policy-import-run-id.output_events.xml.gz", affectedAgentIds);

		// === Step 3: Analyze after policy ===
		System.out.println("Analyzing AFTER policy...");
		Map<String, List<String>> afterRoutes = extractRoutes(
			"output/after-policy-import/after-policy-import-run-id.output_events.xml.gz", affectedAgentIds);

		// === Step 4: Compare paths ===
		int changed = 0;
		for (String agentId : affectedAgentIds) {
			List<String> before = beforeRoutes.getOrDefault(agentId, List.of());
			List<String> after = afterRoutes.getOrDefault(agentId, List.of());

			if (!before.equals(after)) {
				System.out.println("Agent " + agentId + " path changed.");
				System.out.println("  Before: " + before);
				System.out.println("  After : " + after);
				changed++;
			}
		}

		System.out.println("Number of affected agents:" + affectedAgentIds.size());
		System.out.println("Number of agents whose paths have changed: " + changed);
	}

	private static Set<String> loadAffectedAgents(String filePath) throws Exception {
		Set<String> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				ids.add(line.trim());
			}
		}
		return ids;
	}

	private static Map<String, List<String>> extractRoutes(String eventsFile, Set<String> targetAgents) {
		EventsManager manager = EventsUtils.createEventsManager();
		RouteReconstructionHandler handler = new RouteReconstructionHandler(targetAgents);
		manager.addHandler(handler);
		EventsUtils.readEvents(manager, eventsFile);
		return handler.getAgentRoutes();
	}

	static class RouteReconstructionHandler implements LinkEnterEventHandler {

		private final Set<String> targetAgents;
		private final Map<String, List<String>> agentRoutes = new HashMap<>();

		RouteReconstructionHandler(Set<String> targetAgents) {
			this.targetAgents = targetAgents;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			String vehicleId = event.getVehicleId().toString();
			String personId = vehicleId.replace("veh_", "");

			if (targetAgents.contains(personId)) {
				agentRoutes.computeIfAbsent(personId, k -> new ArrayList<>()).add(event.getLinkId().toString());
			}
		}

		public Map<String, List<String>> getAgentRoutes() {
			return agentRoutes;
		}
	}
}


