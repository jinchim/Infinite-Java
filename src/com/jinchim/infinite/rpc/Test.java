package com.jinchim.infinite.rpc;



public class Test implements TestService {

    @Override
    public String test(String str) {
        return "Proxy => " + str;
    }

}
