package me.BaddCamden.HubPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;


public class RegisterEvents implements TabCompleter, Listener, CommandExecutor {
	
	Main mainPlugin;
	public FileConfiguration database;
	public File datafile;
	Random rand = new Random();
	Logger onLog = Bukkit.getLogger();
	public RegisterEvents(Main main) {
		mainPlugin = main; 
		database = main.getDataBase();
		datafile = main.getDataFile();
        boolean foundW = false;
		for(String key : database.getKeys(false)) {
			
			if(key.equalsIgnoreCase("Worlds")) {
				foundW = true;
			}
		}
		if(!foundW) {
			database.createSection("Worlds");
			mainPlugin.saveDataBase();
		}
        boolean foundG = false;
		for(String key : database.getKeys(false)) {
			
			if(key.equalsIgnoreCase("Groups")) {
				foundG = true;
			}
		}
		if(!foundG) {
			database.createSection("Groups");
			mainPlugin.saveDataBase();
		}
        boolean foundNPC = false;
		for(String key : database.getKeys(false)) {
			
			if(key.equalsIgnoreCase("NPCs")) {
				foundNPC = true;
			}
		}
		if(!foundNPC) {
			database.createSection("NPCs");
			mainPlugin.saveDataBase();
		}
		 boolean foundP = false;
			for(String key : database.getKeys(false)) {
				
				if(key.equalsIgnoreCase("Players")) {
					foundP = true;
				}
			}
			if(!foundP) {
				database.createSection("Players");
				mainPlugin.saveDataBase();
			}
	}
	
	@EventHandler
	public void EntityInteractEntityEvent(PlayerInteractEntityEvent event) {
		for(String NPC : database.getConfigurationSection("NPCs").getKeys(false)) {
			if(NPC.equalsIgnoreCase(event.getRightClicked().getUniqueId().toString())) {
				for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
					if(key.equalsIgnoreCase(database.getString("NPCs."+NPC+".group"))) {
						List<World> worlds = new ArrayList<World>();
						for(String string : database.getStringList("Groups."+key+".worlds")) {
							worlds.add(Bukkit.getWorld(string));
						}
						World mostPlayersWorld = null;
						for(World world : worlds) {
							if(world.getPlayers().size() >= database.getInt("Groups."+key+".allowedPeople")) continue;
							if(mostPlayersWorld == null) {
								mostPlayersWorld = world;
							} else if(world.getPlayers().size() > mostPlayersWorld.getPlayers().size()) {
								mostPlayersWorld = null;
							}
						}
						if(mostPlayersWorld == null) {
							sendPlayerToHub(event.getPlayer());
						} else {
							event.getPlayer().setGameMode(GameMode.SURVIVAL);
							event.getPlayer().teleport(mostPlayersWorld.getSpawnLocation());
						}
					}
					
				}
			}
		}
	}
	
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if(arg0 instanceof Player) {
			if(arg2.equals("hubplus")) {
				switch(arg3[0]) {
					case "addworld":
						if(arg3[1] != null) {
							for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
								if(arg3[1].equalsIgnoreCase(key)) {
									List<String> worlds = database.getStringList("Groups."+key+".worlds");
									worlds.add(((Player) arg0).getLocation().getWorld().getName());
									database.set("Groups."+key+".worlds", worlds);
									arg0.sendMessage(ChatColor.GREEN+"Added world to group: "+key+"!");
								}
							}
						} else {
							arg0.sendMessage(ChatColor.RED+"Not enough arguments! (Put group name)");
						}
						break;
					case "removeworld":
						for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
							List<String> worlds = database.getStringList("Groups."+key+".worlds");
							for(int i = worlds.size(); i > 0; i--) {
								if(worlds.get(i).equalsIgnoreCase(((Player) arg0).getWorld().getName())) {
									worlds.remove(i);
								}
							}
							database.set("Groups."+key+".worlds", worlds);
						}
						arg0.sendMessage(ChatColor.RED+"Removed world from all groups!");
						break;
					case "group":
						switch(arg3[1]) {
							case "create":
								if(arg3[2] != null && Integer.parseInt(arg3[3]) > 0) {
									createDataBaseGroup(arg3[2], Integer.parseInt(arg3[3]));
									arg0.sendMessage(ChatColor.GREEN+"Created Group: "+arg3[2]+"!");
								}
								break;
							case "remove":
								if(arg3[2] != null) {
									database.set("Groups."+arg3[2], null);
								}
								
								break;
							default:
								arg0.sendMessage(ChatColor.RED+"Not enough arguments!");
								break;
						}
						break;
					case "npc":
						switch(arg3[1]) {
							case "create":
								if(arg3[2] != null) {
									LivingEntity npc = (LivingEntity) ((Player) arg0).getWorld().spawnEntity(((Player) arg0).getLocation(), EntityType.VILLAGER);
									npc.setAI(false);
									npc.setPersistent(true);
									npc.setSilent(true);
									npc.setInvulnerable(true);
									createDataBaseNPC(npc.getUniqueId().toString(), arg3[2]);
									arg0.sendMessage(ChatColor.GREEN+"Created NPC: "+arg3[2]+"!");
								}
								break;
							case "remove":
								if(arg3[2] != null) {
									Bukkit.getEntity(UUID.fromString(database.getString("NPCs."+arg3[2]))).remove();
									database.set("NPCs."+arg3[2], null);
								}
								
								break;
							default:
								arg0.sendMessage(ChatColor.RED+"Not enough arguments!");
								break;
							
						}
						break;
					default:
						arg0.sendMessage(ChatColor.RED+"Not enough arguments! (Addworld, Removeworld, Settings, Group, and NPC are options)");
						break;

				}
				mainPlugin.saveDataBase();
				
			} else if (arg2.equals("hub")) {
				sendPlayerToHub((Player)arg0);
			}
		}
		return false;
	}
	
	
	public void sendPlayerToHub(Player arg0) {
		for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
			if(key.equalsIgnoreCase("default")) {
				List<World> worlds = new ArrayList<World>();
				for(String string : database.getStringList("Groups."+key+".worlds")) {
					worlds.add(Bukkit.getWorld(string));
				}
				World mostPlayersWorld = null;
				for(World world : worlds) {
					if(world.getPlayers().size() >= database.getInt("Groups."+key+".allowedPeople")) continue;
					if(mostPlayersWorld == null) {
						mostPlayersWorld = world;
					} else if(world.getPlayers().size() > mostPlayersWorld.getPlayers().size()) {
						mostPlayersWorld = null;
					}
				}
				if(mostPlayersWorld == null) {
					arg0.kickPlayer(ChatColor.RED+"Too many players, not enough worlds! Come back later ;(");
				} else {
					arg0.setGameMode(GameMode.SURVIVAL);
					arg0.teleport(mostPlayersWorld.getSpawnLocation());
				}
			}
			
		}
	}
	
	public void createDataBaseWorld(String world) {
		database.createSection("Worlds."+world);
		database.set("gameStarted", false);
		database.set("vipOnly", false);
		mainPlugin.saveDataBase();
		
	}
	public void createDataBasePlayer(String player) {
		database.createSection("Players."+player);
		database.set("rank", "default");
		database.set("xp", 0);
		database.set("coins", 0);
		database.createSection("cosmetics");
		database.createSection("gamestatistics");
		mainPlugin.saveDataBase();
		
	}
	
	public void createDataBaseGroup(String name, int num) {
		database.createSection("Groups."+name);
		database.createSection("Groups."+name+".worlds");
		database.set("Groups."+name+".allowedPeople", num);
		mainPlugin.saveDataBase();
	}
	public void createDataBaseNPC(String uuid, String group) {
		database.createSection("NPCs."+uuid);
		database.set("NPCs."+uuid+".group", group);
		mainPlugin.saveDataBase();
	}
	
	
	
	@Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        List<String> list = new ArrayList<String>();
        List<String> results = new ArrayList<String>();
        if (sender instanceof Player) {
            if (cmd.getName().equalsIgnoreCase("hubplus")) {
                if (args.length == 0) {
                    list.add("addworld");
                    list.add("removeworld");
                    list.add("settings");
                    list.add("group");
                    list.add("npc");
                    Collections.sort(list);
                    return list;
                } else if (args.length == 1) {

                    list.add("addworld");
                    list.add("removeworld");
                    list.add("settings");
                    list.add("group");
                    list.add("npc");
                    for (String s : list){
                        if (s.toLowerCase().startsWith(args[0].toLowerCase())){
                        	results.add(s);
                        }
                    }
                    Collections.sort(results);
                    return results;
                } else if(args.length == 2) {
                	if(args[0].equalsIgnoreCase("settings")) { //Needs doing
                		
                	} else if(args[0].equalsIgnoreCase("addworld")) {
                		for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
                			list.add(key);
                		}
                	} else if(args[0].equalsIgnoreCase("group")) {
                		list.add("create");
                		list.add("remove");
                		
                	} else if(args[0].equalsIgnoreCase("npc")) {
                		list.add("create");
                		list.add("remove");
                		
                	}
                    for (String s : list){
                        if (s.toLowerCase().startsWith(args[1].toLowerCase())){
                            results.add(s);
                        }
                    }
                	Collections.sort(results);
                	return results;
                } else if(args.length == 3) {
                	if(args[0].equalsIgnoreCase("npc")) {
                		
                		if(args[1].equalsIgnoreCase("create")) {
                    		for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
                    			list.add(key);
                    		}
                		} else if(args[1].equalsIgnoreCase("remove")) {
                    		for(String key : database.getConfigurationSection("NPCs").getKeys(false)) {
                    			list.add(key);
                    		}
                		}
                        
                	} else if (args[0].equalsIgnoreCase("group")) {
                		 if(args[1].equalsIgnoreCase("remove")) {
                     		for(String key : database.getConfigurationSection("Groups").getKeys(false)) {
                     			list.add(key);
                     		}
                 		}
                	}
                }
                	
                	
                
                
            }
        }
        return list;
    }
}
