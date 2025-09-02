package com.example.demo.httpserver;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import java.net.URI;

/**
 *
 * @author luisdanielbenavidesnavarro
 */
public class HttpRequest {
    
    URI requri = null;

    HttpRequest(URI requri) {
        this.requri = requri;
    }

    public String getValue(String paramName) {
        if (requri.getQuery() == null) return null;
        String[] pairs = requri.getQuery().split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(paramName)) {
                return kv[1];
            }
        }
        return null;
    }

}
