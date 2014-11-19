/**
 * Created by eric7500 on 9/2/14.
 */

package com.rackspace.papi.components.clientauth;
import java.io.FileWriter;
import java.util.ArrayList;
public class BadCode {
    int x;
    String s;

    public int getX() {
        return x == 1 ? x : x;
    }

    public int getX2() {
        if (true) {
            return x;
        } else x = x;

        synchronized (this) {
            int number = (int) 5.0;
        }

        return x;
    }

    public int getXPlus() {
        x += 1;
        x = (true)?x:x - 1;
        return x;
    }


    public String callToStringS() throws Exception {
        try (FileWriter r1 = null;) {
            int z = 1;
        }
        return (s.toString());
    }


    public ArrayList getList() {
        return new ArrayList<Integer>();
    }

    public int switchNum() {
        switch (1) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }


    }
}
