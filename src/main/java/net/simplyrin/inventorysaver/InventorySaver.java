package net.simplyrin.inventorysaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import net.simplyrin.inventorysaver.commands.LoadInventory;
import net.simplyrin.inventorysaver.commands.SaveInventory;
import net.simplyrin.inventorysaver.commands.ViewInventory;
import net.simplyrin.inventorysaver.listeners.EventListener;
import net.simplyrin.inventorysaver.utils.InvZip;
import net.simplyrin.inventorysaver.utils.InventoryManager;

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
@Getter
public class InventorySaver extends JavaPlugin {
	
	private File playerFile;
	private YamlConfiguration playerConfig;

	private InventorySaver instance;
	private InventoryManager inventoryManager;
	private ViewInventory viewInventory;

	@Override
	public void onEnable() {
		this.instance = this;
		this.inventoryManager = new InventoryManager(this);

		this.saveDefaultConfig();
		
		this.playerFile = new File(this.getDataFolder(), "players.yml");
		if (!this.playerFile.exists()) {
			try {
				this.playerFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.playerConfig = YamlConfiguration.loadConfiguration(this.playerFile);

		LoadInventory loadInventory = new LoadInventory(this);
		this.getCommand("loadinventory").setExecutor(loadInventory);
		this.getCommand("loadinv").setExecutor(loadInventory);
		
		SaveInventory saveInventory = new SaveInventory(this);
		this.getCommand("saveinventory").setExecutor(saveInventory);
		this.getCommand("saveinv").setExecutor(saveInventory);

		this.viewInventory = new ViewInventory(this);
		this.getCommand("viewinventory").setExecutor(this.viewInventory);
		this.getCommand("viewinv").setExecutor(this.viewInventory);

		this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
		
		// 前の日までのデータを全て zip に打ち込む
		this.saveToZip();

		this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
			this.inventoryManager.saveOnlinePlayersInventory();
		}, 0, 20L * this.getConfig().getInt("Interval"));
	}

	@Override
	public void onDisable() {
		try {
			this.playerConfig.save(this.playerFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	public void saveToZip() {
		this.getLogger().info("Zipping previous day's data into a ZIP file...");
		
		File players = new File(this.instance.getDataFolder(), "players");
		players.mkdirs();
		
		HashMap<String, InvZip> map = new HashMap<>();
		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		
		if (players.listFiles() == null) {
			return;
		}
		
		for (File uniqueIdFolder : players.listFiles()) {
			if (uniqueIdFolder.isFile()) {
				continue;
			}
			
			String uuid = uniqueIdFolder.getName();
			
			for (File file : uniqueIdFolder.listFiles()) {
				if (file.getName().endsWith(".yml")) {
					String yyyy_MM_dd = file.getName().split("_")[0];
					
					if (yyyy_MM_dd.equals(today)) {
						continue;
					}
					
					if (map.get(uuid + "_" + yyyy_MM_dd) == null) {
						this.getLogger().info(uuid + ": " + yyyy_MM_dd + ".zip ...");
						
						try {
							File zFile = new File(uniqueIdFolder, uuid + "_" + yyyy_MM_dd + ".zip");

							FileOutputStream fos = new FileOutputStream(zFile);
							ZipOutputStream zos = new ZipOutputStream(fos);
							
							map.put(uuid + "_" + yyyy_MM_dd, new InvZip(zFile, zos));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					InvZip invZip = map.get(uuid + "_" + yyyy_MM_dd);
					
					ZipOutputStream stream = invZip.getZipOutStream();
					
					try {
						stream.putNextEntry(new ZipEntry(file.getName()));
						stream.write(Files.readAllBytes(file.toPath()));
						stream.closeEntry();
						
						invZip.getAddedFiles().add(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		for (Entry<String, InvZip> entry : map.entrySet()) {
			InvZip invZip = entry.getValue();
			
			try {
				this.getLogger().info("Saving " + invZip.getFile().getName() + "...");
				
				invZip.getZipOutStream().close();
				
				this.getLogger().info("Saved  " + invZip.getFile().getName() + "!");
				
				for (File file : invZip.getAddedFiles()) {
					file.delete();
				}
				
				invZip.getAddedFiles().clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.getLogger().info("Data up to the previous day was compressed into a ZIP file.");
	}
}
