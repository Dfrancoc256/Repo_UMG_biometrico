package com.umg.biometrico.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ProbarHash {
    public static void main(String[] args) {
        String hash = "$2a$10$kDeDuCYsbDAzKaQFLZwwJuCvMhResePMDh5.Jj4RBz6ECPMFsrswO";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.matches("1234", hash));
    }
}