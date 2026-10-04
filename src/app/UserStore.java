package app;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserStore {
    private static final String filepath="users.txt";
    private static Map<String, String> users=new HashMap<>();
    static{load();}//static:只运行一次
    public static void load(){//将文本里内容加载到内存
        users.clear();
        File file=new File(filepath);
        if(!file.exists()){
            return;
        }
        try (BufferedReader a=new BufferedReader(new FileReader(file))) {//读完一行之后自动向下一行
            for (String line;(line=a.readLine())!=null;){
                String[] parts=line.split(",");//将文字切成两段
                if(parts.length==2){
                    users.put(parts[0],parts[1]);//校验，然后将切割好的存入HashMap里
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    private static void save() {//将内存里的存入文本
        try (BufferedWriter a=new BufferedWriter(new FileWriter(filepath))) {
            for (Map.Entry<String, String>entry:users.entrySet()) {//entrySet:一组账户和密码
                a.write(entry.getKey()+","+entry.getValue());//用逗号分隔开
                a.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean register(String username, String password) {
        if (username==null||username.isEmpty()||users.containsKey(username)){
            return false;
        }
        users.put(username, password);
        save();
        return true;
    }
    public static boolean login(String username, String password) {
        if (username==null||users.containsKey(username)==false) {
            return false;
        }
        if(users.get(username).equals(password)){//users.get(username)输入的是key,返回的是Value
            return true;
        }//
        else{
            return false;
        }
    }
}