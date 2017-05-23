/*
 * Copyright 2009-2017 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package net.nikr.eve.jeveasset.io.esi;

import com.beimin.eveapi.model.shared.Blueprint;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.nikr.eve.jeveasset.data.Citadel;
import net.nikr.eve.jeveasset.data.MyLocation;
import net.nikr.eve.jeveasset.data.api.OwnerType;
import net.nikr.eve.jeveasset.data.esi.EsiOwner;
import net.nikr.eve.jeveasset.gui.dialogs.update.UpdateTask;
import net.nikr.eve.jeveasset.gui.tabs.assets.MyAsset;
import net.nikr.eve.jeveasset.gui.tabs.contracts.MyContract;
import net.nikr.eve.jeveasset.gui.tabs.jobs.MyIndustryJob;
import net.nikr.eve.jeveasset.gui.tabs.orders.MyMarketOrder;
import net.nikr.eve.jeveasset.io.online.CitadelGetter;
import net.nikr.eve.jeveasset.io.shared.ApiIdConverter;
import net.troja.eve.esi.ApiClient;
import net.troja.eve.esi.ApiException;
import net.troja.eve.esi.auth.SsoScopes;
import net.troja.eve.esi.model.StructureResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EsiStructuresGetter extends AbstractEsiGetter {

	private static final Logger LOG = LoggerFactory.getLogger(EsiStructuresGetter.class);

	private final Map<Long, Set<Long>> map = new HashMap<Long, Set<Long>>();

	public void load(UpdateTask updateTask, List<EsiOwner> owners, List<OwnerType> typeOwners) {
		map.clear();
		for (EsiOwner owner : owners) {
			map.put(owner.getOwnerID(), new HashSet<Long>());
		}
		for (OwnerType owner : typeOwners) {
			Set<Long> locationIDs = map.get(owner.getOwnerID());
			if (locationIDs != null) {
				getIDs(locationIDs, owner);
			}
		}
		super.load(updateTask, owners);
	}

	@Override
	protected ApiClient get(EsiOwner owner) throws ApiException {
		List<Citadel> citadels = new ArrayList<>();
		for (Long locationID : map.get(owner.getOwnerID())) {
			try {
				StructureResponse response = getUniverseApiAuth().getUniverseStructuresStructureId(locationID, DATASOURCE, null, null, null);
				citadels.add(ApiIdConverter.getCitadel(response, locationID));
			} catch (ApiException ex) {
				if (ex.getCode() != 403 && ex.getCode() != 404) { //Ignore 403: Forbidden and 404: Structure not found
					throw ex;
				} else {
					LOG.info("Failed to find locationID: " + locationID);
				}
			}
		}
		CitadelGetter.set(citadels);
		return getUniverseApiAuth().getApiClient();
	}

	@Override
	protected void setNextUpdate(EsiOwner owner, Date date) {
		owner.setStructuresNextUpdate(date);
	}

	@Override
	protected Date getNextUpdate(EsiOwner owner) {
		return owner.getStructuresNextUpdate();
	}

	@Override
	protected boolean inScope(EsiOwner owner) {
		return owner.getScopes().contains(SsoScopes.ESI_UNIVERSE_READ_STRUCTURES_V1);
	}

	@Override
	protected String getTaskName() {
		return "Structures";
	}

	private void getIDs(Set<Long> locationIDs, OwnerType owner) {
		for (MyAsset asset : owner.getAssets()) {
			MyLocation location = asset.getLocation();
			if (location.isEmpty() || location.isUserLocation() || location.isCitadel()) {
				locationIDs.add(location.getLocationID());
			}
		}
		for (Blueprint blueprint : owner.getBlueprints().values()) {
			MyLocation location = ApiIdConverter.getLocation(blueprint.getLocationID());
			if (location.isEmpty() || location.isUserLocation() || location.isCitadel()) {
				locationIDs.add(location.getLocationID());
			}
		}
		for (MyContract contract : owner.getContracts().keySet()) {
			MyLocation locationEnd = contract.getEndStation();
			if (locationEnd.isEmpty() || locationEnd.isUserLocation() || locationEnd.isCitadel()) {
				locationIDs.add(locationEnd.getLocationID());
			}
			MyLocation locationStart = contract.getStartStation();
			if (locationStart.isEmpty() || locationStart.isUserLocation() || locationStart.isCitadel()) {
				locationIDs.add(locationStart.getLocationID());
			}
		}
		for (MyIndustryJob industryJob : owner.getIndustryJobs()) {
			MyLocation locationEnd = industryJob.getLocation();
			if (locationEnd.isEmpty() || locationEnd.isUserLocation() || locationEnd.isCitadel()) {
				locationIDs.add(locationEnd.getLocationID());
			}
		}
		for (MyMarketOrder marketOrder : owner.getMarketOrders()) {
			MyLocation locationEnd = marketOrder.getLocation();
			if (locationEnd.isEmpty() || locationEnd.isUserLocation() || locationEnd.isCitadel()) {
				locationIDs.add(locationEnd.getLocationID());
			}
		}
	}
}
