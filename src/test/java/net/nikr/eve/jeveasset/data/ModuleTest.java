/*
 * Copyright 2009, 2010, 2011, 2012 Contributors (see credits.txt)
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

package net.nikr.eve.jeveasset.data;

import java.util.ArrayList;
import java.util.List;
import net.nikr.eve.jeveasset.data.Module.FlagType;
import static org.junit.Assert.assertTrue;
import org.junit.*;

public class ModuleTest {

	public ModuleTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of FlagType enum, of class Module.
	 */
	@Test
	public void testFlags() {
		ModuleSettings settings = new ModuleSettings();
		List<ItemFlag> flags = new ArrayList<ItemFlag>(settings.getItemFlags().values());

		for (FlagType type : FlagType.values()) {
			if (type == FlagType.TOTAL_VALUE) {
				continue;
			}
			if (type == FlagType.OTHER) {
				continue;
			}
			boolean found = false;
			for (ItemFlag flag : flags) {
				System.out.println(flag.getFlagName());
				if (flag.getFlagName().contains(type.getFlag())) {
					found = true;
					break;
				}
			}
			assertTrue(type.name() + " flag value (" + type.getFlag() + ") is no longer valid", found);
		}
	}
}