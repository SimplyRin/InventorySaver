package net.simplyrin.inventorysaver.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.simplyrin.inventorysaver.InventorySaver;

/**
 * Created by SimplyRin on 2018/11/11.
 *
 * Copyright (c) 2018-2022 SimplyRin
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
public class InventoryManager {

	private InventorySaver instance;

	public InventoryManager(InventorySaver instance) {
		this.instance = instance;

		instance.getDataFolder().mkdirs();
		File file = new File(instance.getDataFolder(), "inventory.yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveOnlinePlayersInventory() {
		for (Player player : this.instance.getServer().getOnlinePlayers()) {
			this.saveInventory(player);
		}
	}

	public void saveInventory(Player player) {
		this.saveInventory(player, true);
	}

	public void saveInventory(Player player, boolean skipEmptyInventory) {
		File players = new File(this.instance.getDataFolder(), "players");
		File folder = new File(players, player.getUniqueId().toString());
		folder.mkdirs();

		File location = new File(folder, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".yml");
		
		ItemStack[] armor = player.getInventory().getArmorContents();
		ItemStack[] contents = player.getInventory().getContents();

		if (skipEmptyInventory) {
			boolean hasItem = false;
			for (ItemStack a : armor) {
				if (a != null) {
					hasItem = true;
				}
			}

			for (ItemStack c : contents) {
				if (c != null) {
					hasItem = true;
				}
			}

			if (!hasItem) {
				return;
			}
		}

		File lastModified = null;
		for (File file : folder.listFiles()) {
			if (!file.getName().endsWith(".yml")) {
				continue;
			}
			
			if (lastModified == null) {
				lastModified = file;
			}
			
			if (lastModified.lastModified() <= file.lastModified()) {
				lastModified = file;
			}
		}

		if (lastModified != null) {
			FileConfiguration lastConfig = YamlConfiguration.loadConfiguration(lastModified);
			InventoryData invData = this.instance.getInventoryManager().getInventoryData(lastConfig);
			
			if (Arrays.equals(invData.getArmor(), armor) && Arrays.equals(invData.getContent(), contents)) {
				return;
			}
		}
			
		FileConfiguration config = YamlConfiguration.loadConfiguration(location);

		config.set("Exp.Exp", (double) player.getExp());
		config.set("Exp.Level", player.getLevel());
		config.set("Inventory.Armor", armor);
		config.set("Inventory.Content", contents);
		config.set("Inventory.Offhand", player.getInventory().getItemInOffHand());
		try {
			config.save(location);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadInventory(Player player, String directory, String filename) {
		InventoryData invData = this.getInventoryData(player, directory, filename);
		
		double exp = invData.getExp();
		if (exp != -1) {
			player.setExp((float) exp);
		}
		
		int level = invData.getLevel();
		if (level != -1) {
			player.setLevel(level);
		}

		player.getInventory().setArmorContents(invData.getArmor());
		player.getInventory().setContents(invData.getContent());
		player.getInventory().setItemInOffHand(invData.getOffhand());
	}
	
	public InventoryData getInventoryData(Player player, String directory, String filename) {
		File players = new File(this.instance.getDataFolder(), "players");
		
		directory = this.instance.getPlayerConfig().getString("name." + directory.toLowerCase());
		File dataFolder = new File(players, directory);
		
		File file = new File(dataFolder, filename);
		
		FileConfiguration config = null;
		
		// あればファイルを読み込む、なければ zip ファイルから読み込む
		if (file.exists()) {
			config = YamlConfiguration.loadConfiguration(file);
		} else {
			// ZIP ファイルを開く
			String yyyy_MM_dd = filename.split("_")[0];
			
			File zFile = null;
			for (File childFile : dataFolder.listFiles()) {
				if (childFile.getName().endsWith(yyyy_MM_dd + ".zip")) {
					zFile = childFile;
				}
			}
			
			// File zFile = new File(dataFolder, yyyy_MM_dd + ".zip");
			
			this.instance.getLogger().info("Open: " + zFile.getName() + "/" + filename);
			
			try {
				ZipFile zipFile = new ZipFile(zFile);
				ZipEntry zipEntry = zipFile.getEntry(filename);
				
				InputStream is = zipFile.getInputStream(zipEntry);
				
				config = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
				
				zipFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		return this.getInventoryData(config);
	}
	
	public InventoryData getInventoryData(FileConfiguration config) {
		double exp = config.getDouble("Exp.Exp", -1);
		int level = config.getInt("Exp.Level", -1);
		
		ItemStack[] armor = ((List<ItemStack>) config.get("Inventory.Armor")).toArray(new ItemStack[0]);
		ItemStack[] content = ((List<ItemStack>) config.get("Inventory.Content")).toArray(new ItemStack[0]);
		ItemStack offhand = config.getItemStack("Inventory.Offhand");
		
		return new InventoryData(exp, level, armor, content, offhand);
	}

}