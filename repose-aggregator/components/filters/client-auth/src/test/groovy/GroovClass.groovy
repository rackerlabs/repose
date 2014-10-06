package a
import groovy.json.JsonBuilder
class GroovClass {
    void hello()
    {
        println "hello, world"
        boolean x = true;
        if (x){

        } else{

        }

        JsonBuilder jb = new JsonBuilder()

        int j = 0
        int m = 0

        for (int i = 0; i < 10; i++) {
            println "for"
            j = i == 2 ? 2 :3;
            m = i == 2? 2 : 3;
        }
        println j
        println m


    }







}
