/*
 * Copyright 2009-2020 Contributors (see credits.txt)
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
package net.nikr.eve.jeveasset.data.settings;

import net.nikr.eve.jeveasset.data.sde.MyLocation;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.SplashUpdater;
import net.nikr.eve.jeveasset.data.api.raw.RawMarketOrder.MarketOrderRange;
import net.nikr.eve.jeveasset.data.settings.ContractPriceManager.ContractPriceSettings;
import net.nikr.eve.jeveasset.data.settings.tag.Tag;
import net.nikr.eve.jeveasset.data.settings.tag.TagID;
import net.nikr.eve.jeveasset.data.settings.tag.Tags;
import net.nikr.eve.jeveasset.gui.shared.CaseInsensitiveComparator;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor.ResizeMode;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor.SimpleColumn;
import net.nikr.eve.jeveasset.gui.shared.table.View;
import net.nikr.eve.jeveasset.gui.tabs.orders.Outbid;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewGroup;
import net.nikr.eve.jeveasset.gui.tabs.stockpile.Stockpile;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerDate;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerNote;
import net.nikr.eve.jeveasset.io.local.SettingsReader;
import net.nikr.eve.jeveasset.io.local.SettingsWriter;
import net.nikr.eve.jeveasset.io.shared.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

	private static final String PATH_ASSET_ADDED = "data" + File.separator + "added.json";
	private static final String PATH_ASSET_ADDED_DATABASE = "data" + File.separator + "addedsql.db";
	private static final String PATH_TRACKER_DATA = "data" + File.separator + "tracker.json";
	private static final String PATH_CONTRACT_PRICES = "data" + File.separator + "contract_prices.json";
	private static final String PATH_SETTINGS = "data" + File.separator + "settings.xml";
	private static final String PATH_ITEMS = "data" + File.separator + "items.xml";
	private static final String PATH_ITEMS_UPDATES = "data" + File.separator + "items_updates.xml";
	private static final String PATH_JUMPS = "data" + File.separator + "jumps.xml";
	private static final String PATH_LOCATIONS = "data" + File.separator + "locations.xml";
	private static final String PATH_FLAGS = "data" + File.separator + "flags.xml";
	private static final String PATH_PRICE_DATA = "data" + File.separator + "pricedata.dat";
	private static final String PATH_ASSETS = "data" + File.separator + "assets.xml";
	private static final String PATH_CONQUERABLE_STATIONS = "data" + File.separator + "conquerable_stations.xml";
	private static final String PATH_CITADEL = "data" + File.separator + "citadel.xml";
	private static final String PATH_README = "readme.txt";
	private static final String PATH_LICENSE = "license.txt";
	private static final String PATH_CREDITS = "credits.txt";
	private static final String PATH_CHANGELOG = "changelog.txt";
	
	private static final String PATH_PROFILES = "profiles";
	private static final String PATH_DATA = "data";

	public static enum SettingFlag {
		FLAG_IGNORE_SECURE_CONTAINERS,
		FLAG_FILTER_ON_ENTER,
		FLAG_REPROCESS_COLORS,
		FLAG_INCLUDE_SELL_ORDERS,
		FLAG_INCLUDE_BUY_ORDERS,
		FLAG_INCLUDE_SELL_CONTRACTS,
		FLAG_INCLUDE_BUY_CONTRACTS,
		FLAG_INCLUDE_MANUFACTURING,
		FLAG_HIGHLIGHT_SELECTED_ROWS,
		FLAG_STOCKPILE_FOCUS_TAB,
		FLAG_STOCKPILE_HALF_COLORS,
		FLAG_BLUEPRINT_BASE_PRICE_TECH_1,
		FLAG_BLUEPRINT_BASE_PRICE_TECH_2,
		FLAG_TRANSACTION_HISTORY,
		FLAG_JOURNAL_HISTORY,
		FLAG_MARKET_ORDER_HISTORY,
		FLAG_STRONG_COLORS,
		FLAG_ASKED_CHECK_ALL_TRACKER,
		FLAG_TRACKER_USE_ASSET_PRICE_FOR_SELL_ORDERS,
		FLAG_FOCUS_EVE_ONLINE_ON_ESI_UI_CALLS,
		FLAG_SAVE_TOOLS_ON_EXIT
	}

	private static final SettingsLock LOCK = new SettingsLock();
	private static Settings settings;
	private static boolean testMode = false;

//External
	//Price						Saved by PriceDataGetter.process() in pricedata.dat (on api update)
	private Map<Integer, PriceData> priceDatas = new HashMap<>(); //TypeID : int
//API Data
	//Api id to owner name		Saved by TaskDialog.update() (on API update)
	private final Map<Long, String> owners = new HashMap<>();
//!! - Values
	//OK - Custom Price			Saved by JUserListPanel.edit()/delete() + SettingsDialog.save()
	//Lock OK
	private Map<Integer, UserItem<Integer, Double>> userPrices = new HashMap<>(); //TypeID : int
	//OK - Custom Item Name		Saved by JUserListPanel.edit()/delete() + SettingsDialog.save()
	//Lock OK
	private Map<Long, UserItem<Long, String>> userNames = new HashMap<>(); //ItemID : long
	//Eve Item Name				Saved by TaskDialog.update() (on API update)
	//Lock ???
	private Map<Long, String> eveNames = new HashMap<>();
//!! - Stockpile				Saved by StockpileTab.removeItems() / addStockpile() / removeStockpile()
	//							Could be more selective...
	//Lock FAIL!!!
	private final List<Stockpile> stockpiles = new ArrayList<>();
	private int stockpileColorGroup2 = 100;
	private int stockpileColorGroup3 = 0;
//Routing						Saved by ???
	//Lock ???
	private final RoutingSettings routingSettings = new RoutingSettings();
//Overview						Saved by JOverviewMenu.ListenerClass.NEW/DELETE/RENAME
	//Lock OK
	private final Map<String, OverviewGroup> overviewGroups = new HashMap<>();
//Export						Saved in ExportDialog.saveSettings()
	//Lock OK
	private final ExportSettings exportSettings = new ExportSettings();
//Tracker						Saved by TaskDialog.update() (on API update)
	private final Map<TrackerDate, TrackerNote> trackerNotes = new HashMap<>();
	private final Map<String, Boolean> trackerFilters = new HashMap<>();
	private boolean trackerSelectNew = true;
//Runtime flags					Is not saved to file
	private boolean settingsLoadError = false;
//Settings Dialog:				Saved by SettingsDialog.save()
	//Lock OK
	//Mixed boolean flags
	private final Map<SettingFlag, Boolean> flags = new EnumMap<>(SettingFlag.class);
	//Price
	private PriceDataSettings priceDataSettings = new PriceDataSettings();
	//Contract price
	private final ContractPriceSettings contractPriceSettings = new ContractPriceSettings();
	//Proxy (API)
	private ProxyData proxyData = new ProxyData();
	//FIXME - - > Settings: Create windows settings
	//Window
	//							Saved by MainWindow.ListenerClass.componentMoved() (on change)
	private Point windowLocation = new Point(0, 0);
	//							Saved by MainWindow.ListenerClass.componentResized() (on change)
	private Dimension windowSize = new Dimension(800, 600);
	//							Saved by MainWindow.ListenerClass.componentMoved() (on change)
	private boolean windowMaximized = false;
	//							Saved by SettingsDialog.save()
	private boolean windowAutoSave = true;
	private boolean windowAlwaysOnTop = false;
	//Assets
	private int maximumPurchaseAge = 0;
	//Reprocess price
	private ReprocessSettings reprocessSettings = new ReprocessSettings();
	//Public Market Orders Last Update
	private Date publicMarketOrdersLastUpdate = null;
	//Public Market Orders Next Update
	private Date publicMarketOrdersNextUpdate = getNow();
	//Market Orders Outbid
	private final Map<Long, Outbid> marketOrdersOutbid = new HashMap<>();
	//SellOrderRange
	private MarketOrderRange outbidOrderRange = MarketOrderRange.REGION;
	//Cache
	private Boolean filterOnEnter = null; //Filter tools
	private Boolean highlightSelectedRows = null;  //Assets
	private Boolean reprocessColors = null;  //Assets
	private Boolean stockpileHalfColors = null; //Stockpile
//Table settings
	//Filters					Saved by ExportFilterControl.saveSettings()
	//Lock OK
	private final Map<String, Map<String, List<Filter>>> tableFilters = new HashMap<>();
	//Columns					Saved by EnumTableFormatAdaptor.getMenu() - Reset
	//									 EditColumnsDialog.save() - Edit Columns
	//									 JAutoColumnTable.ListenerClass.mouseReleased() - Moved
	//									 ViewManager.loadView() - Load View
	//Lock OK
	private final Map<String, List<SimpleColumn>> tableColumns = new HashMap<>();
	//Column Width				Saved by JAutoColumnTable.saveColumnsWidth()
	//Lock OK
	private final Map<String, Map<String, Integer>> tableColumnsWidth = new HashMap<>();
	//Resize Mode				Saved by EnumTableFormatAdaptor.getMenu()
	//Lock OK
	private final Map<String, ResizeMode> tableResize = new HashMap<>();
	//Views						Saved by EnumTableFormatAdaptor.getMenu() - New
	//									 ViewManager.rename() - Rename
	//									 ViewManager.delete() - Delete
	//Lock OK
	private final Map<String, Map<String, View>> tableViews = new HashMap<>();
//Tags						Saved by JMenuTags.addTag()/removeTag() + SettingsDialog.save()
	//Lock OK
	private final Map<String, Tag> tags = new HashMap<>();
	private final Map<TagID, Tags> tagIds = new HashMap<>();
//Jumps
	private final Map<Class<?>, List<MyLocation>> jumpLocations = new HashMap<>();
//Tools
	private final List<String> showTools = new ArrayList<>();

	protected Settings() {
		//Settings
		flags.put(SettingFlag.FLAG_FILTER_ON_ENTER, false); //Cached
		flags.put(SettingFlag.FLAG_HIGHLIGHT_SELECTED_ROWS, true); //Cached
		flags.put(SettingFlag.FLAG_REPROCESS_COLORS, false); //Cached
		flags.put(SettingFlag.FLAG_IGNORE_SECURE_CONTAINERS, true);
		flags.put(SettingFlag.FLAG_STOCKPILE_FOCUS_TAB, true);
		flags.put(SettingFlag.FLAG_STOCKPILE_HALF_COLORS, false); //Cached
		flags.put(SettingFlag.FLAG_INCLUDE_SELL_ORDERS, true);
		flags.put(SettingFlag.FLAG_INCLUDE_BUY_ORDERS, false);
		flags.put(SettingFlag.FLAG_INCLUDE_SELL_CONTRACTS, false);
		flags.put(SettingFlag.FLAG_INCLUDE_BUY_CONTRACTS, false);
		flags.put(SettingFlag.FLAG_INCLUDE_MANUFACTURING, false);
		flags.put(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_1, true);
		flags.put(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_2, false);
		flags.put(SettingFlag.FLAG_TRANSACTION_HISTORY, true);
		flags.put(SettingFlag.FLAG_JOURNAL_HISTORY, true);
		flags.put(SettingFlag.FLAG_MARKET_ORDER_HISTORY, true);
		flags.put(SettingFlag.FLAG_STRONG_COLORS, false);
		flags.put(SettingFlag.FLAG_ASKED_CHECK_ALL_TRACKER, false);
		flags.put(SettingFlag.FLAG_TRACKER_USE_ASSET_PRICE_FOR_SELL_ORDERS, false);
		flags.put(SettingFlag.FLAG_FOCUS_EVE_ONLINE_ON_ESI_UI_CALLS, true);
		flags.put(SettingFlag.FLAG_SAVE_TOOLS_ON_EXIT, false);
		cacheFlags();
	}

	public static Settings get() {
		load();
		return settings;
	}

	public static void setTestMode(boolean testMode) {
		Settings.testMode = testMode;
	}

	public static void lock(String msg) {
		LOCK.lock(msg);
	}

	public static void unlock(String msg) {
		LOCK.unlock(msg);
	}

	public static boolean ignoreSave() {
		return LOCK.ignoreSave();
	}

	public static void waitForEmptySaveQueue() {
		LOCK.waitForEmptySaveQueue();
	}

	public static void saveStart() {
		LOCK.saveStart();
	}

	public static void saveEnd() {
		LOCK.saveEnd();
	}

	public synchronized static void load() {
		if (settings == null) {
			SplashUpdater.setProgress(30);
			autoImportSettings();
			settings = SettingsReader.load(new EmptySettingsFactory(), Settings.getPathSettings());
			SplashUpdater.setProgress(35);
		}
	}

	private static void autoImportSettings() {
		if (Program.PROGRAM_DEV_BUILD && !testMode) { //Need import
			Program.setPortable(false);
			Path settingsFrom = Paths.get(Settings.getPathSettings());
			Path trackerFrom = Paths.get(Settings.getPathTrackerData());
			Path assetAddedFrom = Paths.get(Settings.getPathAssetAdded());
			Path assetAddedDatabaseFrom = Paths.get(Settings.getPathAssetAddedDatabase());
			Path citadelFrom = Paths.get(Settings.getPathCitadel());
			Path priceFrom = Paths.get(Settings.getPathPriceData());
			Path profilesFrom = Paths.get(Settings.getPathProfilesDirectory());
			Path contractPricesFrom = Paths.get(Settings.getPathContractPrices());
			Program.setPortable(true);
			Path settingsTo = Paths.get(Settings.getPathSettings());
			Path trackerTo = Paths.get(Settings.getPathTrackerData());
			Path assetAddedTo = Paths.get(Settings.getPathAssetAdded());
			Path assetAddedDatabaseTo = Paths.get(Settings.getPathAssetAddedDatabase());
			Path citadelTo = Paths.get(Settings.getPathCitadel());
			Path priceTo = Paths.get(Settings.getPathPriceData());
			Path profilesTo = Paths.get(Settings.getPathProfilesDirectory());
			Path contractPricesTo = Paths.get(Settings.getPathContractPrices());
			if (Files.exists(settingsFrom) && !Files.exists(settingsTo)) {
				LOG.info("Importing settings");
				try {
					Files.copy(settingsFrom, settingsTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(trackerFrom) && !Files.exists(trackerTo)) {
				LOG.info("Importing tracker data");
				try {
					Files.copy(trackerFrom, trackerTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(assetAddedFrom) && !Files.exists(assetAddedTo)) {
				LOG.info("Importing asset added");
				try {
					Files.copy(assetAddedFrom, assetAddedTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(assetAddedDatabaseFrom) && !Files.exists(assetAddedDatabaseTo)) {
				LOG.info("Importing asset added");
				try {
					Files.copy(assetAddedDatabaseFrom, assetAddedDatabaseTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(citadelFrom) && !Files.exists(citadelTo)) {
				LOG.info("Importing citadels");
				try {
					Files.copy(citadelFrom, citadelTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(priceFrom) && !Files.exists(priceTo)) {
				LOG.info("Importing prices");
				try {
					Files.copy(priceFrom, priceTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(contractPricesFrom) && !Files.exists(contractPricesTo)) {
				LOG.info("Importing contract prices");
				try {
					Files.copy(contractPricesFrom, contractPricesTo);
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
			if (Files.exists(profilesFrom) && !Files.exists(profilesTo)) {
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.xml");
				try {
					LOG.info("Importing profiles");
					Files.walkFileTree(profilesFrom, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
							if (dir.equals(profilesFrom)) {
								Files.createDirectories(profilesTo.resolve(profilesFrom.relativize(dir)));
								return FileVisitResult.CONTINUE;
							} else {
								return FileVisitResult.SKIP_SUBTREE;
							}
						}

						@Override
						public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
							if (matcher.matches(file.getFileName())) {
								Files.copy(file, profilesTo.resolve(profilesFrom.relativize(file)));
							}
							return FileVisitResult.CONTINUE;
						}
					});
					LOG.info("	OK");
				} catch (IOException ex) {
					LOG.info("	FAILED");
				}
			}
		}
	}

	public ExportSettings getExportSettings() {
		return exportSettings;
	}

	public static void saveSettings() {
		LOCK.lock("Save Settings");
		try {
			SettingsWriter.save(settings, Settings.getPathSettings());
		} finally {
			LOCK.unlock("Save Settings");
		}
	}

	public Map<TrackerDate, TrackerNote> getTrackerNotes() {
		return trackerNotes;
	}

	public Map<String, Boolean> getTrackerFilters() {
		return trackerFilters;
	}

	public boolean isTrackerSelectNew() {
		return trackerSelectNew;
	}

	public void setTrackerSelectNew(boolean trackerSelectNew) {
		this.trackerSelectNew = trackerSelectNew;
	}

	public PriceDataSettings getPriceDataSettings() {
		return priceDataSettings;
	}

	public void setPriceDataSettings(final PriceDataSettings priceDataSettings) {
		this.priceDataSettings = priceDataSettings;
	}

	public ContractPriceSettings getContractPriceSettings() {
		return contractPriceSettings;
	}

	public Map<Integer, UserItem<Integer, Double>> getUserPrices() {
		return userPrices;
	}

	public void setUserPrices(final Map<Integer, UserItem<Integer, Double>> userPrices) {
		this.userPrices = userPrices;
	}

	public Map<Long, UserItem<Long, String>> getUserItemNames() {
		return userNames;
	}

	public void setUserItemNames(final Map<Long, UserItem<Long, String>> userItemNames) {
		this.userNames = userItemNames;
	}

	public void setPriceData(final Map<Integer, PriceData> priceData) {
		this.priceDatas = priceData;
	}

	public Map<Long, String> getEveNames() {
		return eveNames;
	}

	public void setEveNames(Map<Long, String> eveNames) {
		this.eveNames = eveNames;
	}

	public Map<Integer, PriceData> getPriceData() {
		return priceDatas;
	}

	public Map<SettingFlag, Boolean> getFlags() {
		return flags;
	}

	public final void cacheFlags() {
		highlightSelectedRows = flags.get(SettingFlag.FLAG_HIGHLIGHT_SELECTED_ROWS);
		filterOnEnter = flags.get(SettingFlag.FLAG_FILTER_ON_ENTER);
		reprocessColors = flags.get(SettingFlag.FLAG_REPROCESS_COLORS);
		stockpileHalfColors = flags.get(SettingFlag.FLAG_STOCKPILE_HALF_COLORS);
	}

	public ReprocessSettings getReprocessSettings() {
		return reprocessSettings;
	}

	public void setReprocessSettings(final ReprocessSettings reprocessSettings) {
		this.reprocessSettings = reprocessSettings;
	}

	public RoutingSettings getRoutingSettings() {
		return routingSettings;
	}

	public List<MyLocation> getJumpLocations(Class<?> clazz) {
		List<MyLocation> locations = jumpLocations.get(clazz);
		if (locations == null) {
			locations = new ArrayList<>();
			jumpLocations.put(clazz, locations);
		}
		return locations;
	}

	public void addJumpLocation(Class<?> clazz, MyLocation location) {
		getJumpLocations(clazz).add(location);
	}
	public void removeJumpLocation(Class<?> clazz, MyLocation location) {
		getJumpLocations(clazz).remove(location);
	}
	public void clearJumpLocations(Class<?> clazz) {
		getJumpLocations(clazz).clear();
	}

	public ProxyData getProxyData() {
		return proxyData;
	}

	public void setProxyData(ProxyData proxyData) {
		this.proxyData = proxyData;
	}

	public Map<Long, String> getOwners() {
		return owners;
	}

	public Map<String, Map<String, List<Filter>>> getTableFilters() {
		return tableFilters;
	}

	public Map<String, List<Filter>> getTableFilters(final String key) {
		if (!tableFilters.containsKey(key)) {
			tableFilters.put(key, new HashMap<>());
		}
		return tableFilters.get(key);
	}

	public Map<String, List<SimpleColumn>> getTableColumns() {
		return tableColumns;
	}

	public Map<String, Map<String, Integer>> getTableColumnsWidth() {
		return tableColumnsWidth;
	}

	public Map<String, ResizeMode> getTableResize() {
		return tableResize;
	}

	public Map<String, Map<String, View>> getTableViews() {
		return tableViews;
	}

	public Map<String, View> getTableViews(String name) {
		Map<String, View> views = tableViews.get(name);
		if (views == null) {
			views = new TreeMap<>(new CaseInsensitiveComparator());
			tableViews.put(name, views);
		}
		return views;
	}

	public Map<String, Tag> getTags() {
		return tags;
	}

	public Tags getTags(TagID tagID) {
		Tags set = tagIds.get(tagID);
		if (set == null) {
			set = new Tags();
			tagIds.put(tagID, set);
		}
		return set;
	}

	public Date getPublicMarketOrdersNextUpdate() {
		return publicMarketOrdersNextUpdate;
	}

	public void setPublicMarketOrdersNextUpdate(Date publicMarketOrdersNextUpdate) {
		this.publicMarketOrdersNextUpdate = publicMarketOrdersNextUpdate;
	}

	public Date getPublicMarketOrdersLastUpdate() {
		return publicMarketOrdersLastUpdate;
	}

	public void setPublicMarketOrdersLastUpdate(Date publicMarketOrdersLastUpdate) {
		this.publicMarketOrdersLastUpdate = publicMarketOrdersLastUpdate;
	}

	public MarketOrderRange getOutbidOrderRange() {
		return outbidOrderRange;
	}

	public void setOutbidOrderRange(MarketOrderRange sellOrderOutbidRange) {
		this.outbidOrderRange = sellOrderOutbidRange;
	}

	public Map<Long, Outbid> getMarketOrdersOutbid() {
		return marketOrdersOutbid;
	}

	public void setMarketOrdersOutbid(Map<Long, Outbid> outbids) {
		marketOrdersOutbid.putAll(outbids);
	}

	public int getMaximumPurchaseAge() {
		return maximumPurchaseAge;
	}

	public void setMaximumPurchaseAge(final int maximumPurchaseAge) {
		this.maximumPurchaseAge = maximumPurchaseAge;
	}

	public boolean isFilterOnEnter() {
		if (filterOnEnter == null) {
			filterOnEnter = flags.get(SettingFlag.FLAG_FILTER_ON_ENTER);
		}
		return filterOnEnter;
	}

	public void setFilterOnEnter(final boolean filterOnEnter) {
		flags.put(SettingFlag.FLAG_FILTER_ON_ENTER, filterOnEnter); //Save & Load
		this.filterOnEnter = filterOnEnter;
	}

	public boolean isHighlightSelectedRows() { //High volume call - Map.get is too slow, use cache
		return highlightSelectedRows;
	}

	public void setHighlightSelectedRows(final boolean highlightSelectedRows) {
		flags.put(SettingFlag.FLAG_HIGHLIGHT_SELECTED_ROWS, highlightSelectedRows);
		this.highlightSelectedRows = highlightSelectedRows;
	}

	public boolean isIgnoreSecureContainers() {
		return flags.get(SettingFlag.FLAG_IGNORE_SECURE_CONTAINERS);
	}

	public void setIgnoreSecureContainers(final boolean ignoreSecureContainers) {
		flags.put(SettingFlag.FLAG_IGNORE_SECURE_CONTAINERS, ignoreSecureContainers);
	}

	public boolean isReprocessColors() { //High volume call - Map.get is too slow, use cache
		return reprocessColors;
	}

	public void setReprocessColors(final boolean reprocessColors) {
		flags.put(SettingFlag.FLAG_REPROCESS_COLORS, reprocessColors);
		this.reprocessColors = reprocessColors;
	}

	public boolean isStockpileFocusTab() {
		return flags.get(SettingFlag.FLAG_STOCKPILE_FOCUS_TAB);
	}

	public void setStockpileFocusTab(final boolean stockpileFocusOnAdd) {
		flags.put(SettingFlag.FLAG_STOCKPILE_FOCUS_TAB, stockpileFocusOnAdd);
	}

	public boolean isStockpileHalfColors() {
		return stockpileHalfColors;
	}

	public void setStockpileHalfColors(final boolean stockpileHalfColors) {
		flags.put(SettingFlag.FLAG_STOCKPILE_HALF_COLORS, stockpileHalfColors);
		this.stockpileHalfColors = stockpileHalfColors;
	}

	public boolean isIncludeSellOrders() {
		return flags.get(SettingFlag.FLAG_INCLUDE_SELL_ORDERS);
	}

	public void setIncludeSellOrders(final boolean includeSellOrders) {
		flags.put(SettingFlag.FLAG_INCLUDE_SELL_ORDERS, includeSellOrders);
	}

	public boolean isIncludeBuyOrders() {
		return flags.get(SettingFlag.FLAG_INCLUDE_BUY_ORDERS);
	}

	public void setIncludeBuyOrders(final boolean includeBuyOrders) {
		flags.put(SettingFlag.FLAG_INCLUDE_BUY_ORDERS, includeBuyOrders);
	}

	public boolean isIncludeBuyContracts() {
		return flags.get(SettingFlag.FLAG_INCLUDE_BUY_CONTRACTS);
	}

	public void setIncludeBuyContracts(final boolean includeBuyContracts) {
		flags.put(SettingFlag.FLAG_INCLUDE_BUY_CONTRACTS, includeBuyContracts);
	}

	public boolean isIncludeSellContracts() {
		return flags.get(SettingFlag.FLAG_INCLUDE_SELL_CONTRACTS);
	}

	public void setIncludeSellContracts(final boolean includeSellContracts) {
		flags.put(SettingFlag.FLAG_INCLUDE_SELL_CONTRACTS, includeSellContracts);
	}

	public boolean isIncludeManufacturing() {
		return flags.get(SettingFlag.FLAG_INCLUDE_MANUFACTURING);
	}

	public boolean setIncludeManufacturing(final boolean includeManufacturing) {
		return flags.put(SettingFlag.FLAG_INCLUDE_MANUFACTURING, includeManufacturing);
	}

	public boolean isBlueprintBasePriceTech1() {
		return flags.get(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_1);
	}

	public void setBlueprintBasePriceTech1(final boolean blueprintsTech1) {
		flags.put(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_1, blueprintsTech1);
	}

	public boolean isBlueprintBasePriceTech2() {
		return flags.get(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_2);
	}

	public void setBlueprintBasePriceTech2(final boolean blueprintsTech2) {
		flags.put(SettingFlag.FLAG_BLUEPRINT_BASE_PRICE_TECH_2, blueprintsTech2);
	}

	public boolean isTransactionHistory() {
		return flags.get(SettingFlag.FLAG_TRANSACTION_HISTORY);
	}

	public void setTransactionHistory(final boolean transactionHistory) {
		flags.put(SettingFlag.FLAG_TRANSACTION_HISTORY, transactionHistory);
	}

	public boolean isJournalHistory() {
		return flags.get(SettingFlag.FLAG_JOURNAL_HISTORY);
	}

	public void setJournalHistory(final boolean journalHistory) {
		flags.put(SettingFlag.FLAG_JOURNAL_HISTORY, journalHistory);
	}
	public boolean isMarketOrderHistory() {
		return flags.get(SettingFlag.FLAG_MARKET_ORDER_HISTORY);
	}

	public void setMarketOrderHistory(final boolean marketOrderHistory) {
		flags.put(SettingFlag.FLAG_MARKET_ORDER_HISTORY, marketOrderHistory);
	}

	public boolean isStrongColors() {
		return flags.get(SettingFlag.FLAG_STRONG_COLORS);
	}

	public void setStrongColors(final boolean strongColors) {
		flags.put(SettingFlag.FLAG_STRONG_COLORS, strongColors);
	}

	public boolean isAskedCheckAllTracker() {
		return flags.get(SettingFlag.FLAG_ASKED_CHECK_ALL_TRACKER);
	}

	public void setAskedCheckAllTracker(final boolean checkAllTracker) {
		flags.put(SettingFlag.FLAG_ASKED_CHECK_ALL_TRACKER, checkAllTracker);
	}

	public boolean isTrackerUseAssetPriceForSellOrders() {
		return flags.get(SettingFlag.FLAG_TRACKER_USE_ASSET_PRICE_FOR_SELL_ORDERS);
	}

	public void setTrackerUseAssetPriceForSellOrders(final boolean checkAllTracker) {
		flags.put(SettingFlag.FLAG_TRACKER_USE_ASSET_PRICE_FOR_SELL_ORDERS, checkAllTracker);
	}

	public boolean isFocusEveOnlineOnEsiUiCalls() {
		return flags.get(SettingFlag.FLAG_FOCUS_EVE_ONLINE_ON_ESI_UI_CALLS);
	}

	public void setFocusEveOnlineOnEsiUiCalls(final boolean focusEveOnlineOnEsiUiCalls) {
		flags.put(SettingFlag.FLAG_FOCUS_EVE_ONLINE_ON_ESI_UI_CALLS, focusEveOnlineOnEsiUiCalls);
	}

	public boolean isSaveToolsOnExit() {
		return flags.get(SettingFlag.FLAG_SAVE_TOOLS_ON_EXIT);
	}

	public void setSaveToolsOnExit(final boolean saveToolsOnExit) {
		flags.put(SettingFlag.FLAG_SAVE_TOOLS_ON_EXIT, saveToolsOnExit);
	}

	public List<String> getShowTools() {
		return showTools;
	}

	public List<Stockpile> getStockpiles() {
		return stockpiles;
	}

	public int getStockpileColorGroup2() {
		return stockpileColorGroup2;
	}

	public void setStockpileColorGroup2(int stockpileColorGroup1) {
		this.stockpileColorGroup2 = stockpileColorGroup1;
	}

	public int getStockpileColorGroup3() {
		return stockpileColorGroup3;
	}

	public void setStockpileColorGroup3(int stockpileColorGroup2) {
		this.stockpileColorGroup3 = stockpileColorGroup2;
	}

	//Window
	public Point getWindowLocation() {
		return windowLocation;
	}

	public void setWindowLocation(final Point windowLocation) {
		this.windowLocation = windowLocation;
	}

	public boolean isWindowMaximized() {
		return windowMaximized;
	}

	public void setWindowMaximized(final boolean windowMaximized) {
		this.windowMaximized = windowMaximized;
	}

	public Dimension getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(final Dimension windowSize) {
		this.windowSize = windowSize;
	}

	public boolean isWindowAutoSave() {
		return windowAutoSave;
	}

	public void setWindowAutoSave(final boolean windowAutoSave) {
		this.windowAutoSave = windowAutoSave;
	}

	public boolean isWindowAlwaysOnTop() {
		return windowAlwaysOnTop;
	}

	public void setWindowAlwaysOnTop(final boolean windowAlwaysOnTop) {
		this.windowAlwaysOnTop = windowAlwaysOnTop;
	}

	public boolean isSettingsLoadError() {
		return settingsLoadError;
	}

	public void setSettingsLoadError(boolean settingsLoadError) {
		this.settingsLoadError = settingsLoadError;
	}

	public Map<String, OverviewGroup> getOverviewGroups() {
		return overviewGroups;
	}

	private static String getPathSettings() {
		return FileUtil.getLocalFile(Settings.PATH_SETTINGS, !Program.isPortable());
	}

	public static String getPathTrackerData() {
		return FileUtil.getLocalFile(Settings.PATH_TRACKER_DATA, !Program.isPortable());
	}

	public static String getPathContractPrices() {
		return FileUtil.getLocalFile(Settings.PATH_CONTRACT_PRICES, !Program.isPortable());
	}

	public static String getPathAssetAdded() {
		return FileUtil.getLocalFile(Settings.PATH_ASSET_ADDED, !Program.isPortable());
	}

	public static String getPathAssetAddedDatabase() {
		return FileUtil.getLocalFile(Settings.PATH_ASSET_ADDED_DATABASE, !Program.isPortable());
	}

	public static String getPathConquerableStations() {
		return FileUtil.getLocalFile(Settings.PATH_CONQUERABLE_STATIONS, !Program.isPortable());
	}

	public static String getPathCitadel() {
		return FileUtil.getLocalFile(Settings.PATH_CITADEL, !Program.isPortable());
	}

	public static String getPathJumps() {
		return FileUtil.getLocalFile(Settings.PATH_JUMPS, false);
	}

	public static String getPathFlags() {
		return FileUtil.getLocalFile(Settings.PATH_FLAGS, false);
	}

	public static String getPathPriceData() {
		return FileUtil.getLocalFile(Settings.PATH_PRICE_DATA, !Program.isPortable());
	}

	public static String getPathAssetsOld() {
		return FileUtil.getLocalFile(Settings.PATH_ASSETS, !Program.isPortable());
	}

	public static String getPathProfilesDirectory() {
		return FileUtil.getLocalFile(Settings.PATH_PROFILES, !Program.isPortable());
	}

	public static String getPathStaticDataDirectory() {
		return FileUtil.getLocalFile(Settings.PATH_DATA, false);
	}

	public static String getPathDataDirectory() {
		return FileUtil.getLocalFile(Settings.PATH_DATA, !Program.isPortable());
	}

	public static String getPathItems() {
		return FileUtil.getLocalFile(Settings.PATH_ITEMS, false);
	}
	
	public static String getPathItemsUpdates() {
		return FileUtil.getLocalFile(Settings.PATH_ITEMS_UPDATES, !Program.isPortable());
	}

	public static String getPathLocations() {
		return FileUtil.getLocalFile(Settings.PATH_LOCATIONS, false);
	}

	public static String getPathReadme() {
		return FileUtil.getLocalFile(Settings.PATH_README, false);
	}

	public static String getPathLicense() {
		return FileUtil.getLocalFile(Settings.PATH_LICENSE, false);
	}

	public static String getPathCredits() {
		return FileUtil.getLocalFile(Settings.PATH_CREDITS, false);
	}

	public static String getPathChangeLog() {
		return FileUtil.getLocalFile(Settings.PATH_CHANGELOG, false);
	}

	public static String getUserDirectory() {
		File userDir = new File(System.getProperty("user.home", "."));
		return userDir.getAbsolutePath() + File.separator;
	}

	public static Date getNow() {
		return new Date();
	}

	public static DateFormat getSettingsDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	}

	public boolean isUpdatable(final Date date) {
		Date now = Settings.getNow();
		return date != null &&
				((now.after(date)
				|| now.equals(date)
				|| Program.isForceUpdate())
				&& !Program.isForceNoUpdate());
	}

	private static class SettingsLock {

		private boolean locked = false;
		private final SettingsQueue settingsQueue = new SettingsQueue();

		public boolean ignoreSave() {
			return settingsQueue.ignoreSave();
		}

		public void saveStart() {
			settingsQueue.saveStart();
		}

		public void saveEnd() {
			settingsQueue.saveEnd();
		}

		public void waitForEmptySaveQueue() {
			settingsQueue.waitForEmptySaveQueue();
		}

		public synchronized void lock(String msg) {
			while (locked) {
				try {
					wait();
				} catch (InterruptedException ex) {

				}
			}
			locked = true;
			LOG.debug("Settings Locked: " + msg);
		}

		public synchronized void unlock(String msg) {
			locked = false;
			LOG.debug("Settings Unlocked: " + msg);
			notify();
		}
	}

	private static class SettingsQueue {

		private short savesQueue = 0;

		public synchronized boolean ignoreSave() {
			LOG.debug("Save Queue: " + savesQueue + " ignore: " + (savesQueue > 1));
			return savesQueue > 1;
		}

		public synchronized void saveStart() {
			this.savesQueue++;
			notifyAll();
		}

		public synchronized void saveEnd() {
			this.savesQueue--;
			notifyAll();
		}

		public synchronized void waitForEmptySaveQueue() {
			while (savesQueue > 0) {
				try {
					wait();
				} catch (InterruptedException ex) {

				}
			}
		}
	}

	private static class EmptySettingsFactory implements SettingsFactory {
		@Override
		public Settings create() {
			return new Settings();
		}
	}

	public static interface SettingsFactory {
		public Settings create();
	}
}
