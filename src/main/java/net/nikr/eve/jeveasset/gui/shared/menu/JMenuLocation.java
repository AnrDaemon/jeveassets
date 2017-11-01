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
package net.nikr.eve.jeveasset.gui.shared.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.sde.MyLocation;
import net.nikr.eve.jeveasset.data.settings.Citadel;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.components.JSelectionDialog;
import net.nikr.eve.jeveasset.i18n.GuiShared;
import net.nikr.eve.jeveasset.io.online.CitadelGetter;


public class JMenuLocation<T> extends MenuManager.JAutoMenu<T> {

	private enum MenuLocationAction {
		EDIT,
		CLEAR
	}
	private final JMenuItem jEdit;
	private final JMenuItem jReset;
	private final JSelectionDialog<MyLocation> jLocationDialog;

	private MenuData<T> menuData;

	public JMenuLocation(final Program program) {
		super((GuiShared.get().location()), program);
		setIcon(Images.LOC_LOCATIONS.getIcon());

		ListenerClass listener = new ListenerClass();

		jLocationDialog = new JSelectionDialog<MyLocation>(program);

		jEdit = new JMenuItem(GuiShared.get().itemEdit());
		jEdit.setIcon(Images.EDIT_EDIT.getIcon());
		jEdit.setActionCommand(MenuLocationAction.EDIT.name());
		jEdit.addActionListener(listener);
		add(jEdit);

		addSeparator();

		jReset = new JMenuItem(GuiShared.get().itemDelete());
		jReset.setIcon(Images.EDIT_DELETE.getIcon());
		jReset.setActionCommand(MenuLocationAction.CLEAR.name());
		jReset.addActionListener(listener);
		add(jReset);
	}

	
	
	@Override
	public void setMenuData(MenuData<T> menuData) {
		this.menuData = menuData;
		jEdit.setEnabled(!menuData.getEmptyStations().isEmpty() || !menuData.getUserStations().isEmpty());
		jReset.setEnabled(!menuData.getUserStations().isEmpty());
		
	}

	private class ListenerClass implements ActionListener {
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (MenuLocationAction.CLEAR.name().equals(e.getActionCommand())) {
				if (menuData.getUserStations().size() == 1) { //Single
					MyLocation location = menuData.getUserStations().iterator().next();
					Long locationID = program.getUserLocationSettingsPanel().deleteLocation(location);
					if (locationID != null) {
						CitadelGetter.remove(locationID);
						program.updateLocations(Collections.singleton(location.getLocationID()));
					}
				} else {
					int value = JOptionPane.showConfirmDialog(program.getMainWindow().getFrame(), GuiShared.get().locationClearConfirmAll(menuData.getUserStations().size()), GuiShared.get().locationClear(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (value == JOptionPane.OK_OPTION) { //All
						Set<Long> locationIDs = new HashSet<>();
						for (MyLocation location : menuData.getUserStations()) {
							locationIDs.add(location.getLocationID());
						}
						CitadelGetter.remove(locationIDs);
						program.updateLocations(locationIDs);
					} else { //Single
						MyLocation location = jLocationDialog.show(GuiShared.get().locationID(), menuData.getUserStations());
						Long locationID = program.getUserLocationSettingsPanel().deleteLocation(location);
						if (locationID != null) {
							CitadelGetter.remove(locationID);
							program.updateLocations(Collections.singleton(location.getLocationID()));
						}
					}
				}
			} else if (MenuLocationAction.EDIT.name().equals(e.getActionCommand())) {
				Set<MyLocation> emptyAndUserStations = new HashSet<MyLocation>();
				emptyAndUserStations.addAll(menuData.getEmptyStations());
				emptyAndUserStations.addAll(menuData.getUserStations());
				MyLocation renameLocation = jLocationDialog.show(GuiShared.get().locationID(), emptyAndUserStations);
				Citadel citadel = program.getUserLocationSettingsPanel().editLocation(renameLocation);
				if (citadel != null) {
					CitadelGetter.set(citadel);
					program.updateLocations(Collections.singleton(renameLocation.getLocationID()));
				}
			}
		}
	}
}
