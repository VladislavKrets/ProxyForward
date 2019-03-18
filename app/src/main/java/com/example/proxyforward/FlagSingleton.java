package com.example.proxyforward;

public class FlagSingleton {
    private static FlagSingleton instance;
    private boolean flag;

    private FlagSingleton(){
        flag = true;
    }

    public static synchronized FlagSingleton getInstance() {
        if (instance == null) instance = new FlagSingleton();
        return instance;
    }


    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
