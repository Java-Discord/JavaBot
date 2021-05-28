package Properties;

import java.io.*;
import java.util.Properties;


public class ConfigElement {
    String entryname;

    ConfigElement(String entryname){
        this.entryname = entryname;
    }

    public static void init() {
        try {
            new File("bot.prop").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(String value) throws IOException {
        Properties prop = new Properties();
        prop.load(new BufferedInputStream(new FileInputStream("bot.prop")));
        prop.setProperty(entryname,value);
        prop.store(new FileOutputStream("bot.prop"),"");
    }

    public String load() throws IOException {
        Properties prop = new Properties();
        prop.load(new BufferedInputStream(new FileInputStream("bot.prop")));
        return prop.getProperty(entryname);
    }
    boolean isRegisteredInConfig() {
        try{
            Properties prop = new Properties();
            prop.load(new BufferedInputStream(new FileInputStream("bot.prop")));
            return prop.containsKey(entryname);
        }catch (Exception e){return false;}
    }
}
