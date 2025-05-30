package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class DirectlyAffectedAgentsAnalysis {

	public static void main(String[] args) {
		// 1. Creating a Event Manager
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// 2. Create and register handler
		AffectedAgentsHandler handler = new AffectedAgentsHandler();
		eventsManager.addHandler(handler);

		// 3. Read events file
		String eventsFile = "output/before-policy-import/before-policy-import-run-id.output_events.xml.gz";
		EventsUtils.readEvents(eventsManager, eventsFile);

		// 4. result sout
		Set<Id<Vehicle>> affectedAgents = handler.getAffectedAgentIds();

		System.out.println("The total number of agents directly affected: " + affectedAgents.size());
		for (Id<Vehicle> agentId : affectedAgents) {
			System.out.println(agentId);
		}
	}

	// Internal class: handles LinkLeaveEvent, logs affected agents
	static class AffectedAgentsHandler implements LinkLeaveEventHandler {

		private final Set<String> affectedLinkIds = Set.of(
			"704457557", "-27392253",
			"829998750", "27392395#1",
			"127264833#1", "26801184",
			"27392248", "27392235#0",
			"27392297#0", "27392395#0",
			"27392347","27392316",
			"27392397","27392235#1",
			"26838017#1","26838017#2",
			"27408682","27392297#1",
			"830819754","27391765#4",
			"883724370","27391765#3",
			"788760788","788760789",
			"788760787#1","788760786",
			"27443742#0","27392444#0",
			"27392444#1","27443742#1",
			"830820073","27391765#1",
			"27100093","27392519#0",
			"27392519#1","27443742#2",
			"27391727#0","27391727#1",
			"27391765#0","921612255",
			"27404404#1","27392445",
			"27404404#0","620648779",
			"96590904","961410932",
			"8599767","495011550",
			"26948571#1","26948571#0",
			"405739722","27408695"

		);

		private final Set<Id<Vehicle>> affectedAgentIds = new HashSet<>();

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (affectedLinkIds.contains(event.getLinkId().toString())) {
				affectedAgentIds.add(event.getVehicleId());
			}
		}

		public Set<Id<Vehicle>> getAffectedAgentIds() {
			return affectedAgentIds;
		}
	}
}
