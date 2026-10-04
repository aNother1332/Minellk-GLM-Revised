package app;

public class UserSession {

    public static boolean loggedIn=false;
    public static String username="游客";
    public static String currentSkin="nature";
    public static void login(String name) {
        loggedIn=true;
        username=name;
    }
    public static void logout() {
        loggedIn=false;
        username="游客";
        currentSkin="nature";
    }

}