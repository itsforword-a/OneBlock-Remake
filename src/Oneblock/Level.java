package Oneblock;

import java.util.ArrayList;

import org.bukkit.boss.BarColor;

public class Level {
	public static Level max = new Level("Level: MAX");
	public static ArrayList <Level> levels = new ArrayList <>();
	public static int multiplier = 5;
	
	public static Level get(int i) {
		if (i < levels.size())
			return levels.get(i);
		return max;
	}
	
	public static int size() {
		return levels.size();
	}
	
	public String name;
	public ArrayList<Object> blocks = new ArrayList<>();
	public int need = 100;
	public BarColor color;
	
	public Level(String name) {
        this.name = name;
        this.color = BarColor.GREEN;
    }
	
	public Level(String name, BarColor color) {
        this.name = name;
        this.color = color;
    }
	
	public int getId() {
		for (int i = 0; i < size(); i++) {
			Level lvl = get(i);
			if (lvl == this)
				return i;
		}
		return 1;
	}
	
	public void addBlock(Object block) {
        blocks.add(block);
    }
	
	public void setNeed(int need) {
        this.need = need;
    }
	
	public void setColor(BarColor color) {
        this.color = color;
    }
}