package org.abimon.eHandbook;

import java.io.File;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.abimon.omnis.io.ClassLoaderDataPool;
import org.abimon.omnis.io.Data;
import org.abimon.omnis.io.VirtualPrintStream;
import org.abimon.omnis.lanterna.ScrollPanel;
import org.abimon.omnis.lanterna.ScrollWindow;
import org.abimon.omnis.ludus.Ludus;
import org.abimon.omnis.net.Website;
import org.abimon.omnis.util.EnumOS;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.FileDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class eHandbook {

	public static Terminal terminal;
	public static Screen screen;
	public static MultiWindowTextGUI gui;

	public static JsonObject loadedEvidence = null;
	public static File loadedFile = null;

	public static File eHandbookLocation = EnumOS.determineOS().getStorageLocation("eHandbook");

	public static LinkedList<JsonObject> maps = new LinkedList<JsonObject>();

	public static void main(String[] args){
		new eHandbook();
	}

	public eHandbook(){
		try{
			if(!eHandbookLocation.exists())
				eHandbookLocation.mkdir();
			File mapsFile = new File(eHandbookLocation, "maps");
			File evidenceFile = new File(eHandbookLocation, "evidence");
			if(!mapsFile.exists())
				mapsFile.mkdir();
			if(!evidenceFile.exists())
				evidenceFile.mkdir();
			Ludus.registerDataPool(new ClassLoaderDataPool(this.getClass()));
			Ludus.registerDataPool(new File("resources"));
			Ludus.registerDataPool(eHandbookLocation);

			Runtime.getRuntime().addShutdownHook(new Thread(){
				public void run(){
					if(loadedEvidence != null)
						saveEvidence(loadedFile);
				}
			});

			Data[] mapData = Ludus.getAllData(".*maps/.*.json");
			for(Data map : mapData)
				maps.add(new JsonParser().parse(map.getAsString()).getAsJsonObject());

			DefaultTerminalFactory factory = new DefaultTerminalFactory();
			factory.setSwingTerminalFrameTitle("eHandbook - " + System.getProperty("user.name"));
			terminal = factory.createTerminal();
			screen = new TerminalScreen(terminal);
			screen.startScreen();

			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			panel.addComponent(new Label("Welcome " + System.getProperty("user.name")));
			panel.addComponent(new Label("Running " + EnumOS.determineOS()));
			panel.addComponent(new Button("Go to eHandbook", new Runnable(){
				public void run(){
					menu();
				}
			}));

			Window window = new BasicWindow();
			window.setComponent(panel);

			gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.CYAN));
			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			th.printStackTrace();
		}
	}

	public void menu(){
		try{
			for(Window window : gui.getWindows())
				gui.removeWindow(window);
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			panel.addComponent(new Button("Map", new Runnable(){
				public void run(){
					map();
				}
			}));

			panel.addComponent(new Button("Truth Bullets", new Runnable(){
				public void run(){
					evidence();
				}
			}));

			panel.addComponent(new Button("Turn Off", new Runnable(){
				public void run(){
					System.exit(0);
				}
			}));

			Window window = new BasicWindow();
			window.setComponent(panel);
			gui.addWindowAndWait(window);
		}
		catch(Throwable th){}
	}

	public static final Runnable BACK_BUTTON = new Runnable(){
		public void run(){
			gui.removeWindow(gui.getActiveWindow());
		}
	};

	public static void saveEvidence(){
		try{
			FileDialog jfc = new FileDialog("Evidence", "Choose a location to save to", "Save", new TerminalSize(50, 5), false, new File(eHandbookLocation, "evidence"));
			File savingLocation = jfc.showDialog(gui);
			if(savingLocation == null)
				return;
			loadedFile = savingLocation;
			saveEvidence(savingLocation);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public static void saveEvidence(File file){
		try{
			if(file == null)
				return;
			GsonBuilder builder = new GsonBuilder();
			builder.setPrettyPrinting();
			new Data(builder.create().toJson(loadedEvidence).toString()).write(file);
		}
		catch(Throwable th){
			th.printStackTrace();
		}
	}

	public void evidence() {
		try{
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			panel.addComponent(new Button("View", new Runnable(){
				public void run(){
					evidence("");
				}
			}));

			panel.addComponent(new Button("Load...", new Runnable(){
				public void run(){
					loadEvidence();
				}
			}));

			panel.addComponent(new Button("Back", BACK_BUTTON));

			Window window = new BasicWindow();
			window.setComponent(panel);

			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public void evidence(String bullet) {
		try{
			Panel panel;
			Window window;

			if(bullet.equals("")){
				panel = new ScrollPanel(10);
				window = new ScrollWindow();

				panel.setLayoutManager(new GridLayout(1));
				try{
					for(final Entry<String, JsonElement> element : loadedEvidence.get("evidence").getAsJsonObject().entrySet()){
						panel.addComponent(new Button(element.getKey(), new Runnable(){
							public void run(){
								evidence(element.getKey());
							}
						}));
					}
				}
				catch(Throwable th){}

				panel.addComponent(new Button("Add", new Runnable(){
					public void run(){
						addEvidence();
					}
				}));
				panel.addComponent(new Button("Back", BACK_BUTTON));
			}
			else{
				panel = new Panel();
				window = new BasicWindow();

				panel.setLayoutManager(new GridLayout(2));
				try{
					System.out.println(loadedEvidence);
					JsonObject truthBullet = loadedEvidence.get("evidence").getAsJsonObject().get(bullet).getAsJsonObject();

					panel.addComponent(new Label("When it was discovered: "));
					panel.addComponent(new Label(truthBullet.get("discovered_time").getAsString()));

					panel.addComponent(new Label("Where it was discovered: "));
					panel.addComponent(new Label(truthBullet.get("discovered_location").getAsString().split(":")[truthBullet.get("discovered_location").getAsString().split(":").length-1]));

					panel.addComponent(new Label("Who discovered it: "));
					panel.addComponent(new Label(truthBullet.get("discovered_person").getAsString()));
				}
				catch(Throwable th){
					th.printStackTrace();
				}

				panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
				panel.addComponent(new Button("Back", BACK_BUTTON));
			}

			window.setComponent(panel);
			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public void addEvidence() {
		try{
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(2));

			final TextBox name 		=		new TextBox();

			final TextBox time 		= 		new TextBox(new TerminalSize(20, 1));
			final TextBox location 	= 		new TextBox(new TerminalSize(20, 1));
			final TextBox person 	= 		new TextBox(new TerminalSize(20, 1));
			final TextBox desc 		= 		new TextBox(new TerminalSize(20, 5));

			panel.addComponent(new Label("Name: "));
			panel.addComponent(name);

			panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
			panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));

			panel.addComponent(new Label("When it was discovered: "));
			panel.addComponent(time);

			panel.addComponent(new Label("Where it was discovered: "));
			panel.addComponent(location);

			panel.addComponent(new Label("Who discovered it: "));
			panel.addComponent(person);

			panel.addComponent(new Label("Description: "));
			panel.addComponent(desc);

			panel.addComponent(new Button("Add", new Runnable(){
				public void run(){
					JsonObject truthBullet = new JsonObject();
					truthBullet.add("discovered_time", new JsonPrimitive(time.getText()));
					truthBullet.add("discovered_location", new JsonPrimitive(location.getText()));
					truthBullet.add("discovered_person", new JsonPrimitive(person.getText()));

					JsonArray description = new JsonArray();
					for(String line : desc.getText().split("\n"))
						description.add(line);
					truthBullet.add("desc", description);

					loadedEvidence.get("evidence").getAsJsonObject().add(name.getText(), truthBullet);
					BACK_BUTTON.run();
				}
			}));
			panel.addComponent(new Button("Back", BACK_BUTTON));

			Window window = new BasicWindow();
			window.setComponent(panel);
			gui.addWindowAndWait(window);
		}
		catch(Throwable th){}
	}

	public void loadEvidence(){
		try{
			final Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			panel.addComponent(new Button("...existing file in storage folder", new Runnable(){
				public void run(){
					FileDialog jfc = new FileDialog("Evidence", "Choose a .json evidence file to load", "Load", panel.getSize(), false, new File(eHandbookLocation, "evidence"));
					loadEvidence(jfc.showDialog(gui));
				}
			}));

			panel.addComponent(new Button("...existing file elsewhere on harddrive", new Runnable(){
				public void run(){
					FileDialog jfc = new FileDialog("Evidence", "Choose a .json evidence file to load", "Load", panel.getSize(), false, new File(System.getProperty("user.home")));
					loadEvidence(jfc.showDialog(gui));
				}
			}));
			
			panel.addComponent(new Button("...from a url", new Runnable(){
				public void run(){
					Panel urlPanel = new Panel();
					urlPanel.setLayoutManager(new GridLayout(2));
					
					final TextBox url = new TextBox(new TerminalSize(30, 1));
					
					urlPanel.addComponent(new Label("URL: "));
					urlPanel.addComponent(url);
					urlPanel.addComponent(new Button("Back", BACK_BUTTON));
					urlPanel.addComponent(new Button("Download & Add", new Runnable(){
						public void run(){
							try{
								Website website = new Website(url.getText());
								File fileLoc = new File(new File(eHandbookLocation, "evidence"), url.getText().split("/")[url.getText().split("/").length - 1]);
								if(!fileLoc.exists())
									fileLoc.createNewFile();
								new Data(website.retrieveContent()).write(fileLoc);
								BACK_BUTTON.run();
							}
							catch(Throwable th){}
						}
					}));
					
					Window window = new BasicWindow();
					window.setComponent(urlPanel);
					gui.addWindowAndWait(window);
				}
			}));

			panel.addComponent(new Button("...new, blank evidence sheet", new Runnable(){
				public void run(){
					saveEvidence();
					eHandbook.loadedEvidence = new JsonObject();
					Panel panel = new Panel();
					final TextBox txt = new TextBox(new TerminalSize(20, 1));
					panel.setLayoutManager(new GridLayout(2));
					panel.addComponent(new Label("Trial: "));
					panel.addComponent(txt);
					panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
					panel.addComponent(new Button("Refute!", new Runnable(){
						public void run(){
							eHandbook.loadedEvidence.add("trial", new JsonPrimitive(txt.getText()));
							BACK_BUTTON.run();
							BACK_BUTTON.run();
						}
					}));

					BasicWindow window = new BasicWindow();
					window.setComponent(panel);
					gui.addWindowAndWait(window);
				}
			}));

			panel.addComponent(new Button("Back", BACK_BUTTON));

			Window window = new BasicWindow();
			window.setComponent(panel);

			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public void loadEvidence(File file){
		try{
			Data data = new Data(file);
			if(data != null)
				loadedEvidence = new JsonParser().parse(data.getAsString().trim()).getAsJsonObject();
			loadedFile = file;
			BACK_BUTTON.run();
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public void map() {
		try{
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			for(final JsonObject obj : maps)
				panel.addComponent(new Button(obj.get("name").getAsString(), new Runnable(){
					public void run(){
						map(obj, "");
					}
				}));

			panel.addComponent(new Button("Back", BACK_BUTTON));

			Window window = new BasicWindow();
			window.setComponent(panel);

			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public void map(final JsonObject map, final String room) {
		try{
			Panel panel = new ScrollPanel(10);
			panel.setLayoutManager(new GridLayout(1));

			if(room.equals("") && map != null && map.has("sections")){
				for(final Entry<String, JsonElement> element : map.get("sections").getAsJsonObject().entrySet()){
					panel.addComponent(new Button(element.getKey(), new Runnable(){
						public void run(){
							map(map, element.getKey());
						}
					}));
				}
			}
			else if(room.indexOf(':') == -1 && map != null && map.has("sections")){
				for(final Entry<String, JsonElement> element : map.get("sections").getAsJsonObject().get(room).getAsJsonObject().get("locations").getAsJsonObject().entrySet()){
					panel.addComponent(new Button(element.getKey(), new Runnable(){
						public void run(){
							map(map, room + ":" + element.getKey());
						}
					}));
				}
			}
			else if(map != null && loadedEvidence != null && loadedEvidence.has("evidence")){
				for(final Entry<String, JsonElement> element : loadedEvidence.get("evidence").getAsJsonObject().entrySet()){
					try{
						if(element.getValue().getAsJsonObject().get("discovered_location").getAsString().replaceAll("\\s+", "").equalsIgnoreCase(room.replaceAll("\\s+", "")))
							panel.addComponent(new Button(element.getKey(), new Runnable(){
								public void run(){
									evidence(element.getKey());
								}
							}));
					}
					catch(Throwable th){}
				}
			}

			panel.addComponent(new Button("Back", BACK_BUTTON));

			Window window = new ScrollWindow();
			window.setComponent(panel);

			gui.addWindowAndWait(window);
		}
		catch(Throwable th){
			handleError(th);
		}
	}

	public static void handleError(Throwable th){
		try{
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(1));

			VirtualPrintStream vps = new VirtualPrintStream();
			th.printStackTrace(vps);
			vps.close();

			Label unexpected = new Label("Unexpected error");
			Label error = new Label(vps.getContents().getAsString());
			unexpected.setForegroundColor(TextColor.ANSI.RED);
			error.setForegroundColor(TextColor.ANSI.RED);
			panel.addComponent(unexpected);
			panel.addComponent(error);
			panel.setPreferredSize(error.getPreferredSize());//new TerminalSize(vps.getContents().getAsString().vps.getContents().getAsString().split("\n").length)));

			Window window = new BasicWindow();
			window.setComponent(panel);

			gui.addWindowAndWait(window);
		}
		catch(Throwable the){
			handleError(the);
		}
	}
}
