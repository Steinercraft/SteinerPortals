package io.github.alvinnorin.steinerportals;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.alvinnorin.steinerhomes.Home;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class SteinerPortals extends JavaPlugin implements Listener {
	
	public static JavaPlugin plugin = null;

	public UUID LOCATION = null;
	public HashMap<UUID, Location> LOCATIONS = new HashMap<UUID, Location>();
	public Boolean calculatingLocation = false;
	public Long COOLDOWN = (long) 0;
	public HashMap<Player, Long> FUSE = new HashMap();
	public int COLLECTION = 0;
	private int TPS = 0;
	
	public List<Player> ENTERED = new ArrayList<Player>();
	
	private List<Home> HOMES = io.github.alvinnorin.steinerhomes.API.getHomes();
    
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
    	if (getConfig().contains("x"+event.getPlayer().getLocation().getBlockX())) {
    		if (event.getPlayer().getLocation().getBlockZ() >=
    				getConfig().getInt("x"+event.getPlayer().getLocation().getBlockX()+".from")
    				&&
    				event.getPlayer().getLocation().getBlockZ() <=
    				getConfig().getInt("x"+event.getPlayer().getLocation().getBlockX()+".to")
    				&&
    				(event.getPlayer().getLocation().getBlockY() ==
    				getConfig().getInt("x"+event.getPlayer().getLocation().getBlockX()+".y")
    				|| event.getPlayer().getLocation().getBlockY() ==
    	    				getConfig().getInt("x"+event.getPlayer().getLocation().getBlockX()+".y") + 1)) {
    			randomTeleport(event.getPlayer());
    		}
    	} else if (getConfig().contains("z"+event.getPlayer().getLocation().getBlockZ())) {
    		if (event.getPlayer().getLocation().getBlockX() >=
    				getConfig().getInt("z"+event.getPlayer().getLocation().getBlockZ()+".from")
    				&&
    				event.getPlayer().getLocation().getBlockX() <=
    				getConfig().getInt("z"+event.getPlayer().getLocation().getBlockZ()+".to")
    				&&
    				(event.getPlayer().getLocation().getBlockY() ==
    				getConfig().getInt("z"+event.getPlayer().getLocation().getBlockZ()+".y")
    				|| event.getPlayer().getLocation().getBlockY() ==
    				getConfig().getInt("z"+event.getPlayer().getLocation().getBlockZ()+".y") + 1)) {
    			randomTeleport(event.getPlayer());
    		}
    	}
    }
    
    private static Location getRegion(Location location) {
    	return new Location(location.getWorld(), (int) location.getChunk().getX() >> 5, 0, (int) location.getChunk().getZ() >> 5);
    }
    
    private List<Location> getBlackListedRegions() {
    	List<Location> regions = new ArrayList<Location>();
    	if (getConfig().contains("blacklisted"))
	    	for (String location : getConfig().getConfigurationSection("blacklisted").getKeys(false))
	    		regions.add(deserializeLocation(location));
    	return regions;
    }
    
    private boolean isRegionBlackListed(Location region) {
    	return getConfig().contains("blacklisted."+serializeLocation(region));
    }
    
    private List<Location> getSpotsInRegion(Location region) {
    	List<Location> spots = new ArrayList<Location>();
    	for (String location : getConfig().getStringList("blacklisted."+serializeLocation(region)))
    		spots.add(deserializeLocation(location));
    	return spots;
    }
    
    private void blackListSpot(Location location) {
    	String region = serializeLocation(getRegion(location));
    	String spot = serializeLocation(location);
    	if (!getConfig().contains("blacklisted."+region))
    		getConfig().set("blacklisted."+region, spot);
    	else if (!getConfig().getStringList("blacklisted."+region).contains(spot))
    		getConfig().set("blacklisted."+region, spot);
    }
    
    public void sendTitle(Player player, String title) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(title));
    }
    
    public Location getLocation() {
    	if (LOCATIONS.get(LOCATION) == null)
    		setLocation(getRandomLocation());
    	return LOCATIONS.get(LOCATION);
    }
    
    public void setLocation(Location location) {
		UUID uuid = UUID.randomUUID();
		LOCATIONS.put(uuid, location);
		LOCATION = uuid;
    }
    
    public void setLocation(UUID location) {
    	ENTERED.clear();
        LOCATIONS.remove(LOCATION);
		LOCATION = location;
    }
    
    public UUID getRandomLocation() {
    	if (LOCATIONS.isEmpty()) {
			// Todo: Verbosity options
    		//Bukkit.getConsoleSender().sendMessage(ChatColor.RED+"Empty!");
            //Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Looking for a new random portal location ..");
            long time = System.currentTimeMillis();
        	calculateLocation();
        	//Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Found new random portal location! Took "+(System.currentTimeMillis() - time)/1000+" seconds");
    	} try {
    		UUID uuid = null;
    		while (true) {
	    		uuid = (UUID) LOCATIONS.keySet().toArray()[ThreadLocalRandom.current().nextInt(0, LOCATIONS.size())];
                ProtectedLocation protectedLocation = new ProtectedLocation(LOCATIONS.get(uuid));
	    		if (getProtectedLocations().isLocationProtected(protectedLocation))
	    			LOCATIONS.remove(uuid);
	    		else
	    			return uuid;
                Thread.sleep(50);
    		}
    	} catch (Exception e) {
    		return (UUID) LOCATIONS.keySet().toArray()[0];
    	}
    }
    
    public void addLocation(Location location) {
		UUID uuid = UUID.randomUUID();
		LOCATIONS.put(uuid, location);
		saveLocations();
    }
    
    public void randomTeleport(Player player) {
    	Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+player.getName()+" random teleported");
		COOLDOWN = System.currentTimeMillis();
		final Long cooldown = COOLDOWN;
    	if (ENTERED.contains(player)) {
    		if (LOCATIONS.size() >= 1)
    			setLocation(getRandomLocation());
    		else
	            Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	                @Override
	                public void run() {
	                	setLocation(getRandomLocation());
	                }
	            });
    	} else
    		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable(){
                @Override
                public void run() {
                	if (cooldown == COOLDOWN)
                		setLocation(getRandomLocation());
                }
    		}, 20 * 20);
    	ENTERED.add(player);
    	FUSE.put(player, COOLDOWN);
    	Location location = getLocation();
		player.teleport(location);
		Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable(){
            @Override
            public void run() {
            	if (player.getLocation().getBlockX() == location.getBlockX()
            			&& player.getLocation().getBlockZ() == location.getBlockZ()) {
            		sendTitle(player, ChatColor.DARK_RED+"If you are stuck in a block, do /spawn");
            		Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable(){
                        @Override
                        public void run() {
                        	if (player.getLocation().getBlockX() == location.getBlockX()
                        			&& player.getLocation().getBlockZ() == location.getBlockZ()) {
                        		player.teleport(player.getWorld().getSpawnLocation());
                        		sendTitle(player, ChatColor.DARK_RED+"You have been teleported to spawn due to inactivity");
                        		FUSE.remove(player);
                        	}
                        }
            		}, 20 * 10);
            	}
            }
		}, 20 * 10);
    }
    
    public void calculateLocation() {
    	calculatingLocation = true;
    	World world = getServer().getWorld("world");
    	Location spawn = world.getSpawnLocation();
    	int distance = 100000;
    	
    	int x, z;
    	
    	while (true) {
    		x = ThreadLocalRandom.current().nextInt(spawn.getBlockX() - distance, spawn.getBlockX() + distance);
    		z = ThreadLocalRandom.current().nextInt(spawn.getBlockZ() - distance, spawn.getBlockZ() + distance);
    		Location location = new Location(world, x, 0, z);
            ProtectedLocation protectedLocation = new ProtectedLocation(location);
    		if (!getProtectedLocations().isLocationProtected(protectedLocation))
    			if ((world.getBlockAt(world.getHighestBlockAt(x, z).getLocation().subtract(0, 1, 0)).getBlockData().getMaterial().isSolid())) {
    	    		addLocation(world.getHighestBlockAt(x, z).getLocation().add(0, 3, 0));
    	    		calculatingLocation = false;
    	    		break;
    			}
    		distance += 10000;
    	}
    }
    
    public void blackListHome(Home home) {
    	// Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Blacklisting region for new home location");
    	// TODO: Implement for an optimized version
    }

    public ProtectedLocationSet getProtectedLocations() {
        ProtectedLocationSet set = new ProtectedLocationSet();
        for (Home home : io.github.alvinnorin.steinerhomes.API.getHomes()) {
            Location location = home.getLocation();
            set.add(new ProtectedLocation(home.getWorldUUID(), location.getBlockX(), location.getBlockZ(), home.getOwner()));
        } return set;
    }

    public int getNumberOfHomesNearby(Location location) {
    	int number = 0;
    	for (Home home : io.github.alvinnorin.steinerhomes.API.getHomes()) {
    		try {
	    		if (home.getLocation().distance(location) < 1000) {
	    			number ++;
	    		}
    		} catch (java.lang.IllegalArgumentException e) {}
    	} return number;
    }
    
    public void saveLocations() {
    	try {
	    	for (String key : getConfig().getConfigurationSection("locations").getKeys(false))
	    		getConfig().set("locations."+key, null);
    	} catch (NullPointerException e) {}
    	for (UUID uuid : LOCATIONS.keySet())
    		getConfig().set("locations."+uuid.toString(), serializeLocation(LOCATIONS.get(uuid)));
    	saveConfig();
    }
    
    public void onDisable() {
    	//saveLocations();
        this.getLogger().info("Disabling SteinerPortals..");
    }
    
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onEntityDamageEvent(EntityDamageEvent event) {
    	if (event.getEntityType().equals(EntityType.PLAYER))
	    	if (FUSE.containsKey((Player) event.getEntity())) {
	    		if (event.getCause().equals(DamageCause.SUFFOCATION)
	    				|| event.getCause().equals(DamageCause.FALL))
	    			event.setCancelled(true);
	    	}
    }
    
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
    	FUSE.remove(event.getPlayer());
    }

	public void onEnable() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(this, this);
        plugin = this;
        saveDefaultConfig();
    	if (LOCATION == null) {
    		try {
	    		for (String key : getConfig().getConfigurationSection("locations").getKeys(false))
	    			setLocation(deserializeLocation(getConfig().getString("locations."+key)));
	    		LOCATION = getRandomLocation();
	    		Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Loaded stored random locations");
    		} catch (NullPointerException e) {
    			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Allocating initial random locations");
    			setLocation(getRandomLocation());
    		}
    	} COLLECTION = getConfig().getInt("collection", 100);
    	Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){
            @Override
            public void run() {
            	while (true) {
	            	while (LOCATIONS.size() < COLLECTION) {
	            		//if (TPS >= 19) {
	            		if (Bukkit.getServerTickManager().getTickRate() >= plugin.getConfig().getInt("lag-threshold", 15) && !LOCATIONS.isEmpty()) {
	            			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Looking for a new random teleportation location ..");
		            		long time = System.currentTimeMillis();
		            		calculateLocation();
		            		Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+"Found new random teleportation location! Took "+(System.currentTimeMillis() - time)+" milliseconds");
	            		} else
	            			Bukkit.getConsoleSender().sendMessage(ChatColor.RED+"The TPS is too low to generate new locations as of now ("+Bukkit.getServerTickManager().getTickRate()+" < "+plugin.getConfig().getInt("lag-threshold", 15)+")");
	            		Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN+""+LOCATIONS.size()+" locations generated");
	            		//}
	            	} try {
            			Thread.sleep(50);
            		} catch (Exception ignored) {}
            	}
            }
        }); getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
    		long sec;
    		long currentSec;
    		int ticks;
    		int delay;
    		
    		@Override
    		public void run() {
    			sec = (System.currentTimeMillis() / 1000);
    			
    			if(currentSec == sec) {
    				// this code block triggers each tick
    				ticks++;
    			} else {
    				// this code block triggers each second
    				currentSec = sec;
    				TPS = (TPS == 0 ? ticks : ((TPS + ticks) / 2));
    				ticks = 0;
    				
    				if ((++delay % 300) == 0) {
    					delay = 0;
    				}
    			}
    		}
    	}, 0, 1);
    }
	
	protected static String serializeLocation(Location location) {
		String serialized = "";
		if (location == null) {
			return "";
		}
		try {
			serialized = URLEncoder.encode(location.getWorld().getUID().toString() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} return serialized;
	}
	
	protected static Location deserializeLocation(String s) {
		try {
			s = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} if (s == null || s.trim() == "") {
			return null;
		} final String[] parts = s.split(":");
		if (parts.length == 4) {
			final World w = Bukkit.getServer().getWorld(UUID.fromString(parts[0]));
			final int x = Integer.parseInt(parts[1]);
			final int y = Integer.parseInt(parts[2]);
			final int z = Integer.parseInt(parts[3]);
			return new Location(w, x, y, z);
		} return null;
	}
    
    public void setRandomPortal(Player player) {
    	int from = 0, to = 0;
		if (rpGetPlayerDirection(player).equals("north") || rpGetPlayerDirection(player).equals("south")) {
			//	Width is along X
			//	from is the lower number. to is the higher.
			for (int i = 1; i < 10; i ++)
				if (!player.getLocation().getWorld().getBlockAt(
						player.getLocation().getBlockX() - i, player.getLocation().getBlockY(), player.getLocation().getBlockZ()).isEmpty())
					from = player.getLocation().getBlockX() - 1;
			for (int i = 1; i < 10; i ++)
				if (!player.getLocation().getWorld().getBlockAt(
						player.getLocation().getBlockX() + i, player.getLocation().getBlockY(), player.getLocation().getBlockZ()).isEmpty())
					to = player.getLocation().getBlockX() + 1;
			getConfig().set("z"+player.getLocation().getBlockZ()+".from", from);
			getConfig().set("z"+player.getLocation().getBlockZ()+".to", to);
			getConfig().set("z"+player.getLocation().getBlockZ()+".y", player.getLocation().getBlockY());
		} else if (rpGetPlayerDirection(player).equals("east") || rpGetPlayerDirection(player).equals("west")) {
			//	Width is along Z
			//	from is the lower number. to is the higher.
			from = player.getLocation().getBlockZ() - 1;
			to = player.getLocation().getBlockZ() + 1;
			getConfig().set("x"+player.getLocation().getBlockX()+".from", from);
			getConfig().set("x"+player.getLocation().getBlockX()+".to", to);
			getConfig().set("x"+player.getLocation().getBlockX()+".y", player.getLocation().getBlockY());
		}
		saveConfig();
		player.sendMessage(ChatColor.YELLOW+"Random portal set "+rpGetPlayerDirection(player));
    }
    
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		if (label.equals("setportal") && args[0].equals("random"))
			if (player.isOp()) {
				setRandomPortal(player);
			} else
				player.sendMessage(ChatColor.RED+"Only operators may use this command");

		return false;
	}
	
    public String rpGetPlayerDirection(Player playerSelf){
        String dir = "";
        float y = playerSelf.getLocation().getYaw();
        if( y < 0 ){y += 360;}
        y %= 360;
        int i = (int)((y+8) / 22.5);
        if(i == 0){dir = "west";}
        else if(i == 1){dir = "west northwest";}
        else if(i == 2){dir = "northwest";}
        else if(i == 3){dir = "north northwest";}
        else if(i == 4){dir = "north";}
        else if(i == 5){dir = "north northeast";}
        else if(i == 6){dir = "northeast";}
        else if(i == 7){dir = "east northeast";}
        else if(i == 8){dir = "east";}
        else if(i == 9){dir = "east southeast";}
        else if(i == 10){dir = "southeast";}
        else if(i == 11){dir = "south southeast";}
        else if(i == 12){dir = "south";}
        else if(i == 13){dir = "south southwest";}
        else if(i == 14){dir = "southwest";}
        else if(i == 15){dir = "west southwest";}
        else {dir = "west";}
        return dir;
   }
    
}