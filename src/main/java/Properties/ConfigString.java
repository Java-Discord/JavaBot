package Properties;

public class ConfigString extends ConfigElement {
    private String value;

    public ConfigString(String entryname, String defaultvalue) {
        super(entryname);
        if(this.isRegisteredInConfig()){
            try{
                this.value = this.load();
            }catch (Exception e){
                System.out.println("Failed whilst loading: " + entryname);
                e.printStackTrace();
            }
        }else{
            try{
                this.save(String.valueOf(defaultvalue));
            }catch (Exception e){
                System.out.println("Failed whilst saving: " + entryname);
                e.printStackTrace();
            }

        }
    }
    public String getValue(){
        return this.value;
    }

    public void setValue(String value){
        this.value = value;
        try{
            this.save(String.valueOf(value));
        }catch (Exception e){
            System.out.println("Failed whilst saving: " + entryname);
            e.printStackTrace();
        }
    }


}
