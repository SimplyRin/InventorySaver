package net.simplyrin.inventorysaver.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;
import net.simplyrin.inventorysaver.InventorySaver;
import net.simplyrin.inventorysaver.utils.FileType;
import net.simplyrin.inventorysaver.utils.InventoryData;

/**
 * Created by SimplyRin on 2019/12/15.
 *
 * Copyright (c) 2019-2022 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class ViewInventory implements CommandExecutor, TabCompleter {

	private InventorySaver instance;

	private HashMap<Player, Inventory> previousInventory = new HashMap<>();

	public ViewInventory(InventorySaver instance) {
		this.instance = instance;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length != 1) {
			return null;
		}
		
		List<String> list = new ArrayList<>();
		
		for (String name : this.instance.getPlayerConfig().getConfigurationSection("name").getKeys(false)) {
			list.add(name);
		}
		
		return list;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("inventorysaver.admin")) {
			sender.sendMessage("§cYou do not have access to this command");
			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("§cConsole is not supported!");
			return true;
		}

		Player player = (Player) sender;

		if (args.length > 0) {
			File folder = new File(this.instance.getDataFolder(), "players");
			
			String uuid = this.instance.getPlayerConfig().getString("name." + args[0].toLowerCase(), args[0]);
			File file = new File(folder, uuid);
			// 名前 -> UUID
			
			if (!file.exists()) {
				sender.sendMessage("§cData not exists!");
				return true;
			}
			
			if (file.isDirectory()) {
				int page = 0;
				if (args.length > 1) {
					try {
						page = Integer.valueOf(args[1]);
						if (page != 0) {
							page -= 1;
						}
					} catch (Exception e) {
					}
				}
				
				this.openDirectory(player, args[0], this.readZipFile(file), null, page);
				return true;
			}
		}

		sender.sendMessage("§cUsage: /" + cmd.getName() + " directory (page, optional)");
		return true;
	}
	
	public List<FileType> readZipFile(File directory) {
		List<FileType> files = new ArrayList<>();
		List<FileType> today = new ArrayList<>();
		
		for (File file : directory.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".zip")) {
				try {
					ZipFile zipFile = new ZipFile(file);
					
					Enumeration<? extends ZipEntry> entries = zipFile.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						files.add(new FileType(entry.getName(), true));
					}
					
					zipFile.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				today.add(new FileType(file.getName(), false));
			}
		}
		
		files.addAll(today);
		
		return files;
	}
	
	private HashMap<String, List<FileType>> map = new HashMap<>();

	public void openDirectory(Player player, String directoryName, List<FileType> files, String cacheId, int page) {
		if (files == null) {
			files = this.map.get(cacheId);
		} else {
			cacheId = UUID.randomUUID().toString().split("-")[0];
			this.map.put(cacheId, files);
		}
		
		String uniqueId = this.instance.getPlayerConfig().getString("name." + directoryName);
		directoryName = this.instance.getPlayerConfig().getString("uuid." + uniqueId, directoryName);
		
		List<List<FileType>> divide = this.divide(files, 45);
		
		if (page <= 0) {
			page = 0;
		}
		
		if (page >= divide.size()) {
			page = divide.size() - 1;
		}

		List<FileType> selected = divide.get(page);

		Inventory inventory = this.instance.getServer().createInventory(null, 9 * 6,
				"Directory: " + directoryName + " (" + (page + 1) + "/" + divide.size() + ") " + cacheId);

		int i = 0;
		for (FileType fileType : selected) {
			ItemStack itemStack = new ItemStack(Material.PAPER);
			itemStack.setAmount(i + 1);

			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&b") + fileType.getFilename());
			itemStack.setItemMeta(itemMeta);

			inventory.setItem(i, itemStack);

			i++;
		}

		ItemStack itemStack = new ItemStack(Material.ENDER_PEARL);
		ItemMeta itemMeta = itemStack.getItemMeta();

		itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&7"));
		itemStack.setItemMeta(itemMeta);

		for (int _i = 45; _i <= 53; _i++) {
			inventory.setItem(_i, itemStack);
		}

		itemStack = new ItemStack(Material.ARROW);

		if (page != 0) {
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&b") + "<- Previous Page");
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(45, itemStack);
		}

		if ((page + 1) != divide.size()) {
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&b") + "Next Page ->");
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(53, itemStack);
		}

		player.openInventory(inventory);
	}

	public void openInventory(Player player, InventoryData inventoryData, String inventoryName) {
		this.openInventory(player, inventoryData, inventoryName, null);
	}

	public void openInventory(Player player, InventoryData inventoryData, String inventoryName, Inventory previousInventory) {
		Inventory inventory = this.instance.getServer().createInventory(null, 9 * 5, inventoryName);

		int i = 39;
		for (ItemStack itemStack : inventoryData.getArmor()) {
			inventory.setItem(i, itemStack);
			i--;
		}

		i = 27;
		for (ItemStack itemStack : inventoryData.getContent()) {
			inventory.setItem(i, itemStack);

			i++;

			if (i == 36) {
				i = 0;
			} if (i == 27) {
				break;
			}
		}
		
		inventory.setItem(40, inventoryData.getOffhand());
		
		if (inventoryData.getLevel() != -1) {
			ItemStack exp = new ItemStack(Material.EXPERIENCE_BOTTLE);
			ItemMeta expItemMeta = exp.getItemMeta();
			expItemMeta.setDisplayName("§bExperience");
			
			int percent = (int) (inventoryData.getExp() * 100);
			
			expItemMeta.setLore(Arrays.asList("§r§aLevel: " + inventoryData.getLevel(), "§r§aExp: " + percent + "%"));
			exp.setItemMeta(expItemMeta);
			inventory.setItem(43, exp);
		}
		

		if (previousInventory != null) {
			this.previousInventory.put(player, previousInventory);

			ItemStack arrow = new ItemStack(Material.ARROW);
			ItemMeta arrowItemMeta = arrow.getItemMeta();
			arrowItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&b") + "<- Back");
			arrow.setItemMeta(arrowItemMeta);
			inventory.setItem(44, arrow);
		}

		player.openInventory(inventory);
	}

	public void openPreviousInventory(Player player) {
		if (this.previousInventory.get(player) != null) {
			player.openInventory(this.previousInventory.get(player));
		}
	}

	public <T> List<List<T>> divide(List<T> original, int size) {
		if (original == null || original.isEmpty() || size <= 0) {
			return Collections.emptyList();
		}

		try {
			int block = original.size() / size + (original.size() % size > 0 ? 1 : 0);

			return IntStream.range(0, block).boxed().map(i -> {
				int start = i * size;
				int end = Math.min(start + size, original.size());
				return original.subList(start, end);
			}).collect(Collectors.toList());
		} catch (Exception e) {
			return null;
		}
	}

}
